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
 *     malikov-pro - port of the APK check АПК_00334
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Function;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: в обработчике проведения документа (ОбработкаПроведения) не следует
 * явно записывать наборы записей регистров методом Записать (рус/англ).
 * Запись движений должна выполняться системой неявно при завершении процедуры
 * проведения; явная запись при параллельной работе пользователей может
 * приводить к взаимным блокировкам.
 * <p>
 * Перенос проверки АПК_00334 (статья 450 стандарта 1С). Как и в алгоритме АПК,
 * проверяется вызов {@code Движения.<Регистр>.Записать()}, а также запись через
 * локальную переменную — псевдоним, присвоенный из коллекции движений,
 * включая цепочки псевдонимов: ближайшее предшествующее присваивание
 * {@code имя = выражение} подставляет корень выражения вместо имени.
 * Записи вне обработчика проведения и закомментированные записи не проверяются.
 *
 * @author malikov-pro
 */
public class ApkPostingsWriteOrderCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00334-postings-write-order"; //$NON-NLS-1$

    private static final String HANDLER_NAME_RU = "ОбработкаПроведения"; //$NON-NLS-1$

    private static final String HANDLER_NAME_EN = "Posting"; //$NON-NLS-1$

    private static final String WRITE_NAME_RU = "Записать"; //$NON-NLS-1$

    private static final String WRITE_NAME_EN = "Write"; //$NON-NLS-1$

    private static final String POSTINGS_RU = "Движения"; //$NON-NLS-1$

    private static final String POSTINGS_EN = "RegisterRecords"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public ApkPostingsWriteOrderCheck()
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
        builder.title(Messages.ApkPostingsWriteOrderCheck_title)
            .description(Messages.ApkPostingsWriteOrderCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
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
        if (method instanceof Function || !isPostingHandler(method.getName()))
        {
            return;
        }

        List<SimpleStatement> assignments = EcoreUtil2.getAllContentsOfType(method, SimpleStatement.class);
        for (Invocation invocation : EcoreUtil2.getAllContentsOfType(method, Invocation.class))
        {
            if (progressMonitor.isCanceled())
            {
                return;
            }
            checkInvocation(invocation, assignments, resultAceptor);
        }
    }

    private void checkInvocation(Invocation invocation, List<SimpleStatement> assignments,
        ResultAcceptor resultAceptor)
    {
        // Явная запись — это вызов метода целевого объекта: цель.Записать(), как поиск ".Записать(" в АПК
        if (!(invocation.getMethodAccess() instanceof DynamicFeatureAccess methodAccess)
            || !isWriteMethod(methodAccess.getName()))
        {
            return;
        }

        String rootName = rootFeatureName(methodAccess.getSource());
        if (rootName == null || rootName.isEmpty())
        {
            return;
        }
        if (isPostings(rootName) || chasesToPostings(rootName, offsetOf(invocation), assignments))
        {
            resultAceptor.addIssue(Messages.ApkPostingsWriteOrderCheck_Explicit_postings_write, invocation);
        }
    }

    /**
     * Прослеживает псевдонимы цели записи назад по присваиваниям, как в алгоритме АПК: каждое ближайшее
     * предшествующее присваивание {@code имя = выражение} подставляет вместо имени корень выражения;
     * просмотр заканчивается, когда корнем оказывается коллекция движений.
     */
    private boolean chasesToPostings(String startName, int invocationOffset, List<SimpleStatement> assignments)
    {
        int limit = invocationOffset;
        String currentName = startName;
        while (currentName != null && !currentName.isEmpty())
        {
            SimpleStatement nearest = null;
            int nearestOffset = -1;
            for (SimpleStatement statement : assignments)
            {
                int offset = offsetOf(statement);
                if (offset >= 0 && offset < limit && offset > nearestOffset
                    && currentName.equalsIgnoreCase(rootFeatureName(statement.getLeft())))
                {
                    nearest = statement;
                    nearestOffset = offset;
                }
            }
            if (nearest == null)
            {
                return false;
            }
            limit = nearestOffset;
            currentName = rootFeatureName(nearest.getRight());
            if (currentName != null && isPostings(currentName))
            {
                return true;
            }
        }
        return false;
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

    private boolean isPostingHandler(String name)
    {
        return HANDLER_NAME_RU.equalsIgnoreCase(name) || HANDLER_NAME_EN.equalsIgnoreCase(name);
    }

    private boolean isWriteMethod(String name)
    {
        return WRITE_NAME_RU.equalsIgnoreCase(name) || WRITE_NAME_EN.equalsIgnoreCase(name);
    }

    private boolean isPostings(String name)
    {
        return POSTINGS_RU.equalsIgnoreCase(name) || POSTINGS_EN.equalsIgnoreCase(name);
    }

    private int offsetOf(EObject object)
    {
        INode node = NodeModelUtils.findActualNodeFor(object);
        return node == null ? -1 : node.getOffset();
    }

}
