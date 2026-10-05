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
 *     malikov-pro - port of the BSL Language Server diagnostic NestedTernaryOperator
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.dt.bsl.model.Conditional;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: вложенный тернарный оператор и тернарный оператор в условии
 * «Если»/«ИначеЕсли». Вложенные {@code ?(...)} читаются с трудом — в LS
 * фиксируются как вложенный тернарник; тернарник в условии «Если» — как
 * нежелательное усложнение условия.
 * <p>
 * Перенос диагностики BSL Language Server NestedTernaryOperator
 * (тип CODE_SMELL, серьёзность MAJOR). Семантика LS: фиксируются ВСЕ
 * тернарники внутри условий if/elsif-ветвей и все тернарники, вложенные
 * в другой тернарник (кроме самого внешнего).
 *
 * @author malikov-pro
 */
public class NestedTernaryOperatorCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "nested-ternary-operator"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public NestedTernaryOperatorCheck()
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
        builder.title(Messages.NestedTernaryOperatorCheck_title)
            .description(Messages.NestedTernaryOperatorCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(INVOCATION);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Invocation invocation = (Invocation)object;
        if (!UselessTernaryOperatorCheck.isTernaryOperator(invocation))
        {
            return;
        }
        if (isNestedOrInIfPredicate(invocation))
        {
            resultAceptor.addIssue(Messages.NestedTernaryOperatorCheck_Nested_ternary_operator, invocation);
        }
    }

    /**
     * Тернарник находится внутри другого тернарника или в условии
     * if/elsif-ветви (до ближайшего оператора или метода).
     *
     * @param invocation тернарный оператор, не может быть {@code null}.
     * @return {@code true}, если фиксируется.
     */
    private static boolean isNestedOrInIfPredicate(Invocation invocation)
    {
        EObject ancestor = invocation.eContainer();
        while (ancestor != null && !(ancestor instanceof Statement) && !(ancestor instanceof Method))
        {
            if (ancestor instanceof Conditional)
            {
                // условие if/elsif-ветви (условие «Пока» оформлено иначе и не фиксируется)
                return true;
            }
            if (ancestor instanceof Invocation outer && UselessTernaryOperatorCheck.isTernaryOperator(outer))
            {
                return true;
            }
            ancestor = ancestor.eContainer();
        }
        return false;
    }
}
