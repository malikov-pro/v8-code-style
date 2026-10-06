/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: 1C-Soft LLC
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the APK check АПК_00205
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.Conditional;
import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.IfStatement;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.WhileStatement;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: отсутствие строк в результате запроса следует проверять методом
 * {@code Пустой()}, а не получать выборку и анализировать её в условии:
 * <pre>
 * Выборка = Запрос.Выполнить().Выбрать();
 * Если Выборка.Следующий() Тогда ...
 * </pre>
 * Получение выборки (или выгрузка результата) затрачивает дополнительное время.
 * <p>
 * Перенос проверки АПК_00205 (статья 438 стандарта 1С). Реализуемый статически
 * вариант: локальная переменная метода, которой присвоен результат цепочки
 * {@code Запрос.Выполнить().Выбрать()}, используется в условии Если/Пока
 * вызовом {@code Следующий()}. Замечание ставится на условие. Глубина
 * трекинга — одно присваивание в пределах метода, без межпроцедурного
 * анализа; тип источника не вычисляется (упрощение).
 *
 * @author malikov-pro
 */
public class ApkQueryEmptyResultCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00205-query-empty-result"; //$NON-NLS-1$

    private static final String SELECT_RU = "выбрать"; //$NON-NLS-1$
    private static final String SELECT_EN = "select"; //$NON-NLS-1$

    private static final String EXECUTE_RU = "выполнить"; //$NON-NLS-1$
    private static final String EXECUTE_EN = "execute"; //$NON-NLS-1$

    private static final String NEXT_RU = "следующий"; //$NON-NLS-1$
    private static final String NEXT_EN = "next"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public ApkQueryEmptyResultCheck()
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
        builder.title(Messages.ApkQueryEmptyResultCheck_title)
            .description(Messages.ApkQueryEmptyResultCheck_description)
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

        Map<String, StaticFeatureAccess> selectionVariables = collectSelectionVariables(method);
        if (selectionVariables.isEmpty())
        {
            return;
        }

        Set<EObject> reportedConditions = new HashSet<>();
        for (IfStatement ifStatement : EcoreUtil2.getAllContentsOfType(method, IfStatement.class))
        {
            if (progressMonitor.isCanceled())
            {
                return;
            }
            checkCondition(ifStatement.getIfPart(), selectionVariables, reportedConditions, resultAceptor);
            for (Conditional elsIfPart : ifStatement.getElsIfParts())
            {
                checkCondition(elsIfPart, selectionVariables, reportedConditions, resultAceptor);
            }
        }
        for (WhileStatement whileStatement : EcoreUtil2.getAllContentsOfType(method, WhileStatement.class))
        {
            if (progressMonitor.isCanceled())
            {
                return;
            }
            if (usesSelectionIteration(whileStatement.getPredicate(), selectionVariables))
            {
                reportCondition(whileStatement.getPredicate(), reportedConditions, resultAceptor);
            }
        }
    }

    private Map<String, StaticFeatureAccess> collectSelectionVariables(Method method)
    {
        Map<String, StaticFeatureAccess> result = new HashMap<>();
        for (SimpleStatement statement : EcoreUtil2.getAllContentsOfType(method, SimpleStatement.class))
        {
            if (!(statement.getLeft() instanceof StaticFeatureAccess target))
            {
                continue;
            }
            if (isExecuteSelectChain(statement.getRight()))
            {
                result.put(target.getName().toLowerCase(Locale.ROOT), target);
            }
        }
        return result;
    }

    private boolean isExecuteSelectChain(Expression expression)
    {
        if (!(expression instanceof Invocation selectInvocation)
            || !(selectInvocation.getMethodAccess() instanceof DynamicFeatureAccess selectAccess))
        {
            return false;
        }
        String selectName = selectAccess.getName().toLowerCase(Locale.ROOT);
        if (!SELECT_RU.equals(selectName) && !SELECT_EN.equals(selectName))
        {
            return false;
        }
        if (!(selectAccess.getSource() instanceof Invocation executeInvocation)
            || !(executeInvocation.getMethodAccess() instanceof DynamicFeatureAccess executeAccess))
        {
            return false;
        }
        String executeName = executeAccess.getName().toLowerCase(Locale.ROOT);
        return EXECUTE_RU.equals(executeName) || EXECUTE_EN.equals(executeName);
    }

    private void checkCondition(Conditional conditional, Map<String, StaticFeatureAccess> selectionVariables,
        Set<EObject> reportedConditions, ResultAcceptor resultAceptor)
    {
        if (conditional == null)
        {
            return;
        }
        if (usesSelectionIteration(conditional.getPredicate(), selectionVariables))
        {
            reportCondition(conditional.getPredicate(), reportedConditions, resultAceptor);
        }
    }

    private boolean usesSelectionIteration(Expression predicate, Map<String, StaticFeatureAccess> selectionVariables)
    {
        if (predicate == null)
        {
            return false;
        }
        // Корень-предикат может сам быть вызовом (Если Выборка.Следующий() Тогда) —
        // getAllContentsOfType корень не включает, учитываем его отдельно.
        List<Invocation> invocations = new ArrayList<>();
        if (predicate instanceof Invocation rootInvocation)
        {
            invocations.add(rootInvocation);
        }
        invocations.addAll(EcoreUtil2.getAllContentsOfType(predicate, Invocation.class));
        for (Invocation invocation : invocations)
        {
            if (!(invocation.getMethodAccess() instanceof DynamicFeatureAccess methodAccess))
            {
                continue;
            }
            String methodName = methodAccess.getName().toLowerCase(Locale.ROOT);
            if (!NEXT_RU.equals(methodName) && !NEXT_EN.equals(methodName))
            {
                continue;
            }
            if (methodAccess.getSource() instanceof StaticFeatureAccess source)
            {
                String sourceName = source.getName().toLowerCase(Locale.ROOT);
                if (selectionVariables.containsKey(sourceName))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private void reportCondition(Expression predicate, Set<EObject> reportedConditions, ResultAcceptor resultAceptor)
    {
        EObject condition = predicate;
        if (reportedConditions.add(condition))
        {
            resultAceptor.addIssue(Messages.ApkQueryEmptyResultCheck_Use_query_result_Empty_method, condition);
        }
    }
}
