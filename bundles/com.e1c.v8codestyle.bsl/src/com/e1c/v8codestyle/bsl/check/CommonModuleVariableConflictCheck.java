/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the APK check АПК_01218
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;
import static org.eclipse.xtext.resource.impl.ResourceDescriptionsProvider.PERSISTED_DESCRIPTIONS;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.IResourceDescriptionsProvider;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.core.platform.IConfigurationProject;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckDefinition;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.context.CheckContextCollectingSession;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;
import com.google.inject.Inject;

/**
 * Проверка совпадения имен переменных и наименований общих модулей.
 * <p>
 * Перенос проверки АПК_01218 (статья 467 стандарта 1С).
 *
 * @author malikov-pro
 */
public class CommonModuleVariableConflictCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-01218-common-module-variable-conflict"; //$NON-NLS-1$

    @Inject
    private IV8ProjectManager projectManager;

    @Inject
    private IResourceDescriptionsProvider descriptionsProvider;

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.CommonModuleVariableConflictCheck_title)
            .description(Messages.CommonModuleVariableConflictCheck_description)
            .issueType(IssueType.ERROR)
            .severity(IssueSeverity.CRITICAL)
            .complexity(CheckComplexity.NORMAL)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE);
    }

    @Override
    protected void postConfigureContextCollector(ICheckDefinition definition)
    {
        definition.addGenericModelFeatureChangeContextCollector((object, feature, event, session) -> {
            if (feature == MdClassPackage.Literals.MD_OBJECT__NAME
                || feature == MdClassPackage.Literals.COMMON_MODULE__SERVER
                || feature == MdClassPackage.Literals.COMMON_MODULE__CLIENT_MANAGED_APPLICATION
                || feature == MdClassPackage.Literals.COMMON_MODULE__SERVER_CALL)
            {
                scheduleProjectModules(object, session);
            }
        }, MdClassPackage.Literals.COMMON_MODULE);
        definition.addGenericModelAssociationContextCollector((object, event, session) ->
            scheduleProjectModules(object, session), MdClassPackage.Literals.COMMON_MODULE);
        definition.addGenericModelFeatureChangeContextCollector((object, feature, event, session) -> {
            if (feature == MdClassPackage.Literals.CONFIGURATION__COMMON_MODULES)
            {
                scheduleProjectModules(object, session);
            }
        }, MdClassPackage.Literals.CONFIGURATION);
    }

    private void scheduleProjectModules(IBmObject object, CheckContextCollectingSession session)
    {
        var project = projectManager.getProject(object);
        if (!(project instanceof IConfigurationProject))
        {
            return;
        }
        // A local/formal variable need not have a reference to the metadata module.
        // Scan persisted BSL resources of this project, not only existing references.
        var resources = new ResourceSetImpl();
        resources.getLoadOptions().put(PERSISTED_DESCRIPTIONS, Boolean.TRUE);
        var index = descriptionsProvider.getResourceDescriptions(resources);
        for (var description : index.getAllResourceDescriptions())
        {
            var uri = description.getURI();
            if (uri.isPlatformResource() && project.getProject().getName().equals(uri.segment(1))
                && "bsl".equalsIgnoreCase(uri.fileExtension())) //$NON-NLS-1$
            {
                session.addLanguageCheck(uri.toPlatformString(true));
            }
        }
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Module module = (Module)object;
        if (!(projectManager.getProject(module) instanceof IConfigurationProject project))
        {
            return;
        }
        Map<String, CommonModule> providers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (CommonModule provider : project.getConfiguration().getCommonModules())
        {
            if (!provider.eIsProxy() && provider.getName() != null)
            {
                providers.put(provider.getName(), provider);
            }
        }
        for (SimpleStatement statement : EcoreUtil2.getAllContentsOfType(module, SimpleStatement.class))
        {
            if (progressMonitor.isCanceled())
            {
                return;
            }
            if (statement.getRight() == null || !(statement.getLeft() instanceof StaticFeatureAccess left))
            {
                continue;
            }
            CommonModule provider = left.getName() == null ? null : providers.get(left.getName());
            if (provider != null && available(module, statement, provider))
            {
                resultAceptor.addIssue(MessageFormat.format(Messages.CommonModuleVariableConflictCheck_issue,
                    left.getName()), left);
            }
        }
    }

    private boolean available(Module module, SimpleStatement statement, CommonModule provider)
    {
        if (module.getModuleType() == ModuleType.COMMON_MODULE && module.getOwner() instanceof CommonModule owner)
        {
            return owner.isServer() ? owner.isServerCall() || provider.isServer()
                : provider.isClientManagedApplication();
        }
        if (isServerKind(module.getModuleType()))
        {
            return provider.isServer();
        }
        Method method = EcoreUtil2.getContainerOfType(statement, Method.class);
        if (method != null && method.getFormalParams().stream().anyMatch(p -> p.getDefaultValue() != null
            && leftName(statement).equalsIgnoreCase(p.getName())))
        {
            return false;
        }
        // The APK table has an empty directive for a method without a compiler pragma.
        if (method == null)
        {
            return provider.isServer();
        }
        for (var pragma : method.getPragmas())
        {
            String symbol = pragma.getSymbol() == null ? "" //$NON-NLS-1$
                : pragma.getSymbol().toUpperCase(Locale.ROOT).replace("&", ""); //$NON-NLS-1$ //$NON-NLS-2$
            if (symbol.startsWith("ATCLIENT") || symbol.startsWith("НАКЛИЕНТЕ")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                return provider.isClientManagedApplication() || provider.isServerCall();
            }
            if (symbol.startsWith("ATSERVER") || symbol.startsWith("НАСЕРВЕРЕ")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                return provider.isServer();
            }
        }
        return false;
    }

    private static String leftName(SimpleStatement statement)
    {
        return ((StaticFeatureAccess)statement.getLeft()).getName();
    }

    private static boolean isServerKind(ModuleType type)
    {
        return type == ModuleType.OBJECT_MODULE || type == ModuleType.MANAGER_MODULE
            || type == ModuleType.RECORDSET_MODULE || type == ModuleType.SESSION_MODULE
            || type == ModuleType.ORDINARY_APP_MODULE || type == ModuleType.VALUE_MANAGER_MODULE;
    }
}
