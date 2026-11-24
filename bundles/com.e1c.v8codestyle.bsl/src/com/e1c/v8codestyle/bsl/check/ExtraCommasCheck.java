/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Module;
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
 * Reports a trailing comma in a call parameter list. Empty parameters in the
 * middle of the list are a legal way to skip optional arguments and are not
 * reported; only the last significant token before the closing parenthesis is
 * checked.
 */
public final class ExtraCommasCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "extra-commas"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.ExtraCommasCheck_title)
            .description(Messages.ExtraCommasCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module().checkedObjectType(INVOCATION);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Invocation call = (Invocation)object;
        INode node = NodeModelUtils.getNode(call);
        if (monitor.isCanceled() || node == null)
        {
            return;
        }
        List<ILeafNode> visible = new ArrayList<>();
        for (ILeafNode leaf : node.getLeafNodes())
        {
            if (leaf.getSyntaxErrorMessage() != null)
            {
                // A locally broken call is not a reliable trailing-comma witness.
                return;
            }
            if (!leaf.isHidden())
            {
                visible.add(leaf);
            }
        }
        if (visible.size() < 2)
        {
            return;
        }
        ILeafNode closing = visible.get(visible.size() - 1);
        if (!")".equals(closing.getText())) //$NON-NLS-1$
        {
            return;
        }
        ILeafNode previous = visible.get(visible.size() - 2);
        if (!",".equals(previous.getText())) //$NON-NLS-1$
        {
            return;
        }
        Module module = EcoreUtil2.getContainerOfType(call, Module.class);
        acceptor.addIssue(new BslDirectLocationIssue(Messages.ExtraCommasCheck_message,
            new DirectLocation(previous.getOffset(), previous.getLength(), previous.getStartLine(), module)));
    }
}
