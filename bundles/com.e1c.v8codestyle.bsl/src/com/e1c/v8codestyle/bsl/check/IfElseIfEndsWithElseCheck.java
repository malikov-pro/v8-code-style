/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.IfStatement;
import com._1c.g5.v8.dt.bsl.model.Module;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Checks that an If/ElsIf chain has an Else branch, including an empty one. */
public class IfElseIfEndsWithElseCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "if-else-if-ends-with-else"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.IfElseIfEndsWithElseCheck_title)
            .description(Messages.IfElseIfEndsWithElseCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Module module = (Module)object;
        for (IfStatement statement : EcoreUtil2.getAllContentsOfType(module, IfStatement.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (statement.getElsIfParts().isEmpty())
            {
                continue;
            }
            INode node = NodeModelUtils.getNode(statement);
            if (node == null)
            {
                continue;
            }
            boolean hasElse = false;
            ILeafNode endIf = null;
            for (ILeafNode leaf : node.getLeafNodes())
            {
                if (leaf.getSemanticElement() == statement && leaf.getGrammarElement() instanceof Keyword keyword)
                {
                    hasElse |= "Иначе".equalsIgnoreCase(keyword.getValue()) || "Else".equalsIgnoreCase(keyword.getValue()); //$NON-NLS-1$ //$NON-NLS-2$
                    if ("КонецЕсли".equalsIgnoreCase(keyword.getValue()) || "EndIf".equalsIgnoreCase(keyword.getValue())) //$NON-NLS-1$ //$NON-NLS-2$
                    {
                        endIf = leaf;
                    }
                }
            }
            if (!hasElse && endIf != null)
            {
                acceptor.addIssue(new BslDirectLocationIssue(Messages.IfElseIfEndsWithElseCheck_message,
                    new DirectLocation(endIf.getOffset(), endIf.getLength(), endIf.getStartLine(), module)));
            }
        }
    }
}
