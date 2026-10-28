/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION__PARAMS;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FEATURE_ACCESS__NAME;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.OptInCheckExtension;

/** Opt-in audit of calls named SetPrivilegedMode, following the pinned LS syntax contract. */
public class SetPrivilegedModeCheck extends BasicCheck
{
    public static final String CHECK_ID = "set-privileged-mode"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.SetPrivilegedModeCheck_title)
            .description(Messages.SetPrivilegedModeCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.SECURITY)
            .extension(new OptInCheckExtension())
            .module().checkedObjectType(INVOCATION);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        if (monitor.isCanceled())
        {
            return;
        }
        Invocation call = (Invocation)object;
        var access = call.getMethodAccess();
        if (access == null || !("SetPrivilegedMode".equalsIgnoreCase(access.getName()) //$NON-NLS-1$
            || "УстановитьПривилегированныйРежим".equalsIgnoreCase(access.getName()))) //$NON-NLS-1$
        {
            return;
        }
        if (access instanceof StaticFeatureAccess && hasPinnedFalseFirstMember(call))
        {
            return;
        }
        acceptor.addIssue(Messages.SetPrivilegedModeCheck_message, access, FEATURE_ACCESS__NAME);
    }

    private static boolean hasPinnedFalseFirstMember(Invocation call)
    {
        if (call.getParams().size() != 1)
        {
            return false;
        }
        // LS tests expression.member[0].constValue.FALSE, not the expression's value.
        // Inspect the parameter CST: EMF folds parentheses into the same expression,
        // but LS treats (False) as a nested member, so it must NOT be skipped.
        var nodes = NodeModelUtils.findNodesForFeature(call, INVOCATION__PARAMS);
        if (nodes.size() != 1)
        {
            return false;
        }
        for (var leaf : nodes.get(0).getLeafNodes())
        {
            if (leaf.isHidden())
            {
                continue;
            }
            String token = leaf.getText();
            if ("Not".equalsIgnoreCase(token) || "Не".equalsIgnoreCase(token) //$NON-NLS-1$ //$NON-NLS-2$
                || "+".equals(token) || "-".equals(token)) //$NON-NLS-1$ //$NON-NLS-2$
            {
                continue;
            }
            return "False".equalsIgnoreCase(token) || "Ложь".equalsIgnoreCase(token); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return false;
    }
}
