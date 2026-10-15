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

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Conditional;
import com._1c.g5.v8.dt.bsl.model.ForStatement;
import com._1c.g5.v8.dt.bsl.model.IfStatement;
import com._1c.g5.v8.dt.bsl.model.IfPreprocessorStatement;
import com._1c.g5.v8.dt.bsl.model.LabeledStatement;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.RegionPreprocessorStatement;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com._1c.g5.v8.dt.bsl.model.TryExceptStatement;
import com._1c.g5.v8.dt.bsl.model.WhileStatement;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
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
 * как в LS); отмечаются конечные составные блоки глубже порога в методах
 * и теле модуля. Родительские диапазоны LS не переносятся.
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
            .checkedObjectType(MODULE)
            .parameter(PARAM_MAX_LEVEL, Integer.class, String.valueOf(DEFAULT_MAX_LEVEL),
                Messages.NestedStatementsCheck_Max_allowed_level);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Module module = (Module)object;
        int maxLevel = maxLevel(parameters);
        for (Method method : EcoreUtil2.getAllContentsOfType(module, Method.class))
        {
            walk(method.allStatements(), 1, maxLevel, resultAceptor, progressMonitor, module);
        }
        walk(module.allStatements(), 1, maxLevel, resultAceptor, progressMonitor, module);
    }

    private void walk(List<Statement> statements, int depth, int maxLevel, ResultAcceptor resultAceptor,
        IProgressMonitor monitor, Module module)
    {
        for (Statement statement : expandPreprocessor(statements))
        {
            statement = effectiveStatement(statement);
            if (monitor.isCanceled())
            {
                return;
            }
            if (!isCompound(statement))
            {
                continue;
            }
            List<Statement> nested = expandPreprocessor(nestedStatements(statement));
            boolean hasNestedCompound = nested.stream().map(NestedStatementsCheck::effectiveStatement)
                .anyMatch(NestedStatementsCheck::isCompound);
            if (depth > maxLevel && !hasNestedCompound)
            {
                String message = MessageFormat.format(Messages.NestedStatementsCheck_Too_many_nested_statements,
                    Integer.valueOf(depth), Integer.valueOf(maxLevel));
                INode node = NodeModelUtils.getNode(statement);
                if (node != null)
                {
                    for (ILeafNode leaf : node.getLeafNodes())
                    {
                        if (!leaf.isHidden())
                        {
                            resultAceptor.addIssue(new BslDirectLocationIssue(message,
                                new DirectLocation(leaf.getOffset(), leaf.getLength(), leaf.getStartLine(), module)));
                            break;
                        }
                    }
                }
            }
            walk(nested, depth + 1, maxLevel, resultAceptor, monitor, module);
        }
    }

    private static boolean isCompound(Statement statement)
    {
        return statement instanceof IfStatement || statement instanceof ForStatement
            || statement instanceof TryExceptStatement || statement instanceof WhileStatement;
    }

    /** Preprocessor containers do not add runtime nesting depth. */
    private static List<Statement> expandPreprocessor(List<Statement> statements)
    {
        List<Statement> result = new ArrayList<>();
        for (Statement statement : statements)
        {
            Statement effective = effectiveStatement(statement);
            if (effective instanceof IfPreprocessorStatement || effective instanceof RegionPreprocessorStatement)
            {
                collectWrappedStatements(effective, result);
            }
            else if (effective != null)
            {
                result.add(effective);
            }
        }
        return result;
    }

    private static void collectWrappedStatements(EObject wrapper, List<Statement> result)
    {
        for (EObject child : wrapper.eContents())
        {
            if (child instanceof Statement statement)
            {
                result.addAll(expandPreprocessor(List.of(statement)));
            }
            else if (!(child instanceof Method))
            {
                collectWrappedStatements(child, result);
            }
        }
    }

    private static Statement effectiveStatement(Statement statement)
    {
        Statement result = statement;
        while (result instanceof LabeledStatement labeled)
        {
            result = labeled.getStatement();
        }
        return result;
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
