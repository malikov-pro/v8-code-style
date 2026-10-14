/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * This program and the accompanying materials are made available under the
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 * Contributors: malikov-pro - initial API and implementation
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.BooleanLiteral;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Checks calls that can disable safe mode; dynamic flags are suspicious too. */
public class DisableSafeModeCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "disable-safe-mode"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.DisableSafeModeCheck_title)
            .description(Messages.DisableSafeModeCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.SECURITY)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Module module = (Module)object;
        for (Invocation call : EcoreUtil2.getAllContentsOfType(module, Invocation.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (!(call.getMethodAccess() instanceof StaticFeatureAccess access))
            {
                continue;
            }
            String name = access.getName();
            boolean safeMode = "УстановитьБезопасныйРежим".equalsIgnoreCase(name) //$NON-NLS-1$
                || "SetSafeMode".equalsIgnoreCase(name); //$NON-NLS-1$
            boolean disabledMode = "УстановитьОтключениеБезопасногоРежима".equalsIgnoreCase(name) //$NON-NLS-1$
                || "SetSafeModeDisabled".equalsIgnoreCase(name); //$NON-NLS-1$
            if ((!safeMode && !disabledMode) || (call.getParams().size() == 1
                && call.getParams().get(0) instanceof BooleanLiteral flag && flag.isIsTrue() == safeMode))
            {
                continue;
            }
            INode node = NodeModelUtils.getNode(call);
            if (node != null)
            {
                acceptor.addIssue(new BslDirectLocationIssue(Messages.DisableSafeModeCheck_message,
                    new DirectLocation(node.getOffset(), node.getLength(), node.getStartLine(), module)));
            }
        }
    }
}
