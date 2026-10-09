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
 *     malikov-pro - check for the mandatory roles of the configuration
 *******************************************************************************/
package com.e1c.v8codestyle.md.configuration.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.CONFIGURATION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.CONFIGURATION__ROLES;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import com._1c.g5.v8.dt.core.platform.IConfigurationProject;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.Role;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.md.CorePlugin;
import com.google.inject.Inject;

/**
 * The configuration must define the three mandatory roles of the standard
 * 488 (section 2): <code>ПолныеПрава</code> (FullAccess) — application
 * administration, <code>АдминистраторСистемы</code> (SystemAdministrator) —
 * system administration, and
 * <code>ИнтерактивноеОткрытиеВнешнихОтчетовИОбработок</code>
 * (InteractiveOpenExternalReportsAndDataProcessors) — interactive opening
 * of external reports and data processors. A role is found by its name; both
 * the Russian and the English standard names are accepted. Each missing role
 * is reported on the configuration.
 * <p>
 * Extension configurations are not checked: by the same standard (section 6)
 * an extension must not borrow the mandatory roles and supplies its own
 * roles instead.
 *
 * @author malikov-pro
 */
public class ConfigurationRequiredRolesCheck
    extends BasicCheck
{

    /** Идентификатор проверки (up-701 — задача апстрима #701). */
    public static final String CHECK_ID = "up-701-required-roles"; //$NON-NLS-1$

    private static final String FULL_ACCESS_RU = "ПолныеПрава"; //$NON-NLS-1$
    private static final String FULL_ACCESS_EN = "FullAccess"; //$NON-NLS-1$
    private static final String SYSTEM_ADMINISTRATOR_RU = "АдминистраторСистемы"; //$NON-NLS-1$
    private static final String SYSTEM_ADMINISTRATOR_EN = "SystemAdministrator"; //$NON-NLS-1$
    private static final String INTERACTIVE_OPEN_RU = "ИнтерактивноеОткрытиеВнешнихОтчетовИОбработок"; //$NON-NLS-1$
    private static final String INTERACTIVE_OPEN_EN = "InteractiveOpenExternalReportsAndDataProcessors"; //$NON-NLS-1$

    private final IV8ProjectManager v8ProjectManager;

    /**
     * Instantiates a new check.
     *
     * @param v8ProjectManager the V8 project manager service, cannot be {@code null}
     */
    @Inject
    public ConfigurationRequiredRolesCheck(IV8ProjectManager v8ProjectManager)
    {
        super();
        this.v8ProjectManager = v8ProjectManager;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.ConfigurationRequiredRolesCheck_title)
            .description(Messages.ConfigurationRequiredRolesCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .topObject(CONFIGURATION)
            .checkTop()
            .features(CONFIGURATION__ROLES);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (monitor.isCanceled() || !(object instanceof Configuration configuration))
        {
            return;
        }
        IV8Project project = v8ProjectManager.getProject(configuration);
        if (!(project instanceof IConfigurationProject))
        {
            // Extension configurations must not contain the mandatory roles (std 488, section 6).
            return;
        }

        Set<String> names = new HashSet<>();
        for (Role role : configuration.getRoles())
        {
            if (role.getName() != null && !role.getName().isBlank())
            {
                names.add(role.getName());
            }
        }

        if (!hasRole(names, FULL_ACCESS_RU, FULL_ACCESS_EN))
        {
            resultAceptor.addIssue(MessageFormat.format(Messages.ConfigurationRequiredRolesCheck_Role_is_missing,
                FULL_ACCESS_RU));
        }
        if (!hasRole(names, SYSTEM_ADMINISTRATOR_RU, SYSTEM_ADMINISTRATOR_EN))
        {
            resultAceptor.addIssue(MessageFormat.format(Messages.ConfigurationRequiredRolesCheck_Role_is_missing,
                SYSTEM_ADMINISTRATOR_RU));
        }
        if (!hasRole(names, INTERACTIVE_OPEN_RU, INTERACTIVE_OPEN_EN))
        {
            resultAceptor.addIssue(MessageFormat.format(Messages.ConfigurationRequiredRolesCheck_Role_is_missing,
                INTERACTIVE_OPEN_RU));
        }
    }

    private static boolean hasRole(Set<String> names, String russianName, String englishName)
    {
        return names.contains(russianName) || names.contains(englishName);
    }
}
