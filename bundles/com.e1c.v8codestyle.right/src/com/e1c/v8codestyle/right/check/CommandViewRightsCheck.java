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
 *     malikov-pro - port of the upstream issue #644 (standard 689)
 *******************************************************************************/
package com.e1c.v8codestyle.right.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BASIC_COMMAND;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT__SYNONYM;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EMap;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.core.platform.IConfigurationProvider;
import com._1c.g5.v8.dt.mcore.NamedElement;
import com._1c.g5.v8.dt.metadata.mdclass.BasicCommand;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com._1c.g5.v8.dt.metadata.mdclass.Role;
import com._1c.g5.v8.dt.metadata.mdclass.Subsystem;
import com._1c.g5.v8.dt.rights.IRightInfosService;
import com._1c.g5.v8.dt.rights.model.ObjectRight;
import com._1c.g5.v8.dt.rights.model.ObjectRights;
import com._1c.g5.v8.dt.rights.model.Right;
import com._1c.g5.v8.dt.rights.model.RightValue;
import com._1c.g5.v8.dt.rights.model.RoleDescription;
import com._1c.g5.v8.dt.rights.model.util.RightName;
import com._1c.g5.v8.dt.rights.model.util.RightsModelUtil;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.right.CorePlugin;
import com.google.inject.Inject;

/**
 * Checks that at least one role grants the {@code View} right for the owner
 * object of a command that is available in the command interface.
 * <p>
 * Rights for a command derive from the rights of its owner object (standard
 * 689): if a command is visible in the command interface but no role grants
 * the View right for the owner object, users can never see or open the
 * command, so the command is dead configuration content.
 * <p>
 * As in the source issue:
 * <ul>
 * <li>only commands of configuration objects are checked; a common command
 * has no owner object whose rights govern access to it;</li>
 * <li>only commands available in the command interface are checked, that is
 * the owner object is included in a subsystem, where the subsystem itself
 * and all its parent subsystems up to the root have the "Include in command
 * interface" flag set;</li>
 * <li>the right is counted for a role when it is explicitly granted for the
 * owner object or, when the owner object has no custom rights in the role,
 * the default right value of the role applies to it;</li>
 * <li>adopted objects of extensions are not checked.</li>
 * </ul>
 *
 * @author malikov-pro
 */
public class CommandViewRightsCheck
    extends BasicCheck
{

    /** The check id of the upstream issue #644 port. */
    public static final String CHECK_ID = "up-644-command-view-rights"; //$NON-NLS-1$

    private final IConfigurationProvider configurationProvider;

    private final IRightInfosService rightInfosService;

    /**
     * Instantiates a new check.
     *
     * @param configurationProvider the configuration provider service, cannot be {@code null}.
     * @param rightInfosService the right infos service, cannot be {@code null}.
     */
    @Inject
    public CommandViewRightsCheck(IConfigurationProvider configurationProvider, IRightInfosService rightInfosService)
    {
        super();
        this.configurationProvider = configurationProvider;
        this.rightInfosService = rightInfosService;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.CommandViewRightsCheck_title)
            .description(Messages.CommandViewRightsCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.UI_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID));

        // Common commands are top MdObjects; commands of configuration
        // objects are contained in their owner MdObjects. Top objects are
        // filtered out in check() by the bmIsTop() sign.
        builder.topObject(MD_OBJECT)
            .checkTop()
            .features(MD_OBJECT__SYNONYM)
            .containment(BASIC_COMMAND)
            .features(MD_OBJECT__SYNONYM);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (monitor.isCanceled() || !(object instanceof BasicCommand command))
        {
            return;
        }

        // Only commands of configuration objects are checked: a common
        // command has no owner object whose rights govern access to it.
        IBmObject bmObject = (IBmObject)command;
        if (bmObject.bmIsTop() || !(bmObject.bmGetTopObject() instanceof MdObject owner))
        {
            return;
        }

        if (RightsModelUtil.isAdoptedMdObject(command))
        {
            return;
        }

        if (!supportsViewRight(owner))
        {
            return;
        }

        // Only commands available in the command interface are checked.
        if (!isAvailableInCommandInterface(command, owner, monitor))
        {
            return;
        }

        Configuration configuration = configurationProvider.getConfiguration(command);
        if (configuration == null)
        {
            return;
        }

        for (Role role : configuration.getRoles())
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (hasViewRight(role, owner))
            {
                return;
            }
        }

        resultAceptor.addIssue(
            MessageFormat.format(Messages.CommandViewRightsCheck_No_role_has_view_right, ownerName(owner)),
            MD_OBJECT__SYNONYM);
    }

    /**
     * Returns {@code true} if the owner object class supports the {@code View}
     * right at all, as objects without this right are governed by other
     * rights.
     */
    private boolean supportsViewRight(MdObject owner)
    {
        Set<Right> rights = rightInfosService.getEClassRights(owner, owner.eClass());
        return rights.stream()
            .map(NamedElement::getName)
            .anyMatch(RightName.VIEW.getName()::equals);
    }

    /**
     * Returns {@code true} if the role grants the {@code View} right for the
     * owner object: either explicitly set for the object or, when the object
     * has no custom rights in the role, the default right value of the role
     * applies to the object.
     */
    private boolean hasViewRight(Role role, MdObject owner)
    {
        if (!(role.getRights() instanceof RoleDescription description))
        {
            return false;
        }

        RightValue value = null;
        ObjectRights objectRights = RightsModelUtil.filterObjectRightsByEObject(owner, description.getRights());
        if (objectRights != null)
        {
            for (ObjectRight objectRight : objectRights.getRights())
            {
                Right right = objectRight.getRight();
                if (right != null && RightName.VIEW.getName().equals(right.getName()))
                {
                    value = objectRight.getValue();
                    break;
                }
            }
        }

        if (value == null)
        {
            value = RightsModelUtil.getDefaultRightValue(owner, role);
        }
        return RightsModelUtil.getBooleanRightValue(value);
    }

    /**
     * Returns {@code true} if the owner object of the command is included in
     * a subsystem, where the subsystem itself and the whole chain of its
     * parent subsystems up to the root have the "Include in command
     * interface" flag set, as in the source АПК algorithm of the command
     * interface availability.
     */
    private boolean isAvailableInCommandInterface(BasicCommand command, MdObject owner, IProgressMonitor monitor)
    {
        Configuration configuration = configurationProvider.getConfiguration(command);
        if (configuration == null)
        {
            return false;
        }
        String membershipFqn = ((IBmObject)owner).bmGetFqn();
        if (membershipFqn == null)
        {
            return false;
        }
        for (Subsystem subsystem : configuration.getSubsystems())
        {
            if (monitor.isCanceled())
            {
                return false;
            }
            if (isInAvailableSubsystem(subsystem, membershipFqn))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the subsystem (or one of its child subsystems
     * recursively) contains the object and the whole chain of subsystems from
     * the containing one up to the root has the "Include in command
     * interface" flag set.
     */
    private static boolean isInAvailableSubsystem(Subsystem subsystem, String membershipFqn)
    {
        if (containsObject(subsystem, membershipFqn))
        {
            return isAvailableChain(subsystem);
        }
        for (Subsystem child : subsystem.getSubsystems())
        {
            if (isInAvailableSubsystem(child, membershipFqn))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the subsystem itself and all its parent
     * subsystems up to the root have the "Include in command interface" flag
     * set.
     */
    private static boolean isAvailableChain(Subsystem subsystem)
    {
        Subsystem current = subsystem;
        while (current != null)
        {
            if (!current.isIncludeInCommandInterface())
            {
                return false;
            }
            current = current.getParentSubsystem();
        }
        return true;
    }

    private static boolean containsObject(Subsystem subsystem, String membershipFqn)
    {
        for (MdObject item : subsystem.getContent())
        {
            if (item instanceof IBmObject bmItem && membershipFqn.equals(bmItem.bmGetFqn()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the first synonym value (in any language) of the owner object
     * or its name when the synonym is empty.
     */
    private static String ownerName(MdObject owner)
    {
        EMap<String, String> synonym = owner.getSynonym();
        if (synonym != null)
        {
            for (Map.Entry<String, String> entry : synonym.entrySet())
            {
                String value = entry.getValue();
                if (value != null && !value.isBlank())
                {
                    return value;
                }
            }
        }
        return owner.getName();
    }

}
