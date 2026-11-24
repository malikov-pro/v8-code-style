/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.right.check;

import static com._1c.g5.v8.dt.rights.model.RightsPackage.Literals.ROLE_DESCRIPTION;
import static com._1c.g5.v8.dt.rights.model.RightsPackage.Literals.ROLE_DESCRIPTION__SET_FOR_NEW_OBJECTS;

import java.text.MessageFormat;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.metadata.mdclass.Role;
import com._1c.g5.v8.dt.rights.model.RoleDescription;
import com._1c.g5.v8.dt.rights.model.util.RightsModelUtil;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.WrongParameterException;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.StandardCheckExtension;
import com.e1c.v8codestyle.internal.right.CorePlugin;
import com.google.inject.Inject;

/**
 * Reports the role flag "Set permissions for new objects" ({@code setForNewObjects}) on any role except the roles with
 * full access rights.
 * <p>
 * Port of the BSL LS {@code SetPermissionsForNewObjects} (v8std 532). In the LS the flag is checked for every
 * configuration role while analyzing the managed application module; the port is a right-channel check that is
 * evaluated for each role description and places the marker on the role itself (the role flag feature).
 * <p>
 * Contract:
 * <ul>
 * <li>the role flag {@code setForNewObjects} must be off;</li>
 * <li>roles listed in {@code namesFullAccessRole} (default {@code FullAccess,ПолныеПрава}) are allowed to have the
 * flag;</li>
 * <li>the role name is taken from the {@link Role} metadata object of the role description.</li>
 * </ul>
 * <p>
 * <b>Deviation from the LS:</b> role names are compared case-insensitively, because 1C role names are
 * case-insensitive identifiers, while the LS compares the configured list case-sensitively.
 * <p>
 * <b>Partial scope (v1):</b> the check looks only at the role flag of the configuration roles; roles of configuration
 * extensions and roles on support are not treated specially (an extension role is checked by the same flag rule), and
 * nothing is analyzed for "set for attributes by default", which is a different flag.
 *
 * @author malikov-pro
 */
public class RightSetPermissionsForNewObjectsCheck
    extends BasicCheck<Object>
{
    public static final String CHECK_ID = "right-set-permissions-for-new-objects"; //$NON-NLS-1$

    private static final String PARAM_NAMES_FULL_ACCESS_ROLE = "namesFullAccessRole"; //$NON-NLS-1$

    private static final String DEFAULT_NAMES_FULL_ACCESS_ROLE = "FullAccess,ПолныеПрава"; //$NON-NLS-1$

    private final IBmModelManager bmModelManager;

    /**
     * Creates the check instance.
     *
     * @param bmModelManager the BM model manager, cannot be {@code null}.
     */
    @Inject
    public RightSetPermissionsForNewObjectsCheck(IBmModelManager bmModelManager)
    {
        this.bmModelManager = bmModelManager;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.RightSetPermissionsForNewObjectsCheck_title)
            .description(Messages.RightSetPermissionsForNewObjectsCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.SECURITY)
            .extension(new StandardCheckExtension(532, getCheckId(), CorePlugin.PLUGIN_ID))
            .topObject(ROLE_DESCRIPTION)
            .checkTop()
            .features(ROLE_DESCRIPTION__SET_FOR_NEW_OBJECTS)
            .parameter(PARAM_NAMES_FULL_ACCESS_ROLE, String.class, DEFAULT_NAMES_FULL_ACCESS_ROLE,
                Messages.RightSetPermissionsForNewObjectsCheck_NamesFullAccessRole);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (monitor.isCanceled() || !(object instanceof RoleDescription roleDescription)
            || !roleDescription.isSetForNewObjects())
        {
            return;
        }
        String roleName = getRoleName(roleDescription);
        if (roleName == null || roleName.isBlank()
            || fullAccessRoleNames(parameters).contains(roleName.toLowerCase(Locale.ROOT)))
        {
            return;
        }
        String message = MessageFormat.format(Messages.RightSetPermissionsForNewObjectsCheck_message, roleName);
        resultAceptor.addIssue(message, ROLE_DESCRIPTION__SET_FOR_NEW_OBJECTS);
    }

    private String getRoleName(RoleDescription roleDescription)
    {
        IBmModel model = bmModelManager.getModel(roleDescription);
        if (model == null)
        {
            return null;
        }
        EObject owner = RightsModelUtil.getOwner(roleDescription, model);
        return owner instanceof Role role ? role.getName() : null;
    }

    private static Set<String> fullAccessRoleNames(ICheckParameters parameters)
    {
        String value;
        try
        {
            value = parameters.getString(PARAM_NAMES_FULL_ACCESS_ROLE);
        }
        catch (WrongParameterException e)
        {
            value = DEFAULT_NAMES_FULL_ACCESS_ROLE;
        }
        if (value == null || value.isBlank())
        {
            value = DEFAULT_NAMES_FULL_ACCESS_ROLE;
        }
        Set<String> names = new LinkedHashSet<>();
        for (String name : value.split(",")) //$NON-NLS-1$
        {
            String trimmed = name.trim();
            if (!trimmed.isEmpty())
            {
                names.add(trimmed.toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }
}
