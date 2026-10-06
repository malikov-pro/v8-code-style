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
 *     malikov-pro - port of the BSL Language Server diagnostic NestedStatements
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.dt.bsl.model.Conditional;
import com._1c.g5.v8.dt.bsl.model.ForStatement;
import com._1c.g5.v8.dt.bsl.model.IfStatement;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com._1c.g5.v8.dt.bsl.model.TryExceptStatement;
import com._1c.g5.v8.dt.bsl.model.WhileStatement;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.WrongParameterException;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: вложенность операторов «Если»/циклов/«Попытки» глубже максимума
 * затрудняет чтение и сопровождение.
 * <p>
 * Перенос диагностики BSL Language Server NestedStatements
 * (тип CODE_SMELL, серьёзность CRITICAL). Порог — параметр (по умолчанию 4,
 * как в LS); фиксируются операторы на уровнях глубже порога.
 *
 * @author malikov-pro
 */
public class NestedStatementsCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "nested-statements"; //$NON-NLS-1$

    private static final String PARAM_MAX_LEVEL = "maxAllowedLevel"; //$NON-NLS-1$

    private static final int DEFAULT_MAX_LEVEL = 4;

    /**
     * Instantiates a new check.
     */
    public NestedStatementsCheck()
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
        builder.title(Messages.NestedStatementsCheck_title)
            .description(Messages.NestedStatementsCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(METHOD)
            .parameter(PARAM_MAX_LEVEL, Integer.class, String.valueOf(DEFAULT_MAX_LEVEL),
                Messages.NestedStatementsCheck_Max_allowed_level);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Method method = (Method)object;
        int maxLevel = maxLevel(parameters);
        walk(method.allStatements(), 1, maxLevel, resultAceptor);
    }

    private void walk(List<Statement> statements, int depth, int maxLevel, ResultAcceptor resultAceptor)
    {
        for (Statement statement : statements)
        {
            if (!isCompound(statement))
            {
                continue;
            }
            if (depth > maxLevel)
            {
                String message = MessageFormat.format(Messages.NestedStatementsCheck_Too_many_nested_statements,
                    Integer.valueOf(depth), Integer.valueOf(maxLevel));
                resultAceptor.addIssue(message, statement);
            }
            walk(nestedStatements(statement), depth + 1, maxLevel, resultAceptor);
        }
    }

    private static boolean isCompound(Statement statement)
    {
        return statement instanceof IfStatement || statement instanceof ForStatement
            || statement instanceof TryExceptStatement || statement instanceof WhileStatement;
    }

    private static List<Statement> nestedStatements(Statement statement)
    {
        if (statement instanceof IfStatement ifStatement)
        {
            List<Statement> result = new java.util.ArrayList<>();
            if (ifStatement.getIfPart() != null)
            {
                result.addAll(ifStatement.getIfPart().getStatements());
            }
            for (Conditional conditional : ifStatement.getElsIfParts())
            {
                result.addAll(conditional.getStatements());
            }
            result.addAll(ifStatement.getElseStatements());
            return result;
        }
        if (statement instanceof ForStatement loop)
        {
            return loop.getStatements();
        }
        if (statement instanceof TryExceptStatement tryStatement)
        {
            List<Statement> result = new java.util.ArrayList<>(tryStatement.getTryStatements());
            result.addAll(tryStatement.getExceptStatements());
            return result;
        }
        if (statement instanceof WhileStatement whileStatement)
        {
            return whileStatement.getStatements();
        }
        return List.of();
    }

    private static int maxLevel(ICheckParameters parameters)
    {
        try
        {
            return parameters.getInt(PARAM_MAX_LEVEL);
        }
        catch (WrongParameterException e)
        {
            return DEFAULT_MAX_LEVEL;
        }
    }
}
