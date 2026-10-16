/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.DYNAMIC_FEATURE_ACCESS;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FEATURE_ACCESS__NAME;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.IndexAccess;
import com._1c.g5.v8.dt.bsl.resource.DynamicFeatureAccessComputer;
import com._1c.g5.v8.dt.form.model.DynamicListExtInfo;
import com._1c.g5.v8.dt.form.model.DynamicListTableExtInfo;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormAttribute;
import com._1c.g5.v8.dt.form.model.FormPackage;
import com._1c.g5.v8.dt.form.model.Table;
import com._1c.g5.v8.dt.form.service.datasourceinfo.IDataSourceInfoAssociationService;
import com._1c.g5.v8.dt.mcore.DerivedProperty;
import com._1c.g5.v8.dt.mcore.Environmental;
import com._1c.g5.v8.dt.mcore.McorePackage;
import com._1c.g5.v8.dt.mcore.util.McoreUtil;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckDefinition;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.context.OnModelFeatureChangeContextCollector;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;
import com.google.inject.Inject;

/** Detects dereferencing a dynamic-list table's CurrentRow. */
public class CurrentRowAccessCheck extends BasicCheck
{
    public static final String CHECK_ID = "current-row-access"; //$NON-NLS-1$
    private final DynamicFeatureAccessComputer binding;
    private final IDataSourceInfoAssociationService dataSources;

    @Inject
    public CurrentRowAccessCheck(DynamicFeatureAccessComputer binding, IDataSourceInfoAssociationService dataSources)
    {
        this.binding = binding;
        this.dataSources = dataSources;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.CurrentRowAccessCheck_title)
            .description(Messages.CurrentRowAccessCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.PERFORMANCE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(DYNAMIC_FEATURE_ACCESS);
    }

    @Override
    protected void postConfigureContextCollector(ICheckDefinition definition)
    {
        OnModelFeatureChangeContextCollector collector = (object, feature, event, session) -> {
            if (feature == FormPackage.Literals.TABLE__EXT_INFO
                || feature == FormPackage.Literals.DATA_ITEM__DATA_PATH
                || feature == FormPackage.Literals.FORM_ATTRIBUTE__EXT_INFO
                || feature == FormPackage.Literals.ABSTRACT_DATA_PATH__SEGMENTS
                || feature == FormPackage.Literals.ABSTRACT_DATA_PATH__OBJECTS
                || feature == McorePackage.Literals.TYPE_DESCRIPTION__TYPES
                || feature == FormPackage.Literals.ABSTRACT_FORM_ATTRIBUTE__VALUE_TYPE)
            {
                var form = EcoreUtil2.getContainerOfType(object, Form.class);
                if (form != null && form.getModule() != null)
                {
                    var uri = EcoreUtil.getURI(form.getModule()).trimFragment();
                    if (uri.isPlatformResource())
                    {
                        session.addLanguageCheck(uri.toPlatformString(true));
                    }
                }
            }
        };
        definition.addGenericModelFeatureChangeContextCollector(collector, FormPackage.Literals.TABLE,
            FormPackage.Literals.FORM);
        definition.addGenericModelFeatureChangeContextCollector(collector, FormPackage.Literals.FORM_ATTRIBUTE,
            FormPackage.Literals.FORM);
        definition.addGenericModelFeatureChangeContextCollector(collector, FormPackage.Literals.DATA_PATH,
            FormPackage.Literals.FORM);
        definition.addGenericModelFeatureChangeContextCollector(collector, McorePackage.Literals.TYPE_DESCRIPTION,
            FormPackage.Literals.FORM);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        var access = (DynamicFeatureAccess)object;
        if (monitor.isCanceled() || !("CurrentRow".equalsIgnoreCase(access.getName()) //$NON-NLS-1$
            || "ТекущаяСтрока".equalsIgnoreCase(access.getName()))) //$NON-NLS-1$
        {
            return;
        }
        var environment = EcoreUtil2.getContainerOfType(access, Environmental.class);
        boolean followed = access.eContainer() instanceof DynamicFeatureAccess next && next.getSource() == access
            || access.eContainer() instanceof IndexAccess index && index.getSource() == access;
        if (environment == null || !followed || !(access.getSource() instanceof FeatureAccess tableAccess))
        {
            return;
        }
        var entries = binding.resolveObject(tableAccess, environment.environments());
        if (entries.isEmpty())
        {
            return;
        }
        for (var entry : entries)
        {
            var feature = entry.getFeature();
            if (!(feature instanceof DerivedProperty property) || !(property.getSource() instanceof Table table)
                || !(table.getExtInfo() instanceof DynamicListTableExtInfo))
            {
                return;
            }
            var form = EcoreUtil2.getContainerOfType(table, Form.class);
            if (form == null || table.getDataPath() == null)
            {
                return;
            }
            // Resolve current segments; AbstractDataPath.objects may still describe the previous binding.
            var info = dataSources.findPropertyInfo(form, table.getDataPath(), 0);
            if (info == null || !(info.getSource() instanceof FormAttribute attribute)
                || !(attribute.getExtInfo() instanceof DynamicListExtInfo) || attribute.getValueType() == null
                || !McoreUtil.isOfSingleType(attribute.getValueType(), "DynamicList")) //$NON-NLS-1$
            {
                return;
            }
        }
        acceptor.addIssue(Messages.CurrentRowAccessCheck_message, access, FEATURE_ACCESS__NAME);
    }
}
