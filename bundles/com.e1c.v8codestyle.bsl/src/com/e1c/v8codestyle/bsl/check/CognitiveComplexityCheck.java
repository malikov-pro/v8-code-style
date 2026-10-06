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
 *     malikov-pro - port of the BSL Language Server diagnostic CognitiveComplexity
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.BinaryExpression;
import com._1c.g5.v8.dt.bsl.model.BinaryOperation;
import com._1c.g5.v8.dt.bsl.model.Conditional;
import com._1c.g5.v8.dt.bsl.model.IfStatement;
import com._1c.g5.v8.dt.bsl.model.LoopStatement;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com._1c.g5.v8.dt.bsl.model.TryExceptStatement;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.WrongParameterException;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: когнитивная сложность метода выше порога. Упрощённая формула
 * (полная формула Sonar/LS не переносится без CFG): каждая структура
 * «Если»/«ИначеЕсли»/цикл/«Попытка» даёт {@code 1 + (вложенность - 1)},
 * каждое «И»/«ИЛИ» даёт 1.
 * <p>
 * Перенос диагностики BSL Language Server CognitiveComplexity
 * (тип CODE_SMELL, серьёзность CRITICAL). Порог — параметр (по умолчанию 15,
 * как в LS).
 *
 * @author malikov-pro
 */
public class CognitiveComplexityCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "cognitive-complexity"; //$NON-NLS-1$

    private static final String PARAM_THRESHOLD = "complexityThreshold"; //$NON-NLS-1$

    private static final int DEFAULT_THRESHOLD = 15;

    /**
     * Instantiates a new check.
     */
    public CognitiveComplexityCheck()
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
        builder.title(Messages.CognitiveComplexityCheck_title)
            .description(Messages.CognitiveComplexityCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(METHOD)
            .parameter(PARAM_THRESHOLD, Integer.class, String.valueOf(DEFAULT_THRESHOLD),
                Messages.CognitiveComplexityCheck_Complexity_threshold);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Method method = (Method)object;
        int threshold = threshold(parameters);

        int complexity = sumStructures(method.allStatements(), 1);
        complexity += EcoreUtil2.getAllContentsOfType(method, BinaryExpression.class).stream()
            .filter(expression -> expression.getOperation() == BinaryOperation.AND
                || expression.getOperation() == BinaryOperation.OR)
            .mapToInt(expression -> 1)
            .sum();

        if (complexity > threshold)
        {
            String message = MessageFormat.format(Messages.CognitiveComplexityCheck_Cognitive_complexity,
                Integer.valueOf(complexity), Integer.valueOf(threshold));
            resultAceptor.addIssue(message, method);
        }
    }

    /**
     * Сумма по структурам: «Если»/«ИначеЕсли»/цикл/«Попытка» на глубине d
     * добавляет d (базовая единица плюс бонус за вложенность).
     */
    private static int sumStructures(List<Statement> statements, int depth)
    {
        int sum = 0;
        for (Statement statement : statements)
        {
            if (statement instanceof IfStatement ifStatement)
            {
                sum += depth;
                if (ifStatement.getIfPart() != null)
                {
                    sum += sumStructures(ifStatement.getIfPart().getStatements(), depth + 1);
                }
                for (Conditional conditional : ifStatement.getElsIfParts())
                {
                    sum += depth;
                    sum += sumStructures(conditional.getStatements(), depth + 1);
                }
                sum += sumStructures(ifStatement.getElseStatements(), depth + 1);
            }
            else if (statement instanceof LoopStatement loop)
            {
                sum += depth;
                sum += sumStructures(loop.getStatements(), depth + 1);
            }
            else if (statement instanceof TryExceptStatement tryStatement)
            {
                sum += depth;
                sum += sumStructures(tryStatement.getTryStatements(), depth + 1);
                sum += sumStructures(tryStatement.getExceptStatements(), depth + 1);
            }
        }
        return sum;
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
