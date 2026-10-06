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
 *     malikov-pro - port of the BSL Language Server diagnostic IfElseDuplicatedCondition
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.CONDITIONAL__PREDICATE;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.IF_STATEMENT;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Conditional;
import com._1c.g5.v8.dt.bsl.model.IfStatement;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: повторяющиеся условия в операторе «Если … Тогда … ИначеЕсли».
 * Если условие «ИначеЕсли» совпадает с условием выше, оно никогда не будет
 * проверено — ветка недостижима.
 * <p>
 * Перенос диагностики BSL Language Server IfElseDuplicatedCondition
 * (тип CODE_SMELL, серьёзность MAJOR).
 *
 * @author malikov-pro
 */
public class IfElseDuplicatedConditionCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "if-else-duplicated-condition"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public IfElseDuplicatedConditionCheck()
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
        builder.title(Messages.IfElseDuplicatedConditionCheck_title)
            .description(Messages.IfElseDuplicatedConditionCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(IF_STATEMENT);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        IfStatement ifStatement = (IfStatement)object;
        List<Conditional> conditionals = new ArrayList<>();
        if (ifStatement.getIfPart() != null)
        {
            conditionals.add(ifStatement.getIfPart());
        }
        conditionals.addAll(ifStatement.getElsIfParts());

        for (int i = 1; i < conditionals.size(); i++)
        {
            Conditional current = conditionals.get(i);
            if (current.getPredicate() == null)
            {
                continue;
            }
            String currentText = predicateText(current);
            for (int j = 0; j < i; j++)
            {
                if (conditionals.get(j).getPredicate() == null)
                {
                    continue;
                }
                if (currentText.equals(predicateText(conditionals.get(j))))
                {
                    String message = MessageFormat.format(
                        Messages.IfElseDuplicatedConditionCheck_Duplicate_condition_of_If_statement, currentText);
                    resultAceptor.addIssue(message, current, CONDITIONAL__PREDICATE);
                    break;
                }
            }
        }
    }

    private String predicateText(Conditional conditional)
    {
        return NodeModelUtils.findActualNodeFor(conditional.getPredicate()).getText().trim();
    }
}
