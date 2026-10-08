/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the APK check АПК_00157
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: запись константы (вызов {@code Константы.<Имя>.Установить(...)},
 * {@code Константы.<Имя>.Записать(...)} — рус/англ {@code Constants}) не следует
 * выполнять внутри транзакции. На время записи значения константы работа других
 * сеансов приостанавливается, если они выполняют запись этой же константы, поэтому
 * запись константы в транзакции становится «узким» местом при конкурентной работе.
 * <p>
 * Перенос проверки АПК_00157 (статья 783 стандарта 1С), упрощённо. Охват:
 * <ul>
 * <li>замечание ставится, когда вызов записи константы лексически расположен в том
 * же методе между вызовом «НачатьТранзакцию» и вызовом «ЗафиксироватьТранзакцию»
 * (рус/англ) — по позициям операторов, вложенность Если/Циклов не разворачивается;</li>
 * <li>транзакция, открытая в другом методе, не отслеживается; запись вне пары
 * «начать — зафиксировать» одного метода не помечается;</li>
 * <li>п. 2 алгоритма АПК (методы обхода блокировки констант — тотальное кеширование
 * в параметрах сеанса и функциях с повторным использованием возвращаемого значения)
 * не формализуется статически и не переносится.</li>
 * </ul>
 * Чтение констант ({@code Получить}) и запись других объектов проверкой не помечаются.
 *
 * @author malikov-pro
 */
public class ApkConstantWriteInTransactionCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00157-constants-write-in-txn"; //$NON-NLS-1$

    private static final String CONSTANTS_RU = "Константы"; //$NON-NLS-1$

    private static final String CONSTANTS_EN = "Constants"; //$NON-NLS-1$

    private static final String WRITE_RU = "Записать"; //$NON-NLS-1$

    private static final String WRITE_EN = "Write"; //$NON-NLS-1$

    private static final String SET_RU = "Установить"; //$NON-NLS-1$

    private static final String SET_EN = "Set"; //$NON-NLS-1$

    private static final String BEGIN_TRANSACTION_RU = "НачатьТранзакцию"; //$NON-NLS-1$

    private static final String BEGIN_TRANSACTION_EN = "BeginTransaction"; //$NON-NLS-1$

    private static final String COMMIT_TRANSACTION_RU = "ЗафиксироватьТранзакцию"; //$NON-NLS-1$

    private static final String COMMIT_TRANSACTION_EN = "CommitTransaction"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public ApkConstantWriteInTransactionCheck()
    {
        super();
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.ApkConstantWriteInTransactionCheck_title)
            .description(Messages.ApkConstantWriteInTransactionCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.PERFORMANCE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(METHOD);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Method method = (Method)object;

        // Лексический охват транзакции: позиции операторов «НачатьТранзакцию»/«ЗафиксироватьТранзакцию»
        // в том же методе, без разворачивания вложенности Если/Циклов (техника проверки АПК_00334).
        List<Integer> begins = new ArrayList<>();
        List<Integer> commits = new ArrayList<>();
        for (SimpleStatement statement : EcoreUtil2.getAllContentsOfType(method, SimpleStatement.class))
        {
            if (!(statement.getLeft() instanceof Invocation invocation)
                || !(invocation.getMethodAccess() instanceof StaticFeatureAccess methodAccess))
            {
                continue;
            }
            String name = methodAccess.getName();
            if (!isBeginTransaction(name) && !isCommitTransaction(name))
            {
                continue;
            }
            int offset = offsetOf(statement);
            if (offset < 0)
            {
                continue;
            }
            if (isBeginTransaction(name))
            {
                begins.add(offset);
            }
            else
            {
                commits.add(offset);
            }
        }
        if (begins.isEmpty() || commits.isEmpty())
        {
            return;
        }

        for (Invocation invocation : EcoreUtil2.getAllContentsOfType(method, Invocation.class))
        {
            if (progressMonitor.isCanceled())
            {
                return;
            }
            if (!isConstantWrite(invocation))
            {
                continue;
            }
            int offset = offsetOf(invocation);
            if (offset < 0)
            {
                continue;
            }
            if (isBetween(offset, begins, commits))
            {
                resultAceptor.addIssue(Messages.ApkConstantWriteInTransactionCheck_Constant_write_in_transaction,
                    invocation);
            }
        }
    }

    /**
     * Вызов записи константы: цель вызова — метод «Записать»/«Установить» (рус/англ),
     * а корень цепочки обращений — «Константы»/«Constants». Псевдонимы через локальные
     * переменные не отслеживаются (упрощение относительно алгоритма АПК_00334).
     */
    private boolean isConstantWrite(Invocation invocation)
    {
        if (!(invocation.getMethodAccess() instanceof DynamicFeatureAccess methodAccess))
        {
            return false;
        }
        String methodName = methodAccess.getName();
        if (!isWriteMethod(methodName) && !isSetMethod(methodName))
        {
            return false;
        }
        String root = rootFeatureName(methodAccess.getSource());
        return isConstants(root);
    }

    /**
     * Возвращает имя корневого признака выражения: раскручивает цепочку динамических обращений
     * до первого обычного обращения к признаку; для остальных выражений возвращает {@code null}.
     */
    private String rootFeatureName(Expression expression)
    {
        Expression current = expression;
        while (current instanceof DynamicFeatureAccess featureAccess)
        {
            current = featureAccess.getSource();
        }
        if (current instanceof FeatureAccess featureAccess)
        {
            return featureAccess.getName();
        }
        return null;
    }

    /**
     * Позиция внутри лексического охвата транзакции: есть «НачатьТранзакцию» раньше
     * и «ЗафиксироватьТранзакцию» позже по тексту метода.
     */
    private boolean isBetween(int offset, List<Integer> begins, List<Integer> commits)
    {
        boolean afterBegin = false;
        for (int begin : begins)
        {
            if (begin < offset)
            {
                afterBegin = true;
                break;
            }
        }
        if (!afterBegin)
        {
            return false;
        }
        for (int commit : commits)
        {
            if (commit > offset)
            {
                return true;
            }
        }
        return false;
    }

    private boolean isBeginTransaction(String name)
    {
        return BEGIN_TRANSACTION_RU.equalsIgnoreCase(name) || BEGIN_TRANSACTION_EN.equalsIgnoreCase(name);
    }

    private boolean isCommitTransaction(String name)
    {
        return COMMIT_TRANSACTION_RU.equalsIgnoreCase(name) || COMMIT_TRANSACTION_EN.equalsIgnoreCase(name);
    }

    private boolean isWriteMethod(String name)
    {
        return WRITE_RU.equalsIgnoreCase(name) || WRITE_EN.equalsIgnoreCase(name);
    }

    private boolean isSetMethod(String name)
    {
        return SET_RU.equalsIgnoreCase(name) || SET_EN.equalsIgnoreCase(name);
    }

    private boolean isConstants(String name)
    {
        return CONSTANTS_RU.equalsIgnoreCase(name) || CONSTANTS_EN.equalsIgnoreCase(name);
    }

    private int offsetOf(EObject object)
    {
        INode node = NodeModelUtils.findActualNodeFor(object);
        return node == null ? -1 : node.getOffset();
    }
}
