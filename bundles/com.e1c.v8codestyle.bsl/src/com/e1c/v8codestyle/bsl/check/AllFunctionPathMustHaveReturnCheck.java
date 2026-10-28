/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FUNCTION;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.NAMED_ELEMENT__NAME;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.BooleanLiteral;
import com._1c.g5.v8.dt.bsl.model.Conditional;
import com._1c.g5.v8.dt.bsl.model.Function;
import com._1c.g5.v8.dt.bsl.model.IfPreprocessor;
import com._1c.g5.v8.dt.bsl.model.IfStatement;
import com._1c.g5.v8.dt.bsl.model.PreprocessorIfConditional;
import com._1c.g5.v8.dt.bsl.model.RaiseStatement;
import com._1c.g5.v8.dt.bsl.model.ReturnStatement;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;
import com.e1c.v8codestyle.internal.bsl.cfg.CfgBuilder;
import com.e1c.v8codestyle.internal.bsl.cfg.CfgGraph;

/**
 * Not every function execution path has an explicit return. Port of pinned BSL LS
 * AllFunctionPathMustHaveReturn: functions that have at least one Return but can
 * reach the end of the body without returning are flagged on the function name;
 * functions without any Return stay to the simple function-should-have-return check.
 */
public class AllFunctionPathMustHaveReturnCheck extends BasicCheck
{
    public static final String CHECK_ID = "all-function-path-must-have-return"; //$NON-NLS-1$

    public static final String PARAM_LOOPS_EXECUTED_AT_LEAST_ONCE = "loopsExecutedAtLeastOnce"; //$NON-NLS-1$

    public static final String PARAM_IGNORE_MISSING_ELSE_ON_EXIT = "ignoreMissingElseOnExit"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.AllFunctionPathMustHaveReturnCheck_title)
            .description(Messages.AllFunctionPathMustHaveReturnCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .parameter(PARAM_LOOPS_EXECUTED_AT_LEAST_ONCE, Boolean.class, "true", //$NON-NLS-1$
                Messages.AllFunctionPathMustHaveReturnCheck_Loops_executed_at_least_once)
            .parameter(PARAM_IGNORE_MISSING_ELSE_ON_EXIT, Boolean.class, "false", //$NON-NLS-1$
                Messages.AllFunctionPathMustHaveReturnCheck_Ignore_missing_else_on_exit)
            .module()
            .checkedObjectType(FUNCTION);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        Function function = (Function)object;
        if (monitor.isCanceled() || hasSyntaxErrors(function))
        {
            return;
        }
        // deduplication with the simple function-should-have-return: only functions
        // that already have at least one Return are checked here (as in LS)
        if (EcoreUtil2.getAllContentsOfType(function, ReturnStatement.class).isEmpty())
        {
            return;
        }
        boolean loopsOnce = parameters.getBoolean(PARAM_LOOPS_EXECUTED_AT_LEAST_ONCE);
        boolean ignoreMissingElse = parameters.getBoolean(PARAM_IGNORE_MISSING_ELSE_ON_EXIT);
        CfgGraph graph = CfgBuilder.build(function, true);
        for (CfgGraph.Edge edge : graph.incomingEdgesOf(graph.getExit()))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (nonExplicitExit(edge, graph, loopsOnce, ignoreMissingElse))
            {
                acceptor.addIssue(MessageFormat.format(Messages.AllFunctionPathMustHaveReturnCheck_message,
                    function.getName()), function, NAMED_ELEMENT__NAME);
                return;
            }
        }
    }

    private static boolean hasSyntaxErrors(Function function)
    {
        var node = NodeModelUtils.findActualNodeFor(function);
        if (node == null)
        {
            return true;
        }
        for (var leaf = node.getLeafNodes().iterator(); leaf.hasNext();)
        {
            if (leaf.next().getSyntaxErrorMessage() != null)
            {
                return true;
            }
        }
        return false;
    }

    private boolean nonExplicitExit(CfgGraph.Edge edge, CfgGraph graph, boolean loopsOnce,
        boolean ignoreMissingElse)
    {
        CfgGraph.Vertex source = edge.source();
        if (source instanceof CfgGraph.BasicBlock block)
        {
            if (block.statements().isEmpty())
            {
                return false;
            }
            EObject last = block.statements().get(block.statements().size() - 1);
            return !(last instanceof ReturnStatement) && !(last instanceof RaiseStatement);
        }
        if (source instanceof CfgGraph.Loop loop)
        {
            return !loopsOnce && !isEndlessWhile(loop);
        }
        if (source instanceof CfgGraph.Conditional conditional)
        {
            if (conditional.preprocessor())
            {
                // pinned LS: PreprocessorConditionVertex is not a ConditionalVertex subclass,
                // its classifier falls to the default branch and flags the function
                return true;
            }
            return !ignoreMissingElse && graph.hasEdge(source, graph.getExit(), CfgGraph.EdgeType.FALSE_BRANCH)
                && isElseIfBranch(conditional);
        }
        // pinned LS: any other vertex kind (try/except, label) falls to the default branch
        return true;
    }

    private static boolean isEndlessWhile(CfgGraph.Loop loop)
    {
        return loop.whileLoop() && loop.predicate() instanceof BooleanLiteral literal && literal.isIsTrue();
    }

    private static boolean isElseIfBranch(CfgGraph.Conditional conditional)
    {
        if (conditional.ast() instanceof Conditional branch
            && branch.eContainer() instanceof IfStatement ifStatement)
        {
            return ifStatement.getElsIfParts().contains(branch);
        }
        if (conditional.ast() instanceof PreprocessorIfConditional branch
            && branch.eContainer() instanceof IfPreprocessor preprocessor)
        {
            return preprocessor.getElsIfParts().contains(branch);
        }
        return false;
    }
}
