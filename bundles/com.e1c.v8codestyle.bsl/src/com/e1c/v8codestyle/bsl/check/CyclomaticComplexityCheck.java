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
 *     malikov-pro - port of the BSL Language Server diagnostic CyclomaticComplexity
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.BinaryExpression;
import com._1c.g5.v8.dt.bsl.model.BinaryOperation;
import com._1c.g5.v8.dt.bsl.model.Conditional;
import com._1c.g5.v8.dt.bsl.model.IfStatement;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.LoopStatement;
import com._1c.g5.v8.dt.bsl.model.Method;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.WrongParameterException;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: цикломатическая сложность метода выше порога. Считается как
 * {@code 1 + число ветвей «Если»/«ИначеЕсли» + число циклов + число «И»/«ИЛИ»
 * + число тернарников}.
 * <p>
 * Перенос диагностики BSL Language Server CyclomaticComplexity
 * (тип CODE_SMELL, серьёзность CRITICAL). Порог — параметр (по умолчанию 20,
 * как в LS).
 *
 * @author malikov-pro
 */
public class CyclomaticComplexityCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "cyclomatic-complexity"; //$NON-NLS-1$

    private static final String PARAM_THRESHOLD = "complexityThreshold"; //$NON-NLS-1$

    private static final int DEFAULT_THRESHOLD = 20;

    /**
     * Instantiates a new check.
     */
    public CyclomaticComplexityCheck()
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
        builder.title(Messages.CyclomaticComplexityCheck_title)
            .description(Messages.CyclomaticComplexityCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(METHOD)
            .parameter(PARAM_THRESHOLD, Integer.class, String.valueOf(DEFAULT_THRESHOLD),
                Messages.CyclomaticComplexityCheck_Complexity_threshold);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Method method = (Method)object;
        int threshold = threshold(parameters);

        int branches = 0;
        for (IfStatement ifStatement : EcoreUtil2.getAllContentsOfType(method, IfStatement.class))
        {
            branches += 1 + ifStatement.getElsIfParts().size();
        }
        int loops = EcoreUtil2.getAllContentsOfType(method, LoopStatement.class).size();
        int logicOps = (int) EcoreUtil2.getAllContentsOfType(method, BinaryExpression.class).stream()
            .filter(expression -> expression.getOperation() == BinaryOperation.AND
                || expression.getOperation() == BinaryOperation.OR)
            .count();
        int ternaries = EcoreUtil2.getAllContentsOfType(method, Invocation.class).stream()
            .filter(UselessTernaryOperatorCheck::isTernaryOperator)
            .mapToInt(expression -> 1)
            .sum();

        int complexity = 1 + branches + loops + logicOps + ternaries;
        if (complexity > threshold)
        {
            String message = MessageFormat.format(Messages.CyclomaticComplexityCheck_Cyclomatic_complexity,
                Integer.valueOf(complexity), Integer.valueOf(threshold));
            resultAceptor.addIssue(message, method);
        }
    }

    private static int threshold(ICheckParameters parameters)
    {
        try
        {
            return parameters.getInt(PARAM_THRESHOLD);
        }
        catch (WrongParameterException e)
        {
            return DEFAULT_THRESHOLD;
        }
    }
}
