/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.StringLiteral;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Reports strings assembled by writing string literals one after another
 * without a {@code +} operator (implicit concatenation). The pinned LS check
 * flags a string parse node with more than one child, which is exactly this
 * case; a single multiline literal continued with {@code |} is not reported.
 * The marker spans the whole group of adjacent literals.
 */
public final class CrazyMultilineStringCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "crazy-multiline-string"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.CrazyMultilineStringCheck_title)
            .description(Messages.CrazyMultilineStringCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module().checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Module module = (Module)object;
        for (StringLiteral literal : EcoreUtil2.getAllContentsOfType(module, StringLiteral.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            INode node = NodeModelUtils.getNode(literal);
            if (node == null || fragments(node.getText()) < 2)
            {
                continue;
            }
            acceptor.addIssue(new BslDirectLocationIssue(Messages.CrazyMultilineStringCheck_message,
                new DirectLocation(node.getOffset(), node.getLength(), node.getStartLine(), module)));
        }
    }

    /**
     * Counts independent string segments in the literal text. A single literal
     * with {@code |} continuations has one segment; adjacent literals written
     * without {@code +} have two or more. Doubled quotes inside a literal are
     * an escaped quote, not a new segment.
     */
    private static int fragments(String text)
    {
        int segments = 0;
        boolean inside = false;
        for (int index = 0; index < text.length(); index++)
        {
            if (text.charAt(index) != '"')
            {
                continue;
            }
            if (!inside)
            {
                segments++;
                inside = true;
            }
            else if (index + 1 < text.length() && text.charAt(index + 1) == '"')
            {
                index++;
            }
            else
            {
                inside = false;
            }
        }
        return segments;
    }
}
