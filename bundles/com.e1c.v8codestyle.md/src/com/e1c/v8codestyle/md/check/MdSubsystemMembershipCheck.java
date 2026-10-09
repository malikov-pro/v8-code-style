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
 *     malikov-pro - port of the АПК rule АПК_00458
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.ACCOUNTING_REGISTER;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.ACCUMULATION_REGISTER;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BUSINESS_PROCESS;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.CALCULATION_REGISTER;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.CATALOG;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.CHART_OF_ACCOUNTS;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.CHART_OF_CALCULATION_TYPES;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.CHART_OF_CHARACTERISTIC_TYPES;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.COMMON_COMMAND;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.COMMON_FORM;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.COMMON_MODULE;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.COMMON_TEMPLATE;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.CONSTANT;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.DATA_PROCESSOR;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.DOCUMENT;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.DOCUMENT_JOURNAL;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.ENUM;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.EXCHANGE_PLAN;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.EXTERNAL_DATA_SOURCE;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.FILTER_CRITERION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.HTTP_SERVICE;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.INFORMATION_REGISTER;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT__NAME;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.REPORT;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.SEQUENCE;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.TASK;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.WEB_SERVICE;

import java.text.MessageFormat;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.core.platform.IConfigurationProvider;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
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
 * The metadata object is not included in any subsystem.
 * <p>
 * Every object of the configuration should be included in at least one
 * subsystem (of the whole tree of subsystems, including nested ones), so it
 * has a place in the configuration sections. Objects adopted in extension
 * configurations are not checked.
 * <p>
 * The configuration subsystems are empty or the object is included in none
 * of them — the issue is added. Service objects that are not included in
 * subsystems (subsystems themselves, languages, event subscriptions, common
 * pictures, command groups, etc.) are out of the scope of this check — only
 * applied objects and shared objects that can be included in a subsystem
 * composition are checked.
 *
 * @author malikov-pro
 */
public class MdSubsystemMembershipCheck
    extends BasicCheck
{

    /** The check id of the АПК_00458 rule port. */
    public static final String CHECK_ID = "apk-00458-subsystem-membership"; //$NON-NLS-1$

    /**
     * Top metadata object classes that can be included in a subsystem
     * composition and therefore are checked. Service objects (subsystems,
     * languages, event subscriptions, common pictures, command groups,
     * common attributes, defined types, styles, roles, XDTO packages, web
     * socket clients, bots, etc.) cannot be included in a subsystem
     * composition and are not checked.
     */
    private static final Set<EClass> CHECKED_TYPES = Set.of(CATALOG, DOCUMENT, DOCUMENT_JOURNAL, ENUM, CONSTANT,
        REPORT, DATA_PROCESSOR, BUSINESS_PROCESS, TASK, EXCHANGE_PLAN, CHART_OF_CHARACTERISTIC_TYPES,
        CHART_OF_ACCOUNTS, CHART_OF_CALCULATION_TYPES, INFORMATION_REGISTER, ACCUMULATION_REGISTER,
        ACCOUNTING_REGISTER, CALCULATION_REGISTER, SEQUENCE, FILTER_CRITERION, COMMON_FORM, COMMON_COMMAND,
        COMMON_MODULE, COMMON_TEMPLATE, WEB_SERVICE, HTTP_SERVICE, EXTERNAL_DATA_SOURCE);

    private final IConfigurationProvider configurationProvider;

    /**
     * Instantiates a new check.
     *
     * @param configurationProvider the configuration provider service, cannot be {@code null}.
     */
    @Inject
    public MdSubsystemMembershipCheck(IConfigurationProvider configurationProvider)
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
        builder.title(Messages.MdSubsystemMembershipCheck_title)
            .description(Messages.MdSubsystemMembershipCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.UI_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .extension(new SkipAdoptedInExtensionMdObjectExtension());

        builder.topObject(MD_OBJECT)
            .checkTop()
            .features(MD_OBJECT__NAME);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (monitor.isCanceled() || !(object instanceof MdObject))
        {
            return;
        }
        MdObject mdObject = (MdObject)object;
        if (!CHECKED_TYPES.contains(mdObject.eClass()))
        {
            return;
        }

        Configuration configuration = configurationProvider.getConfiguration(mdObject);
        if (configuration == null)
        {
            return;
        }
        String fqn = fqn(mdObject);
        if (fqn == null)
        {
            return;
        }

        if (isIncludedInAnySubsystem(configuration, fqn, monitor))
        {
            return;
        }

        resultAceptor.addIssue(
            MessageFormat.format(Messages.MdSubsystemMembershipCheck_message, mdObject.getName()));
    }

    /**
     * Returns {@code true} if the object is included in the composition of
     * at least one subsystem of the configuration tree, including nested
     * subsystems. The subsystem tree is traversed once per object with early
     * exit on the first containing subsystem: the content FQNs are not
     * cached because configuration objects are long-lived and a cache would
     * be stale after the subsystem composition changes.
     */
    private static boolean isIncludedInAnySubsystem(Configuration configuration, String fqn, IProgressMonitor monitor)
    {
        for (Subsystem subsystem : configuration.getSubsystems())
        {
            if (monitor.isCanceled())
            {
                return true;
            }
            if (isIncludedInSubsystemTree(subsystem, fqn))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isIncludedInSubsystemTree(Subsystem subsystem, String fqn)
    {
        if (containsObject(subsystem, fqn))
        {
            return true;
        }
        for (Subsystem child : subsystem.getSubsystems())
        {
            if (isIncludedInSubsystemTree(child, fqn))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean containsObject(Subsystem subsystem, String fqn)
    {
        for (MdObject item : subsystem.getContent())
        {
            if (fqn.equals(fqn(item)))
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
