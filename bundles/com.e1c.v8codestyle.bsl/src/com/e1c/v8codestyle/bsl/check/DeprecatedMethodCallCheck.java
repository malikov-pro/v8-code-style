/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FEATURE_ACCESS__NAME;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.text.MessageFormat;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.resource.DynamicFeatureAccessComputer;
import com._1c.g5.v8.dt.core.platform.IConfigurationProvider;
import com._1c.g5.v8.dt.mcore.Environmental;
import com._1c.g5.v8.dt.mcore.Property;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;
import com.google.inject.Inject;

/** Residual deprecated property calls; native EDT owns platform methods and constructors. */
public class DeprecatedMethodCallCheck extends BasicCheck
{
    public static final String CHECK_ID = "deprecated-method-call"; //$NON-NLS-1$
    private static final Pattern VERSION = Pattern.compile("^(\\d+)[._](\\d+)(?:[._](\\d+))?"); //$NON-NLS-1$
    private final DynamicFeatureAccessComputer computer;
    private final IConfigurationProvider configurations;

    @Inject
    public DeprecatedMethodCallCheck(DynamicFeatureAccessComputer computer, IConfigurationProvider configurations)
    {
        this.computer = computer;
        this.configurations = configurations;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.DeprecatedMethodCallCheck_title)
            .description(Messages.DeprecatedMethodCallCheck_description)
            .complexity(CheckComplexity.NORMAL).severity(IssueSeverity.MINOR).issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module().checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Module module = (Module)object;
        var configuration = configurations.getConfiguration(module);
        if (configuration == null)
        {
            return;
        }
        String target = configuration.getCompatibilityMode() == null ? null
            : configuration.getCompatibilityMode().getLiteral();
        for (var access : EcoreUtil2.getAllContentsOfType(module, DynamicFeatureAccess.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            var environment = EcoreUtil2.getContainerOfType(access, Environmental.class);
            if (environment == null)
            {
                continue;
            }
            var entries = computer.resolveObject(access, environment.environments());
            for (var entry : entries)
            {
                if (entry.getFeature() == null)
                {
                    continue;
                }
                var feature = EcoreUtil.resolve(entry.getFeature(), access);
                if (feature instanceof Property property && !property.eIsProxy()
                    && isPlatformProperty(property) && deprecated(property.getDeprecatedSince(), target))
                {
                    acceptor.addIssue(MessageFormat.format(Messages.DeprecatedMethodCallCheck_message,
                        access.getName(), String.join(", ", property.getReplacedBy())), access, FEATURE_ACCESS__NAME); //$NON-NLS-1$
                    break;
                }
            }
        }
    }

    private boolean isPlatformProperty(Property property)
    {
        var uri = EcoreUtil.getURI(property);
        return uri.isPlatformPlugin() && uri.segmentCount() > 1
            && uri.segment(1).startsWith("com._1c.g5.v8.dt.platform_v"); //$NON-NLS-1$
    }

    private boolean deprecated(String since, String target)
    {
        int[] deprecated = version(since, 99);
        if (deprecated == null)
        {
            return false;
        }
        int[] configured = version(target, 0);
        if (configured == null)
        {
            // EDT DontUse is the same latest-version sentinel as the pinned LS contract.
            return target == null;
        }
        for (int i = 0; i < configured.length; i++)
        {
            if (configured[i] != deprecated[i])
            {
                return configured[i] > deprecated[i];
            }
        }
        return true;
    }

    private int[] version(String text, int missingPatch)
    {
        if (text == null)
        {
            return null;
        }
        var matcher = VERSION.matcher(text);
        if (!matcher.find())
        {
            return null;
        }
        try
        {
            return new int[] { Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                matcher.group(3) == null ? missingPatch : Integer.parseInt(matcher.group(3)) };
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }
}
