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
 *     malikov-pro - port of the APK check АПК_00073
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.CONFIGURATION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.CONFIGURATION__FUNCTIONAL_OPTIONS_PARAMETERS;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;

import com._1c.g5.v8.dt.core.platform.IConfigurationProject;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.md.CorePlugin;
import com.google.inject.Inject;

/**
 * Ограничения на использование параметров функциональных опций.
 * <p>
 * Перенос проверки АПК_00073 (статья 470 стандарта 1С).
 *
 * @author malikov-pro
 */
public class FunctionalOptionParametersLimitCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00073-functional-option-parameters-limit"; //$NON-NLS-1$

    private static final int MAX_PARAMETERS = 10;

    private final IV8ProjectManager projectManager;

    /** @param projectManager project kind resolver */
    @Inject
    public FunctionalOptionParametersLimitCheck(IV8ProjectManager projectManager)
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
        builder.title(Messages.FunctionalOptionParametersLimitCheck_title)
            .description(Messages.FunctionalOptionParametersLimitCheck_description)
            .issueType(IssueType.PERFORMANCE)
            .severity(IssueSeverity.MINOR)
            .complexity(CheckComplexity.NORMAL)
            .extension(new CommonSenseCheckExtension(CHECK_ID, CorePlugin.PLUGIN_ID))
            .topObject(CONFIGURATION)
            .checkTop()
            .features(CONFIGURATION__FUNCTIONAL_OPTIONS_PARAMETERS);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Configuration configuration = (Configuration)object;
        if (progressMonitor.isCanceled() || !(projectManager.getProject(configuration) instanceof IConfigurationProject))
        {
            return;
        }
        int count = configuration.getFunctionalOptionsParameters().size();
        if (count > MAX_PARAMETERS)
        {
            resultAceptor.addIssue(MessageFormat.format(
                Messages.FunctionalOptionParametersLimitCheck_too_many, Integer.valueOf(count),
                Integer.valueOf(MAX_PARAMETERS)), CONFIGURATION__FUNCTIONAL_OPTIONS_PARAMETERS);
        }
    }
}
