/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FEATURE_ACCESS__NAME;

import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.resource.DynamicFeatureAccessComputer;
import com._1c.g5.v8.dt.core.platform.IConfigurationProvider;
import com._1c.g5.v8.dt.mcore.Environmental;
import com._1c.g5.v8.dt.mcore.Property;
import com._1c.g5.v8.dt.mcore.Type;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;
import com.google.inject.Inject;

/** Scoped residual for bound ErrorCategory values; not a general platform API version service. */
public class UnavailableMemberCallCheck extends BasicCheck
{
    public static final String CHECK_ID = "unavailable-member-call"; //$NON-NLS-1$
    // Pinned builtin-platform-types.json, 33a1400681e342ad4bd12281dcee8c7db781ee3d.
    // Keys are resolved platform property names, never source spellings.
    private static final Map<String, Integer> INTRODUCED = Map.ofEntries(
        Map.entry("AllErrors", 17), Map.entry("ExceptionRaisedFromScript", 17), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("AccessViolation", 17), Map.entry("UnsupportedFormat", 26), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("InvalidPassword", 21), Map.entry("NoPermissionToUseFunctionality", 19), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("ExternalDataSourceError", 17), Map.entry("ScriptRuntimeError", 17), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("LocalFileAccessError", 17), Map.entry("ScriptUseError", 17), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("ScriptCompileError", 17), Map.entry("ConfigurationError", 21), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("DatabaseCopyError", 18), Map.entry("DataCompositionSettingsError", 17), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("GotoURLError", 17), Map.entry("FullTextSearchError", 17), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("DocumentConversionError", 20), Map.entry("SignatureVerificationError", 21), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("PrinterError", 17), Map.entry("SpeechProcessingError", 23), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("SessionError", 17), Map.entry("NetworkError", 17), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("CollaborationSystemError", 17), Map.entry("MultimediaToolsError", 19), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("DatabaseTablespaceError", 23), Map.entry("StoredDataError", 17), //$NON-NLS-1$ //$NON-NLS-2$
        Map.entry("ForcedShutdown", 25), Map.entry("OtherError", 17)); //$NON-NLS-1$ //$NON-NLS-2$
    private final IConfigurationProvider configurations;
    private final DynamicFeatureAccessComputer computer;

    @Inject
    public UnavailableMemberCallCheck(IConfigurationProvider configurations, DynamicFeatureAccessComputer computer)
    {
        this.configurations = configurations;
        this.computer = computer;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.UnavailableMemberCallCheck_title)
            .description(Messages.UnavailableMemberCallCheck_description)
            .complexity(CheckComplexity.NORMAL).severity(IssueSeverity.MAJOR).issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module().checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Module module = (Module)object;
        var configuration = configurations.getConfiguration(module);
        if (configuration == null || configuration.getCompatibilityMode() == null)
        {
            // No configured target means latest, as in pinned LS.
            return;
        }
        String target = configuration.getCompatibilityMode().getLiteral();
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
            for (var entry : computer.resolveObject(access, environment.environments()))
            {
                if (entry.getFeature() == null)
                {
                    continue;
                }
                var feature = EcoreUtil.resolve(entry.getFeature(), access);
                if (!(feature instanceof Property property) || property.eIsProxy())
                {
                    continue;
                }
                var owner = EcoreUtil2.getContainerOfType(property, Type.class);
                var uri = EcoreUtil.getURI(property);
                if (owner == null || !owner.isSysEnum() || !"EnumErrorCategory".equals(owner.getName()) //$NON-NLS-1$
                    || !uri.isPlatformPlugin() || uri.segmentCount() < 2
                    || !uri.segment(1).startsWith("com._1c.g5.v8.dt.platform_v")) //$NON-NLS-1$
                {
                    continue;
                }
                Integer since = INTRODUCED.get(property.getName());
                if (since != null && before(target, since))
                {
                    acceptor.addIssue(MessageFormat.format(Messages.UnavailableMemberCallCheck_message,
                        access.getName(), "8.3." + since), access, FEATURE_ACCESS__NAME); //$NON-NLS-1$
                    break;
                }
            }
        }
    }

    private boolean before(String target, int patch)
    {
        String[] parts = target.split("\\."); //$NON-NLS-1$
        // Target is an EDT CompatibilityMode literal, not arbitrary user text.
        if (parts.length != 3)
        {
            return false;
        }
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        return major < 8 || major == 8 && (minor < 3 || minor == 3 && Integer.parseInt(parts[2]) < patch);
    }
}
