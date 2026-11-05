/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.ACCOUNTING_REGISTER;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.ACCUMULATION_REGISTER;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.CALCULATION_REGISTER;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.INFORMATION_REGISTER;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.REGISTER_DIMENSION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.REGISTER_DIMENSION__DENY_INCOMPLETE_VALUES;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.NAMED_ELEMENT__NAME;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EClass;

import com._1c.g5.v8.dt.metadata.mdclass.RegisterDimension;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckRepository;
import com.e1c.v8codestyle.check.OptInCheckExtension;
import com.e1c.v8codestyle.internal.md.CorePlugin;
import com.google.inject.Inject;

/** Opt-in audit of optional register dimensions, independent of the presence of BSL modules. */
public class DenyIncompleteValuesCheck extends BasicCheck
{
    public static final String CHECK_ID = "deny-incomplete-values"; //$NON-NLS-1$

    @Inject
    private ICheckRepository checkRepository;

    @Inject
    private IV8ProjectManager projectManager;

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.DenyIncompleteValuesCheck_title)
            .description(Messages.DenyIncompleteValuesCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.WARNING)
            .extension(new OptInCheckExtension());
        for (EClass register : new EClass[] { INFORMATION_REGISTER, ACCUMULATION_REGISTER,
            ACCOUNTING_REGISTER, CALCULATION_REGISTER })
        {
            builder.topObject(register).containment(REGISTER_DIMENSION)
                .features(REGISTER_DIMENSION__DENY_INCOMPLETE_VALUES, NAMED_ELEMENT__NAME);
        }
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        if (!monitor.isCanceled() && object instanceof RegisterDimension dimension
            && !dimension.isDenyIncompleteValues())
        {
            // A profile reset to the disabled default can still schedule an explicit metadata recheck.
            var project = projectManager.getProject(dimension);
            if (project == null || !checkRepository.getSettings(new CheckUid(CHECK_ID, CorePlugin.PLUGIN_ID),
                project.getProject()).isEnabled())
            {
                return;
            }
            acceptor.addIssue(MessageFormat.format(Messages.DenyIncompleteValuesCheck_message, dimension.getName()),
                REGISTER_DIMENSION__DENY_INCOMPLETE_VALUES);
        }
    }
}
