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
 *     malikov-pro - port of the BSL Language Server diagnostic TernaryOperatorUsage
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;

import org.eclipse.core.runtime.IProgressMonitor;

import com._1c.g5.v8.dt.bsl.model.Invocation;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.OptInCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: использование тернарного оператора {@code ?(Условие, А, Б)} —
 * рекомендуется конструкция «Если … Тогда … Иначе».
 * <p>
 * Перенос диагностики BSL Language Server TernaryOperatorUsage
 * (тип CODE_SMELL, серьёзность MINOR). Как и в LS, проверка по умолчанию
 * ВЫКЛЮЧЕНА ({@link OptInCheckExtension}) — включается в настройках проверок.
 *
 * @author malikov-pro
 */
public class TernaryOperatorUsageCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "ternary-operator-usage"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public TernaryOperatorUsageCheck()
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
        builder.title(Messages.TernaryOperatorUsageCheck_title)
            .description(Messages.TernaryOperatorUsageCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new OptInCheckExtension())
            .module()
            .checkedObjectType(INVOCATION);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Invocation invocation = (Invocation)object;
        if (UselessTernaryOperatorCheck.isTernaryOperator(invocation))
        {
            resultAceptor.addIssue(Messages.TernaryOperatorUsageCheck_Use_if_instead_of_ternary_operator,
                invocation);
        }
    }
}
