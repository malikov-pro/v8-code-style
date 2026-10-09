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
 *     malikov-pro - port of the upstream issue #722 (АПК rule 1211)
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BASIC_DB_OBJECT;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BASIC_DB_OBJECT__EXTENDED_OBJECT_PRESENTATION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BASIC_DB_OBJECT__OBJECT_PRESENTATION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.INFORMATION_REGISTER;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.INFORMATION_REGISTER__EXTENDED_RECORD_PRESENTATION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.INFORMATION_REGISTER__RECORD_PRESENTATION;

import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.EStructuralFeature;

import com._1c.g5.v8.dt.metadata.mdclass.BasicDbObject;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegister;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.md.CorePlugin;

/**
 * The extended object presentation equals the object presentation.
 * <p>
 * The extended object presentation (for a register — the extended record
 * presentation, рус. «Расширенное представление объекта» / «Расширенное
 * представление записи») is filled with the full name of the object in the
 * singular. It is shown in object form titles and hints instead of the
 * object presentation, so filling it with the same text as the object
 * presentation is meaningless: in this case it should not be filled at all
 * (standard 468, clause 3).
 * <p>
 * The presentations are compared in every language where both are specified,
 * ignoring case and leading/trailing spaces. If in at least one language the
 * extended presentation equals the plain presentation (both non-empty), the
 * object is reported. The case where the object presentation is not filled
 * and the extended presentation equals the synonym is out of the scope of
 * this check (that is the separate АПК rule 1213).
 * <p>
 * Checked are top DB objects (catalogs, documents, etc.) and information
 * registers. Accumulation, accounting and calculation registers have no
 * record presentation properties in the metadata model. Objects adopted in
 * extension configurations are not checked.
 *
 * @author malikov-pro
 */
public class MdExtendedObjectPresentationEqualsCheck
    extends BasicCheck
{

    /** The check id of the upstream issue #722 (АПК rule 1211) port. */
    public static final String CHECK_ID = "up-722-ext-presentation-equals-object"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public MdExtendedObjectPresentationEqualsCheck()
    {
        super();
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.MdExtendedObjectPresentationEqualsCheck_title)
            .description(Messages.MdExtendedObjectPresentationEqualsCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.UI_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .extension(new SkipAdoptedInExtensionMdObjectExtension());

        builder.topObject(BASIC_DB_OBJECT)
            .checkTop()
            .features(BASIC_DB_OBJECT__OBJECT_PRESENTATION, BASIC_DB_OBJECT__EXTENDED_OBJECT_PRESENTATION);
        builder.topObject(INFORMATION_REGISTER)
            .checkTop()
            .features(INFORMATION_REGISTER__RECORD_PRESENTATION, INFORMATION_REGISTER__EXTENDED_RECORD_PRESENTATION);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (monitor.isCanceled())
        {
            return;
        }

        EMap<String, String> extended;
        EMap<String, String> presentation;
        EStructuralFeature extendedFeature;

        if (object instanceof BasicDbObject dbObject)
        {
            presentation = dbObject.getObjectPresentation();
            extended = dbObject.getExtendedObjectPresentation();
            extendedFeature = BASIC_DB_OBJECT__EXTENDED_OBJECT_PRESENTATION;
        }
        else if (object instanceof InformationRegister register)
        {
            presentation = register.getRecordPresentation();
            extended = register.getExtendedRecordPresentation();
            extendedFeature = INFORMATION_REGISTER__EXTENDED_RECORD_PRESENTATION;
        }
        else
        {
            return;
        }

        String matched = firstEqualValue(extended, presentation, monitor);
        if (matched != null)
        {
            resultAceptor.addIssue(
                MessageFormat.format(Messages.MdExtendedObjectPresentationEqualsCheck_message, matched),
                extendedFeature);
        }
    }

    /**
     * Returns the first value of the extended presentation that equals the
     * plain presentation in the same language, ignoring case and
     * leading/trailing spaces, or {@code null} if the presentations differ
     * in every language where both are specified.
     */
    private static String firstEqualValue(EMap<String, String> extended, EMap<String, String> presentation,
        IProgressMonitor monitor)
    {
        if (extended == null || presentation == null)
        {
            return null;
        }
        for (Map.Entry<String, String> entry : extended.entrySet())
        {
            if (monitor.isCanceled())
            {
                return null;
            }
            String extendedValue = entry.getValue();
            if (extendedValue == null || extendedValue.isBlank())
            {
                continue;
            }
            String presentationValue = presentation.get(entry.getKey());
            if (presentationValue == null || presentationValue.isBlank())
            {
                continue;
            }
            if (extendedValue.strip().equalsIgnoreCase(presentationValue.strip()))
            {
                return extendedValue.strip();
            }
        }
        return null;
    }
}
