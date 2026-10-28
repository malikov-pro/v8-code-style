/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FEATURE_ACCESS__NAME;
import static org.eclipse.xtext.resource.impl.ResourceDescriptionsProvider.PERSISTED_DESCRIPTIONS;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.xtext.resource.IResourceDescriptionsProvider;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.resource.TypesComputer;
import com._1c.g5.v8.dt.mcore.Environmental;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.mcore.util.McoreUtil;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.CatalogCodesSeries;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfAccounts;
import com._1c.g5.v8.dt.metadata.mdclass.CharOfAccountCodeSeries;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfCharacteristicTypes;
import com._1c.g5.v8.dt.metadata.mdclass.CharacteristicKindCodesSeries;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.ICheckDefinition;
import com.e1c.g5.v8.dt.check.context.OnModelFeatureChangeContextCollector;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;
import com.google.inject.Inject;

/** Checks code searches against metadata uniqueness and series settings. */
public class UnsafeFindByCodeCheck extends BasicCheck
{
    public static final String CHECK_ID = "unsafe-find-by-code"; //$NON-NLS-1$
    private final TypesComputer computer;
    private final IResourceDescriptionsProvider descriptionsProvider;

    @Inject
    public UnsafeFindByCodeCheck(TypesComputer computer, IResourceDescriptionsProvider descriptionsProvider)
    {
        this.computer = computer;
        this.descriptionsProvider = descriptionsProvider;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.UnsafeFindByCodeCheck_title)
            .description(Messages.UnsafeFindByCodeCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(INVOCATION);
    }

    @Override
    protected void postConfigureContextCollector(ICheckDefinition definition)
    {
        OnModelFeatureChangeContextCollector collector = (object, feature, event, session) -> {
            String importedType;
            if (object instanceof Catalog catalog && (feature == MdClassPackage.Literals.CATALOG__CHECK_UNIQUE
                || feature == MdClassPackage.Literals.CATALOG__CODE_SERIES))
            {
                importedType = "CatalogManager." + catalog.getName(); //$NON-NLS-1$
            }
            else if (object instanceof ChartOfAccounts chart
                && (feature == MdClassPackage.Literals.CHART_OF_ACCOUNTS__CHECK_UNIQUE
                    || feature == MdClassPackage.Literals.CHART_OF_ACCOUNTS__CODE_SERIES))
            {
                importedType = "ChartOfAccountsManager." + chart.getName(); //$NON-NLS-1$
            }
            else if (object instanceof ChartOfCharacteristicTypes chart
                && (feature == MdClassPackage.Literals.CHART_OF_CHARACTERISTIC_TYPES__CHECK_UNIQUE
                    || feature == MdClassPackage.Literals.CHART_OF_CHARACTERISTIC_TYPES__CODE_SERIES))
            {
                importedType = "ChartOfCharacteristicTypesManager." + chart.getName(); //$NON-NLS-1$
            }
            else
            {
                return;
            }
            var changed = EcoreUtil.getURI(object);
            var resources = new ResourceSetImpl();
            resources.getLoadOptions().put(PERSISTED_DESCRIPTIONS, Boolean.TRUE);
            var index = descriptionsProvider.getResourceDescriptions(resources);
            for (var description : index.getAllResourceDescriptions())
            {
                var uri = description.getURI();
                if (!uri.isPlatformResource() || !uri.segment(1).equals(changed.authority()))
                {
                    continue;
                }
                for (var imported : description.getImportedNames())
                {
                    if (importedType.equalsIgnoreCase(imported.toString()))
                    {
                        session.addLanguageCheck(uri.toPlatformString(true));
                        break;
                    }
                }
            }
        };
        definition.addGenericModelFeatureChangeContextCollector(collector, MdClassPackage.Literals.CATALOG);
        definition.addGenericModelFeatureChangeContextCollector(collector, MdClassPackage.Literals.CHART_OF_ACCOUNTS);
        definition.addGenericModelFeatureChangeContextCollector(collector,
            MdClassPackage.Literals.CHART_OF_CHARACTERISTIC_TYPES);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Invocation call = (Invocation)object;
        if (monitor.isCanceled() || !(call.getMethodAccess() instanceof DynamicFeatureAccess access)
            || !("НайтиПоКоду".equalsIgnoreCase(access.getName()) || "FindByCode".equalsIgnoreCase(access.getName()))) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return;
        }
        // The pinned LS MdoRefBuilder recognizes a direct metadata-manager root, not manager aliases.
        if (!(access.getSource() instanceof DynamicFeatureAccess manager)
            || !(manager.getSource() instanceof StaticFeatureAccess root))
        {
            return;
        }
        var environment = EcoreUtil2.getContainerOfType(call, Environmental.class);
        if (environment == null)
        {
            return;
        }
        var types = computer.computeTypes(access.getSource(), environment.environments());
        if (types.isEmpty())
        {
            return;
        }
        boolean unsafe = false;
        for (var type : types)
        {
            var resolved = (TypeItem)EcoreUtil.resolve(type, call);
            if (resolved.eIsProxy())
            {
                return;
            }
            var owner = EcoreUtil2.getContainerOfType(resolved, MdObject.class);
            String category = McoreUtil.getTypeCategory(resolved);
            if (owner instanceof Catalog catalog && "CatalogManager".equals(category) //$NON-NLS-1$
                && ("Catalogs".equalsIgnoreCase(root.getName()) || "Справочники".equalsIgnoreCase(root.getName()))) //$NON-NLS-1$ //$NON-NLS-2$
            {
                unsafe |= !catalog.isCheckUnique() || catalog.getCodeSeries() != CatalogCodesSeries.WHOLE_CATALOG;
            }
            else if (owner instanceof ChartOfAccounts chart && "ChartOfAccountsManager".equals(category) //$NON-NLS-1$
                && ("ChartsOfAccounts".equalsIgnoreCase(root.getName()) //$NON-NLS-1$
                    || "ПланыСчетов".equalsIgnoreCase(root.getName()))) //$NON-NLS-1$
            {
                unsafe |= !chart.isCheckUnique()
                    || chart.getCodeSeries() != CharOfAccountCodeSeries.WHOLE_CHART_OF_ACCOUNTS;
            }
            else if (owner instanceof ChartOfCharacteristicTypes chart
                && "ChartOfCharacteristicTypesManager".equals(category) //$NON-NLS-1$
                && ("ChartsOfCharacteristicTypes".equalsIgnoreCase(root.getName()) //$NON-NLS-1$
                    || "ПланыВидовХарактеристик".equalsIgnoreCase(root.getName()))) //$NON-NLS-1$
            {
                unsafe |= !chart.isCheckUnique()
                    || chart.getCodeSeries() != CharacteristicKindCodesSeries.WHOLE_CHARACTERISTIC_KIND;
            }
            else
            {
                return;
            }
        }
        if (unsafe)
        {
            acceptor.addIssue(Messages.UnsafeFindByCodeCheck_message, access, FEATURE_ACCESS__NAME);
        }
    }
}
