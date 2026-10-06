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
 *     malikov-pro - port of the BSL Language Server diagnostic IfConditionComplexity
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.CONDITIONAL__PREDICATE;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.IF_STATEMENT;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.BinaryExpression;
import com._1c.g5.v8.dt.bsl.model.BinaryOperation;
import com._1c.g5.v8.dt.bsl.model.Conditional;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.IfStatement;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.WrongParameterException;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: сложное условие «Если»/«ИначеЕсли» — количество операций
 * «И»/«ИЛИ» в одном условии превышает максимум.
 * <p>
 * Перенос диагностики BSL Language Server IfConditionComplexity
 * (тип CODE_SMELL, серьёзность MINOR). Порог — параметр (по умолчанию 3
 * операции, как в LS). Quick fix не предусмотрен: разбиение условия —
 * решение пользователя.
 *
 * @author malikov-pro
 */
public class IfConditionComplexityCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "if-condition-complexity"; //$NON-NLS-1$

    private static final String PARAM_MAX_CONDITIONS = "maxIfConditionComplexity"; //$NON-NLS-1$

    private static final int DEFAULT_MAX = 3;

    /**
     * Instantiates a new check.
     */
    public IfConditionComplexityCheck()
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
        builder.title(Messages.IfConditionComplexityCheck_title)
            .description(Messages.IfConditionComplexityCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(IF_STATEMENT)
            .parameter(PARAM_MAX_CONDITIONS, Integer.class, String.valueOf(DEFAULT_MAX),
                Messages.IfConditionComplexityCheck_Max_condition_complexity);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        IfStatement ifStatement = (IfStatement)object;
        int max = maxConditionComplexity(parameters);

        checkPredicate(ifStatement.getIfPart(), max, resultAceptor);
        for (Conditional conditional : ifStatement.getElsIfParts())
        {
            checkPredicate(conditional, max, resultAceptor);
        }
    }

    private void checkPredicate(Conditional conditional, int max, ResultAcceptor resultAceptor)
    {
        if (conditional.getPredicate() == null)
        {
            return;
        }
        Expression predicate = conditional.getPredicate();
        // считаем и сам узел-предикат, и вложенные (getAllContentsOfType не включает корень)
        int operations = EcoreUtil2.getAllContentsOfType(predicate, BinaryExpression.class).stream()
            .filter(expression -> expression.getOperation() == BinaryOperation.AND
                || expression.getOperation() == BinaryOperation.OR)
            .mapToInt(expression -> 1)
            .sum();
        if (predicate instanceof BinaryExpression root
            && (root.getOperation() == BinaryOperation.AND || root.getOperation() == BinaryOperation.OR))
        {
            operations++;
        }
        if (operations > max)
        {
            String message = MessageFormat.format(Messages.IfConditionComplexityCheck_Too_many_conditions,
                Integer.valueOf(operations), Integer.valueOf(max));
            resultAceptor.addIssue(message, conditional, CONDITIONAL__PREDICATE);
        }
    }

    private static int maxConditionComplexity(ICheckParameters parameters)
    {
        try
        {
            return parameters.getInt(PARAM_MAX_CONDITIONS);
        }
        catch (WrongParameterException e)
        {
            return DEFAULT_MAX;
        }
    }
}
