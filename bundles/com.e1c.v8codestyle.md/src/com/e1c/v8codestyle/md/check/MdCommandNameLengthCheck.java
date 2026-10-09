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
 *     malikov-pro - port of the АПК rule АПК_00528
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BASIC_COMMAND;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BASIC_COMMAND__GROUP;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT__SYNONYM;

import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.core.platform.IConfigurationProvider;
import com._1c.g5.v8.dt.metadata.mdclass.BasicCommand;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com._1c.g5.v8.dt.metadata.mdclass.Subsystem;
import com._1c.g5.v8.dt.mcore.CommandGroup;
import com._1c.g5.v8.dt.mcore.CommandGroupCategory;
import com._1c.g5.v8.dt.mcore.StandardCommandGroup;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.md.CorePlugin;
import com.google.inject.Inject;

/**
 * The name of a command is longer than 38 characters.
 * <p>
 * To avoid scroll bars at the standard screen resolution, the name
 * (synonym) of a command should not exceed 38 characters. As in the source
 * АПК algorithm:
 * <ul>
 * <li>only commands and common commands are checked, other objects are not
 * checked;</li>
 * <li>if the synonym is empty, the command name is checked;</li>
 * <li>commands whose group is a form command group are not checked;</li>
 * <li>only commands available in the command interface are checked, that is
 * the command (for a command of an object — the owner object) is included
 * in a subsystem, where the subsystem itself and all its parent subsystems
 * up to the root have the "Include in command interface" flag set.</li>
 * </ul>
 * <p>
 * In the EDT model both common commands (top objects) and commands of
 * configuration objects are {@link BasicCommand}. The message contains the
 * section — the synonym of the root subsystem of the command.
 *
 * @author malikov-pro
 */
public class MdCommandNameLengthCheck
    extends BasicCheck
{

    /** The check id of the АПК_00528 rule port. */
    public static final String CHECK_ID = "apk-00528-command-name-length"; //$NON-NLS-1$

    /** The parameter key of the maximum command name length. */
    public static final String MAX_COMMAND_NAME_LENGTH = "maxCommandNameLength"; //$NON-NLS-1$

    /** The default maximum command name length. */
    public static final String MAX_COMMAND_NAME_LENGTH_DEFAULT = "38"; //$NON-NLS-1$

    private final IConfigurationProvider configurationProvider;

    /**
     * Instantiates a new check.
     *
     * @param configurationProvider the configuration provider service, cannot be {@code null}.
     */
    @Inject
    public MdCommandNameLengthCheck(IConfigurationProvider configurationProvider)
    {
        super();
        this.configurationProvider = configurationProvider;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.MdCommandNameLengthCheck_title)
            .description(Messages.MdCommandNameLengthCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.UI_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .extension(new SkipAdoptedInExtensionMdObjectExtension())
            .parameter(MAX_COMMAND_NAME_LENGTH, Integer.class, MAX_COMMAND_NAME_LENGTH_DEFAULT,
                Messages.MdCommandNameLengthCheck_parameter);

        // Common commands are top MdObjects; commands of configuration
        // objects are contained in their owner MdObjects. Commands are
        // filtered out in check() by the BasicCommand type.
        builder.topObject(MD_OBJECT)
            .checkTop()
            .features(MD_OBJECT__SYNONYM, BASIC_COMMAND__GROUP)
            .containment(BASIC_COMMAND)
            .features(MD_OBJECT__SYNONYM, BASIC_COMMAND__GROUP);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (monitor.isCanceled() || !(object instanceof BasicCommand))
        {
            return;
        }
        BasicCommand command = (BasicCommand)object;

        // Commands of form command groups are not checked.
        if (isFormGroup(command.getGroup()))
        {
            return;
        }

        int max = parameters.getInt(MAX_COMMAND_NAME_LENGTH);
        if (max <= 0)
        {
            return;
        }

        String longName = longName(command, max);
        if (longName == null)
        {
            return;
        }

        // Only commands available in the command interface are checked.
        String section = commandInterfaceSection(command, monitor);
        if (section == null)
        {
            return;
        }

        resultAceptor.addIssue(
            MessageFormat.format(Messages.MdCommandNameLengthCheck_message, longName, max, section), MD_OBJECT__SYNONYM);
    }

    /**
     * Returns {@code true} if the command group is a form command group
     * ("form navigation panel" or "form command bar" category), as the
     * source АПК algorithm skips commands whose group refers to a form.
     * The group of an EDT command is either a standard (predefined) command
     * group or a configuration command group.
     */
    private static boolean isFormGroup(CommandGroup group)
    {
        CommandGroupCategory category = null;
        if (group instanceof StandardCommandGroup)
        {
            category = ((StandardCommandGroup)group).getCategory();
        }
        else if (group instanceof com._1c.g5.v8.dt.metadata.mdclass.CommandGroup)
        {
            category = ((com._1c.g5.v8.dt.metadata.mdclass.CommandGroup)group).getCategory();
        }
        return category == CommandGroupCategory.FORM_NAVIGATION_PANEL
            || category == CommandGroupCategory.FORM_COMMAND_BAR;
    }

    /**
     * Returns the first synonym value (in any language) that is longer than
     * the maximum length; if the synonym is empty, returns the command name
     * when it is longer than the maximum length, otherwise returns
     * {@code null}.
     */
    private static String longName(BasicCommand command, int max)
    {
        boolean hasSynonym = false;
        EMap<String, String> synonym = command.getSynonym();
        if (synonym != null)
        {
            for (Map.Entry<String, String> entry : synonym.entrySet())
            {
                String value = entry.getValue();
                if (value == null || value.isBlank())
                {
                    continue;
                }
                hasSynonym = true;
                if (value.length() > max)
                {
                    return value;
                }
            }
        }
        // If the synonym is empty, the command name is checked.
        String name = command.getName();
        if (!hasSynonym && name != null && !name.isBlank() && name.length() > max)
        {
            return name;
        }
        return null;
    }

    /**
     * Returns the section of the command — the synonym (or name) of the root
     * subsystem, if the command is included in a subsystem where the whole
     * chain of subsystems up to the root has the "Include in command
     * interface" flag set. For a command of an object the owner object is
     * checked, as in the source АПК algorithm. Returns {@code null} if the
     * command is not available in the command interface.
     */
    private String commandInterfaceSection(BasicCommand command, IProgressMonitor monitor)
    {
        Configuration configuration = configurationProvider.getConfiguration(command);
        if (configuration == null)
        {
            return null;
        }
        String membershipFqn = membershipFqn(command);
        if (membershipFqn == null)
        {
            return null;
        }
        for (Subsystem subsystem : configuration.getSubsystems())
        {
            if (monitor.isCanceled())
            {
                return null;
            }
            String section = sectionOfContainingSubsystem(subsystem, membershipFqn);
            if (section != null)
            {
                return section;
            }
        }
        return null;
    }

    /**
     * Returns the FQN of the object whose subsystem membership is checked:
     * for a command of an object it is the FQN of the owner object (the top
     * object of the command), for a common command it is the FQN of the
     * command itself. The FQN of a non-top object cannot be requested from
     * the BM, so the top object is used for commands of objects.
     */
    private static String membershipFqn(BasicCommand command)
    {
        IBmObject bmObject = (IBmObject)command;
        if (bmObject.bmIsTop())
        {
            return fqn(bmObject);
        }
        return fqn(bmObject.bmGetTopObject());
    }

    /**
     * If the subsystem (or one of its child subsystems recursively) contains
     * the object and the whole chain of subsystems from the containing one up
     * to the root has the "Include in command interface" flag set, returns
     * the section name — the synonym (or name) of the root subsystem.
     */
    private static String sectionOfContainingSubsystem(Subsystem subsystem, String membershipFqn)
    {
        if (containsObject(subsystem, membershipFqn))
        {
            return isAvailableChain(subsystem) ? sectionName(subsystem) : null;
        }
        for (Subsystem child : subsystem.getSubsystems())
        {
            String section = sectionOfContainingSubsystem(child, membershipFqn);
            if (section != null)
            {
                return section;
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if the subsystem itself and all its parent
     * subsystems up to the root have the "Include in command interface" flag
     * set, as in the source АПК algorithm.
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

    /**
     * Returns the synonym (in any language) or the name of the root
     * subsystem of the chain.
     */
    private static String sectionName(Subsystem subsystem)
    {
        Subsystem root = subsystem;
        while (root.getParentSubsystem() != null)
        {
            root = root.getParentSubsystem();
        }
        String synonym = firstNonBlankValue(root.getSynonym());
        return synonym != null ? synonym : root.getName();
    }

    private static String firstNonBlankValue(EMap<String, String> map)
    {
        if (map == null)
        {
            return null;
        }
        for (Map.Entry<String, String> entry : map.entrySet())
        {
            String value = entry.getValue();
            if (value != null && !value.isBlank())
            {
                return value;
            }
        }
        return null;
    }

    private static boolean containsObject(Subsystem subsystem, String membershipFqn)
    {
        for (MdObject item : subsystem.getContent())
        {
            if (membershipFqn.equals(fqn(item)))
            {
                return true;
            }
        }
        return false;
    }

    private static String fqn(EObject object)
    {
        return object instanceof IBmObject ? ((IBmObject)object).bmGetFqn() : null;
    }
}
