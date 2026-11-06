/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT__NAME;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;

import com._1c.g5.v8.dt.core.platform.IConfigurationProject;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.ICheckDefinition;
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.md.CorePlugin;
import com.google.inject.Inject;

/** Compares storage fields with their owner, not sibling names or section with top owner. */
public final class SameMetadataObjectAndChildNamesCheck extends BasicCheck
{
    public static final String CHECK_ID = "same-metadata-object-and-child-names"; //$NON-NLS-1$
    private final IV8ProjectManager projectManager;

    @Inject
    public SameMetadataObjectAndChildNamesCheck(IV8ProjectManager projectManager)
    {
        this.projectManager = projectManager;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.SameMetadataObjectAndChildNamesCheck_title)
            .description(Messages.SameMetadataObjectAndChildNamesCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .extension(new SkipAdoptedInExtensionMdObjectExtension());
        for (EClass type : MetadataNameScope.SAME_TYPES)
        {
            builder.topObject(type).checkTop().features(MD_OBJECT__NAME)
                .features(type.getEAllContainments().toArray(new EStructuralFeature[0]))
                .containment(MD_OBJECT).features(MD_OBJECT__NAME);
        }
    }

    @Override
    protected void postConfigureContextCollector(ICheckDefinition definition)
    {
        // Parent-name changes must recompute descendant name predicates too.
        definition.addGenericModelFeatureChangeContextCollector((object, feature, event, session) -> {
            if (feature == MD_OBJECT__NAME && object instanceof MdObject)
            {
                session.addFullCheck(object);
            }
        }, MD_OBJECT, MetadataNameScope.SAME_TYPES.toArray(new EClass[0]));
        for (EClass type : new EClass[] { MdClassPackage.Literals.ACCOUNTING_FLAG,
            MdClassPackage.Literals.EXT_DIMENSION_ACCOUNTING_FLAG })
        {
            definition.addGenericModelFeatureChangeContextCollector((object, feature, event, session) -> {
                if (feature == MD_OBJECT__NAME && object.eContainer() instanceof IBmObject owner)
                {
                    session.addFullCheck(owner);
                }
            }, type, MdClassPackage.Literals.CHART_OF_ACCOUNTS);
        }
    }

    @Override
    protected void check(Object object, ResultAcceptor result, ICheckParameters parameters, IProgressMonitor monitor)
    {
        MdObject field = (MdObject)object;
        if (monitor.isCanceled() || !(projectManager.getProject(field) instanceof IConfigurationProject))
        {
            return;
        }
        if (!(field.eContainer() instanceof MdObject owner))
        {
            return;
        }
        MdObject top = owner;
        while (top.eContainer() instanceof MdObject parent)
        {
            top = parent;
        }
        if (!MetadataNameScope.SAME_TYPES.contains(top.eClass())
            || owner != top && !MetadataNameScope.sections(top).contains(owner)
            || !MetadataNameScope.attributes(owner).contains(field))
        {
            return;
        }
        String name = owner.getName();
        if (name != null && name.equalsIgnoreCase(field.getName()))
        {
            result.addIssue(MessageFormat.format(Messages.SameMetadataObjectAndChildNamesCheck_message,
                field.getName(), name), MD_OBJECT__NAME);
        }
    }
}
