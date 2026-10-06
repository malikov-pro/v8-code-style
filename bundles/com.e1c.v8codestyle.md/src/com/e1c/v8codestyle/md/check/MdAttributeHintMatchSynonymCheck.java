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
 *     malikov-pro - port of the АПК rule АПК_00134
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BASIC_DB_OBJECT;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BASIC_FEATURE__TOOL_TIP;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BASIC_REGISTER;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT__SYNONYM;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.STANDARD_ATTRIBUTE;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.STANDARD_ATTRIBUTE__SYNONYM;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.STANDARD_ATTRIBUTE__TOOL_TIP;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.core.platform.IConfigurationProvider;
import com._1c.g5.v8.dt.metadata.mdclass.BasicFeature;
import com._1c.g5.v8.dt.metadata.mdclass.BasicTabularSection;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegisterPeriodicity;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com._1c.g5.v8.dt.metadata.mdclass.StandardAttribute;
import com._1c.g5.v8.dt.metadata.mdclass.Subsystem;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.md.CorePlugin;
import com.google.inject.Inject;

/**
 * The hint of an attribute matches its synonym.
 * <p>
 * A hint that just repeats the synonym of the attribute does not explain
 * the purpose of the attribute to the user. Hints are set for attributes
 * that are exposed to the user as interface elements and require an
 * explanation of their purpose.
 * <p>
 * As in the source АПК algorithm:
 * <ul>
 * <li>attributes with an empty hint are not checked;</li>
 * <li>of standard attributes only Code, Description, Parent, Owner and
 * Period are checked, whereas the Period of a non-periodic information
 * register is not checked;</li>
 * <li>only attributes of objects included in at least one subsystem with
 * the "Include in command interface" flag are checked; for an attribute of
 * a tabular section the owner is the object of the tabular section.</li>
 * </ul>
 * <p>
 * Checked are object attributes, tabular section attributes, register
 * dimensions and resources, and the listed standard attributes. Objects
 * adopted in extension configurations are not checked.
 *
 * @author malikov-pro
 */
public class MdAttributeHintMatchSynonymCheck
    extends BasicCheck
{

    /** The check id of the АПК_00134 rule port. */
    public static final String CHECK_ID = "apk-00134-attribute-hint"; //$NON-NLS-1$

    /**
     * Standard attributes (АПК: Код, Наименование, Родитель, Владелец,
     * Период) which are the only standard attributes checked by this rule.
     */
    private static final Set<String> STANDARD_ATTRIBUTE_NAMES =
        Set.of("Code", "Description", "Parent", "Owner", "Period"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

    private static final String PERIOD_NAME = "Period"; //$NON-NLS-1$

    private final IConfigurationProvider configurationProvider;

    /**
     * Instantiates a new check.
     *
     * @param configurationProvider the configuration provider service, cannot be {@code null}.
     */
    @Inject
    public MdAttributeHintMatchSynonymCheck(IConfigurationProvider configurationProvider)
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
        builder.title(Messages.MdAttributeHintMatchSynonymCheck_title)
            .description(Messages.MdAttributeHintMatchSynonymCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.UI_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .extension(new SkipAdoptedInExtensionMdObjectExtension());

        // Object attributes, tabular section attributes, register dimensions and resources
        builder.topObject(MD_OBJECT)
            .containment(MD_OBJECT)
            .features(MD_OBJECT__SYNONYM, BASIC_FEATURE__TOOL_TIP);
        // Standard attributes of catalogs, documents and other DB objects
        builder.topObject(BASIC_DB_OBJECT)
            .containment(STANDARD_ATTRIBUTE)
            .features(STANDARD_ATTRIBUTE__SYNONYM, STANDARD_ATTRIBUTE__TOOL_TIP);
        // Standard attributes of registers (Period of the information register)
        builder.topObject(BASIC_REGISTER)
            .containment(STANDARD_ATTRIBUTE)
            .features(STANDARD_ATTRIBUTE__SYNONYM, STANDARD_ATTRIBUTE__TOOL_TIP);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (monitor.isCanceled() || !(object instanceof EObject))
        {
            return;
        }
        EObject eObject = (EObject)object;

        String name;
        EMap<String, String> synonym;
        EMap<String, String> hint;
        EStructuralFeature toolTipFeature;
        EObject owner;

        if (eObject instanceof StandardAttribute)
        {
            StandardAttribute attribute = (StandardAttribute)eObject;
            if (!STANDARD_ATTRIBUTE_NAMES.contains(attribute.getName()))
            {
                // Of standard attributes only Code, Description, Parent, Owner and Period are checked.
                return;
            }
            // Standard attributes of a standard tabular section are out of the scope of this check.
            owner = attribute.eContainer();
            if (!(owner instanceof MdObject))
            {
                return;
            }
            name = attribute.getName();
            synonym = attribute.getSynonym();
            hint = attribute.getToolTip();
            toolTipFeature = STANDARD_ATTRIBUTE__TOOL_TIP;
        }
        else if (eObject instanceof BasicFeature)
        {
            BasicFeature feature = (BasicFeature)eObject;
            owner = ownerObject(feature);
            if (!(owner instanceof MdObject))
            {
                return;
            }
            name = feature.getName();
            synonym = feature.getSynonym();
            hint = feature.getToolTip();
            toolTipFeature = BASIC_FEATURE__TOOL_TIP;
        }
        else
        {
            return;
        }

        // The standard attribute "Period" of a non-periodic information register is not checked.
        if (PERIOD_NAME.equals(name) && owner instanceof InformationRegister
            && ((InformationRegister)owner).getInformationRegisterPeriodicity() == InformationRegisterPeriodicity.NONPERIODICAL)
        {
            return;
        }

        // Only attributes of objects included in a subsystem with the
        // "Include in command interface" flag are checked.
        if (!isIncludedInCommandInterfaceSubsystem((MdObject)owner, monitor))
        {
            return;
        }

        // An attribute with an empty hint is not checked.
        if (!hasNonBlankValue(hint))
        {
            return;
        }

        if (matchesSynonym(hint, synonym))
        {
            resultAceptor.addIssue(
                MessageFormat.format(Messages.MdAttributeHintMatchSynonymCheck_message, name), eObject,
                toolTipFeature);
        }
    }

    /**
     * Returns the owner object of the feature: for an attribute of a tabular
     * section it is the object of the tabular section, as in the АПК algorithm.
     */
    private static EObject ownerObject(BasicFeature feature)
    {
        EObject owner = feature.eContainer();
        if (owner instanceof BasicTabularSection)
        {
            owner = owner.eContainer();
        }
        return owner;
    }

    private boolean isIncludedInCommandInterfaceSubsystem(MdObject owner, IProgressMonitor monitor)
    {
        Configuration configuration = configurationProvider.getConfiguration(owner);
        if (configuration == null)
        {
            return false;
        }
        for (Subsystem subsystem : configuration.getSubsystems())
        {
            if (monitor.isCanceled())
            {
                return false;
            }
            if (isIncludedInCommandInterfaceSubsystem(subsystem, owner))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isIncludedInCommandInterfaceSubsystem(Subsystem subsystem, MdObject owner)
    {
        if (subsystem.isIncludeInCommandInterface() && containsOwner(subsystem, owner))
        {
            return true;
        }
        for (Subsystem child : subsystem.getSubsystems())
        {
            if (isIncludedInCommandInterfaceSubsystem(child, owner))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean containsOwner(Subsystem subsystem, MdObject owner)
    {
        String ownerFqn = fqn(owner);
        if (ownerFqn == null)
        {
            return false;
        }
        for (MdObject item : subsystem.getContent())
        {
            if (ownerFqn.equals(fqn(item)))
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

    private static boolean hasNonBlankValue(EMap<String, String> map)
    {
        if (map == null)
        {
            return false;
        }
        for (Map.Entry<String, String> entry : map.entrySet())
        {
            String value = entry.getValue();
            if (value != null && !value.isBlank())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the hint equals the synonym in any language where both
     * are specified. The {@code СтрСравнить()} of the АПК algorithm compares
     * strings ignoring case.
     */
    private static boolean matchesSynonym(EMap<String, String> hint, EMap<String, String> synonym)
    {
        if (hint == null || synonym == null)
        {
            return false;
        }
        for (Map.Entry<String, String> entry : hint.entrySet())
        {
            String hintValue = entry.getValue();
            if (hintValue == null || hintValue.isBlank())
            {
                continue;
            }
            String synonymValue = synonym.get(entry.getKey());
            if (synonymValue == null)
            {
                continue;
            }
            if (hintValue.strip().equalsIgnoreCase(synonymValue.strip()))
            {
                return true;
            }
        }
        return false;
    }
}
