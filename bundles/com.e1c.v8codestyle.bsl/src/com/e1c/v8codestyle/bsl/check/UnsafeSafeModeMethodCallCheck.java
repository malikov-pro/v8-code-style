/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.BinaryExpression;
import com._1c.g5.v8.dt.bsl.model.BinaryOperation;
import com._1c.g5.v8.dt.bsl.model.Conditional;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.UnaryExpression;
import com._1c.g5.v8.dt.core.platform.IConfigurationProvider;
import com._1c.g5.v8.dt.metadata.mdclass.CompatibilityMode;
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

/** Reports implicit Boolean use of SafeMode, which can return a profile name. */
public class UnsafeSafeModeMethodCallCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "unsafe-safe-mode-method-call"; //$NON-NLS-1$
    private final IConfigurationProvider configurationProvider;

    @Inject
    public UnsafeSafeModeMethodCallCheck(IConfigurationProvider configurationProvider)
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
        builder.title(Messages.UnsafeSafeModeMethodCallCheck_title)
            .description(Messages.UnsafeSafeModeMethodCallCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(INVOCATION);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Invocation call = (Invocation)object;
        if (monitor.isCanceled() || !(call.getMethodAccess() instanceof StaticFeatureAccess access)
            || !("БезопасныйРежим".equalsIgnoreCase(access.getName()) || "SafeMode".equalsIgnoreCase(access.getName()))) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return;
        }
        var configuration = configurationProvider.getConfiguration(call);
        if (configuration != null)
        {
            CompatibilityMode mode = configuration.getCompatibilityMode();
            if (mode == CompatibilityMode.VERSION8_1 || mode == CompatibilityMode.VERSION8_213
                || mode == CompatibilityMode.VERSION8_216)
            {
                return;
            }
        }
        EObject parent = call.eContainer();
        boolean unsafe = parent instanceof UnaryExpression
            || parent instanceof Conditional conditional && conditional.getPredicate() == call
            || parent instanceof BinaryExpression binary
                && (binary.getOperation() == BinaryOperation.AND || binary.getOperation() == BinaryOperation.OR);
        if (unsafe)
        {
            var node = NodeModelUtils.getNode(call);
            if (node != null)
            {
                acceptor.addIssue(new BslDirectLocationIssue(Messages.UnsafeSafeModeMethodCallCheck_message,
                    new DirectLocation(node.getOffset(), node.getLength(), node.getStartLine(), call)));
            }
        }
    }
}
