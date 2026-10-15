/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FEATURE_ACCESS__NAME;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Reports literal or empty first arguments of receiver-based lookup calls. */
public class UsingFindElementByStringCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "using-find-element-by-string"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.UsingFindElementByStringCheck_title)
            .description(Messages.UsingFindElementByStringCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(INVOCATION);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Invocation call = (Invocation)object;
        if (monitor.isCanceled() || !(call.getMethodAccess() instanceof DynamicFeatureAccess access))
        {
            return;
        }
        String name = access.getName();
        if (!("НайтиПоНаименованию".equalsIgnoreCase(name) || "FindByDescription".equalsIgnoreCase(name) //$NON-NLS-1$ //$NON-NLS-2$
            || "НайтиПоКоду".equalsIgnoreCase(name) || "FindByCode".equalsIgnoreCase(name) //$NON-NLS-1$ //$NON-NLS-2$
            || "НайтиПоНомеру".equalsIgnoreCase(name) || "FindByNumber".equalsIgnoreCase(name))) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return;
        }
        var node = NodeModelUtils.getNode(call);
        if (node == null)
        {
            return;
        }
        // Start at the called method's name, not the receiver (which may contain nested calls).
        var names = NodeModelUtils.findNodesForFeature(access, FEATURE_ACCESS__NAME);
        if (names.isEmpty())
        {
            return;
        }
        int methodOffset = names.get(0).getOffset();
        boolean afterOpen = false;
        boolean suspicious = false;
        int end = node.getEndOffset();
        for (ILeafNode leaf : node.getLeafNodes())
        {
            if (leaf.isHidden() || leaf.getOffset() < methodOffset)
            {
                continue;
            }
            String token = leaf.getText();
            if (!afterOpen)
            {
                afterOpen = "(".equals(token); //$NON-NLS-1$
                continue;
            }
            suspicious = ",".equals(token) || ")".equals(token) || token.startsWith("\"") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                || !token.isEmpty() && Character.isDigit(token.charAt(0));
            break;
        }
        if (suspicious)
        {
            acceptor.addIssue(new BslDirectLocationIssue(Messages.UsingFindElementByStringCheck_message,
                new DirectLocation(methodOffset, end - methodOffset, names.get(0).getStartLine(), call)));
        }
    }
}
