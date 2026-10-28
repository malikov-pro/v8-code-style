/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
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
 * Unreachable code detected by the control flow graph, scoped to the cases that neither
 * the platform nor the lite check cover: statements after Break, Continue and Goto.
 * Statements after Return and Raise stay to the native platform check and
 * up-1068-unreachable-code. Port basis: pinned BSL LS UnreachableCodeDiagnostic.
 */
public class UnreachableCodeCheck extends BasicCheck
{
    public static final String CHECK_ID = "unreachable-code"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.UnreachableCodeCheck_title)
            .description(Messages.UnreachableCodeCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        Module module = (Module)object;
        for (Method method : module.allMethods())
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (hasSyntaxErrors(method))
            {
                continue;
            }
            reportGaps(CfgBuilder.build(method, false), acceptor, monitor);
        }
        if (monitor.isCanceled() || module.getModuleType() == ModuleType.COMMON_MODULE)
        {
            return;
        }
        // module body code is not allowed in common modules (the platform reports it itself)
        if (hasSyntaxErrors(module))
        {
            return;
        }
        reportGaps(CfgBuilder.build(module, false), acceptor, monitor);
    }

    private static void reportGaps(CfgGraph graph, ResultAcceptor acceptor, IProgressMonitor monitor)
    {
        for (CfgGraph.Vertex vertex : graph.vertexSet())
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (vertex == graph.getEntry() || !graph.incomingEdgesOf(vertex).isEmpty()
                || !(vertex instanceof CfgGraph.BasicBlock block) || block.statements().isEmpty())
            {
                continue;
            }
            // variant B: only the gaps; statements after Return and Raise are already
            // marked by the platform check and up-1068-unreachable-code
            CfgGraph.OrphanCause cause = block.orphanCause();
            if (cause != CfgGraph.OrphanCause.BREAK && cause != CfgGraph.OrphanCause.CONTINUE
                && cause != CfgGraph.OrphanCause.GOTO)
            {
                continue;
            }
            acceptor.addIssue(Messages.UnreachableCodeCheck_message, block.statements().get(0));
        }
    }

    private static boolean hasSyntaxErrors(EObject element)
    {
        var node = NodeModelUtils.findActualNodeFor(element);
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
}
