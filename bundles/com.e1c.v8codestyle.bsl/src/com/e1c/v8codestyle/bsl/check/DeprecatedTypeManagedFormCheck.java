/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.StringLiteral;
import com._1c.g5.v8.dt.core.platform.IConfigurationProvider;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;
import com.google.inject.Inject;

/**
 * Reports the deprecated managed-form type name in a {@code Тип("УправляемаяФорма")}
 * call. The pinned LS diagnostic is active for compatibility mode 8.3.14 and
 * older; newer configurations are skipped. The marker covers the whole call,
 * while LS anchors the parameter list.
 */
public final class DeprecatedTypeManagedFormCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "deprecated-type-managed-form"; //$NON-NLS-1$

    private final IConfigurationProvider configurationProvider;

    @Inject
    public DeprecatedTypeManagedFormCheck(IConfigurationProvider configurationProvider)
    {
        this.configurationProvider = configurationProvider;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.DeprecatedTypeManagedFormCheck_title)
            .description(Messages.DeprecatedTypeManagedFormCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.TRIVIAL)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module().checkedObjectType(INVOCATION);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Invocation call = (Invocation)object;
        if (monitor.isCanceled() || !(call.getMethodAccess() instanceof StaticFeatureAccess access)
            || !("Тип".equalsIgnoreCase(access.getName()) || "Type".equalsIgnoreCase(access.getName()))) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return;
        }
        if (tooNew(call))
        {
            return;
        }
        List<Expression> params = call.getParams();
        if (params.size() != 1 || !(params.get(0) instanceof StringLiteral literal))
        {
            return;
        }
        INode literalNode = NodeModelUtils.getNode(literal);
        if (literalNode == null)
        {
            return;
        }
        String text = literalNode.getText().trim();
        if (text.length() < 2 || !text.startsWith("\"") || !text.endsWith("\"")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return;
        }
        String name = text.substring(1, text.length() - 1);
        if (!("УправляемаяФорма".equalsIgnoreCase(name) || "ManagedForm".equalsIgnoreCase(name))) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return;
        }
        INode node = NodeModelUtils.getNode(call);
        if (node != null)
        {
            acceptor.addIssue(new BslDirectLocationIssue(Messages.DeprecatedTypeManagedFormCheck_message,
                new DirectLocation(node.getOffset(), node.getLength(), node.getStartLine(),
                    EcoreUtil2.getContainerOfType(call, Module.class))));
        }
    }

    private boolean tooNew(Invocation call)
    {
        var configuration = configurationProvider.getConfiguration(call);
        if (configuration == null)
        {
            return false;
        }
        var mode = configuration.getCompatibilityMode();
        if (mode == null)
        {
            return false;
        }
        String[] version = mode.getLiteral().split("\\."); //$NON-NLS-1$
        if (version.length != 3)
        {
            return false;
        }
        int major = Integer.parseInt(version[0]);
        int minor = Integer.parseInt(version[1]);
        int patch = Integer.parseInt(version[2]);
        return major > 8 || major == 8 && (minor > 3 || minor == 3 && patch > 14);
    }
}
