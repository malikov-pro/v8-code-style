/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.TryExceptStatement;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Reports global numeric conversions inside Try bodies. */
public class TryNumberCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "try-number"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.TryNumberCheck_title)
            .description(Messages.TryNumberCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.CODE_STYLE)
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
            if (!(call.getMethodAccess() instanceof StaticFeatureAccess access)
                || !("Число".equalsIgnoreCase(access.getName()) || "Number".equalsIgnoreCase(access.getName())) //$NON-NLS-1$ //$NON-NLS-2$
                || !insideTryBody(call))
            {
                continue;
            }
            var node = NodeModelUtils.getNode(call);
            if (node != null)
            {
                acceptor.addIssue(new BslDirectLocationIssue(Messages.TryNumberCheck_message,
                    new DirectLocation(node.getOffset(), node.getLength(), node.getStartLine(), module)));
            }
        }
    }

    private static boolean insideTryBody(EObject object)
    {
        EObject child = object;
        for (EObject parent = child.eContainer(); parent != null; parent = child.eContainer())
        {
            if (parent instanceof TryExceptStatement statement && statement.getTryStatements().contains(child))
            {
                return true;
            }
            child = parent;
        }
        return false;
    }
}
