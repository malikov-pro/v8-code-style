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
 *     malikov-pro - port of the upstream issue #725 (АПК rule 1215)
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.ACCUMULATION_REGISTER;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.ACCUMULATION_REGISTER__EXTENDED_LIST_PRESENTATION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.ACCUMULATION_REGISTER__LIST_PRESENTATION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.ACCOUNTING_REGISTER;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.ACCOUNTING_REGISTER__EXTENDED_LIST_PRESENTATION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.ACCOUNTING_REGISTER__LIST_PRESENTATION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BASIC_DB_OBJECT;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BASIC_DB_OBJECT__EXTENDED_LIST_PRESENTATION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BASIC_DB_OBJECT__LIST_PRESENTATION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.CALCULATION_REGISTER;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.CALCULATION_REGISTER__EXTENDED_LIST_PRESENTATION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.CALCULATION_REGISTER__LIST_PRESENTATION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.DOCUMENT_JOURNAL;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.DOCUMENT_JOURNAL__EXTENDED_LIST_PRESENTATION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.DOCUMENT_JOURNAL__LIST_PRESENTATION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.ENUM;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.ENUM__EXTENDED_LIST_PRESENTATION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.ENUM__LIST_PRESENTATION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.FILTER_CRITERION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.FILTER_CRITERION__EXTENDED_LIST_PRESENTATION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.FILTER_CRITERION__LIST_PRESENTATION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.INFORMATION_REGISTER;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.INFORMATION_REGISTER__EXTENDED_LIST_PRESENTATION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.INFORMATION_REGISTER__LIST_PRESENTATION;

import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.md.CorePlugin;

/**
 * The extended list presentation equals the list presentation.
 * <p>
 * The extended list presentation (рус. «Расширенное представление списка»)
 * is filled with the full name of the object list. It is shown in list form
 * titles and hints instead of the list presentation, so filling it with the
 * same text as the list presentation is meaningless: in this case it should
 * not be filled at all (standard 468, clause 5).
 * <p>
 * The presentations are compared in every language where both are specified,
 * ignoring case and leading/trailing spaces. If in at least one language the
 * extended list presentation equals the list presentation (both non-empty),
 * the object is reported. The case where the list presentation is not filled
 * and the extended list presentation equals the synonym is out of the scope
 * of this check (that is the separate АПК rule 1216).
 * <p>
 * Checked are top metadata objects that have both list presentation
 * properties: DB objects (catalogs, documents, etc.), information registers,
 * accumulation, accounting and calculation registers, enums, document
 * journals and filter criteria. Objects adopted in extension configurations
 * are not checked.
 *
 * @author malikov-pro
 */
public class MdExtendedListPresentationEqualsCheck
    extends BasicCheck
{

    /** The check id of the upstream issue #725 (АПК rule 1215) port. */
    public static final String CHECK_ID = "up-725-ext-presentation-equals-list"; //$NON-NLS-1$

    private static final String LIST_PRESENTATION_FEATURE_NAME = "listPresentation"; //$NON-NLS-1$
    private static final String EXTENDED_LIST_PRESENTATION_FEATURE_NAME = "extendedListPresentation"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public MdExtendedListPresentationEqualsCheck()
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
        builder.title(Messages.MdExtendedListPresentationEqualsCheck_title)
            .description(Messages.MdExtendedListPresentationEqualsCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.UI_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .extension(new SkipAdoptedInExtensionMdObjectExtension());

        builder.topObject(BASIC_DB_OBJECT)
            .checkTop()
            .features(BASIC_DB_OBJECT__LIST_PRESENTATION, BASIC_DB_OBJECT__EXTENDED_LIST_PRESENTATION);
        builder.topObject(INFORMATION_REGISTER)
            .checkTop()
            .features(INFORMATION_REGISTER__LIST_PRESENTATION, INFORMATION_REGISTER__EXTENDED_LIST_PRESENTATION);
        builder.topObject(ACCUMULATION_REGISTER)
            .checkTop()
            .features(ACCUMULATION_REGISTER__LIST_PRESENTATION, ACCUMULATION_REGISTER__EXTENDED_LIST_PRESENTATION);
        builder.topObject(ACCOUNTING_REGISTER)
            .checkTop()
            .features(ACCOUNTING_REGISTER__LIST_PRESENTATION, ACCOUNTING_REGISTER__EXTENDED_LIST_PRESENTATION);
        builder.topObject(CALCULATION_REGISTER)
            .checkTop()
            .features(CALCULATION_REGISTER__LIST_PRESENTATION, CALCULATION_REGISTER__EXTENDED_LIST_PRESENTATION);
        builder.topObject(ENUM)
            .checkTop()
            .features(ENUM__LIST_PRESENTATION, ENUM__EXTENDED_LIST_PRESENTATION);
        builder.topObject(DOCUMENT_JOURNAL)
            .checkTop()
            .features(DOCUMENT_JOURNAL__LIST_PRESENTATION, DOCUMENT_JOURNAL__EXTENDED_LIST_PRESENTATION);
        builder.topObject(FILTER_CRITERION)
            .checkTop()
            .features(FILTER_CRITERION__LIST_PRESENTATION, FILTER_CRITERION__EXTENDED_LIST_PRESENTATION);
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

        EStructuralFeature extendedFeature = eObject.eClass().getEStructuralFeature(EXTENDED_LIST_PRESENTATION_FEATURE_NAME);
        EStructuralFeature listFeature = eObject.eClass().getEStructuralFeature(LIST_PRESENTATION_FEATURE_NAME);
        if (extendedFeature == null || listFeature == null)
        {
            return;
        }

        Object extendedValue = eObject.eGet(extendedFeature);
        Object listValue = eObject.eGet(listFeature);
        if (!(extendedValue instanceof EMap) || !(listValue instanceof EMap))
        {
            return;
        }

        String matched = firstEqualValue((EMap<?, ?>)extendedValue, (EMap<?, ?>)listValue, monitor);
        if (matched != null)
        {
            resultAceptor.addIssue(
                MessageFormat.format(Messages.MdExtendedListPresentationEqualsCheck_message, matched), extendedFeature);
        }
    }

    /**
     * Returns the first value of the extended list presentation that equals
     * the list presentation in the same language, ignoring case and
     * leading/trailing spaces, or {@code null} if the presentations differ
     * in every language where both are specified.
     */
    private static String firstEqualValue(EMap<?, ?> extended, EMap<?, ?> list, IProgressMonitor monitor)
    {
        for (Map.Entry<?, ?> entry : extended.entrySet())
        {
            if (monitor.isCanceled())
            {
                return null;
            }
            String extendedValue = entry.getValue() instanceof String string ? string : null;
            if (extendedValue == null || extendedValue.isBlank())
            {
                continue;
            }
            Object listObject = list.get(entry.getKey());
            String listValue = listObject instanceof String string ? string : null;
            if (listValue == null || listValue.isBlank())
            {
                continue;
            }
            if (extendedValue.strip().equalsIgnoreCase(listValue.strip()))
            {
                return extendedValue.strip();
            }
        }
        return null;
    }
}
