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
 *     malikov-pro - port of the АПК rule АПК_01196
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BASIC_COMMAND__TOOL_TIP;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BASIC_FEATURE__TOOL_TIP;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BASIC_TABULAR_SECTION__TOOL_TIP;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.COMMAND_GROUP__TOOL_TIP;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.CONSTANT__TOOL_TIP;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.FIELD__TOOL_TIP;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT__COMMENT;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT__SYNONYM;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.STANDARD_ATTRIBUTE;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.STANDARD_ATTRIBUTE__COMMENT;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.STANDARD_ATTRIBUTE__SYNONYM;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.STANDARD_ATTRIBUTE__TOOL_TIP;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.STANDARD_TABULAR_SECTION_DESCRIPTION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.STANDARD_TABULAR_SECTION_DESCRIPTION__COMMENT;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.STANDARD_TABULAR_SECTION_DESCRIPTION__SYNONYM;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.STANDARD_TABULAR_SECTION_DESCRIPTION__TOOL_TIP;

import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com._1c.g5.v8.dt.metadata.mdclass.StandardAttribute;
import com._1c.g5.v8.dt.metadata.mdclass.StandardTabularSectionDescription;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.md.CorePlugin;

/**
 * The check that interface texts of metadata objects (synonym, comment, tooltip)
 * do not contain French quotes (guillemets). Straight double quotes should be used
 * in interface texts; French quotes are allowed only in help texts.
 * <p>
 * The source АПК rule is universal: for each metadata object it inspects the
 * properties Синоним, Комментарий and Подсказка (if the object has them).
 * This port checks all metadata objects (both top objects and contained ones —
 * attributes, tabular sections, commands, etc.), standard attributes and standard
 * tabular section descriptions. The tooltip property exists only for a part of
 * the classes (attributes, tabular sections, commands, constants, etc.) and is
 * checked when the object class has it. One issue is added per property, as in
 * the source algorithm; all language variants of the multi-language properties
 * are inspected.
 *
 * @author malikov-pro
 */
public class MdObjectFrenchQuotesCheck
    extends BasicCheck
{

    /** The check id of the АПК_01196 rule port. */
    public static final String CHECK_ID = "apk-01196-french-quotes-md"; //$NON-NLS-1$

    private static final char QUOTE_LEFT = '«';
    private static final char QUOTE_RIGHT = '»';

    private static final String TOOLTIP_FEATURE_NAME = "toolTip"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public MdObjectFrenchQuotesCheck()
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
        builder.title(Messages.MdObjectFrenchQuotesCheck_title)
            .description(Messages.MdObjectFrenchQuotesCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.TRIVIAL)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .extension(new SkipAdoptedInExtensionMdObjectExtension());

        builder.topObject(MD_OBJECT)
            .checkTop()
            .features(MD_OBJECT__SYNONYM, MD_OBJECT__COMMENT, CONSTANT__TOOL_TIP)
            .containment(MD_OBJECT)
            .features(MD_OBJECT__SYNONYM, MD_OBJECT__COMMENT, BASIC_COMMAND__TOOL_TIP, BASIC_FEATURE__TOOL_TIP,
                BASIC_TABULAR_SECTION__TOOL_TIP, COMMAND_GROUP__TOOL_TIP)
            .containment(STANDARD_ATTRIBUTE)
            .features(STANDARD_ATTRIBUTE__SYNONYM, STANDARD_ATTRIBUTE__COMMENT, STANDARD_ATTRIBUTE__TOOL_TIP)
            .containment(STANDARD_TABULAR_SECTION_DESCRIPTION)
            .features(STANDARD_TABULAR_SECTION_DESCRIPTION__SYNONYM, STANDARD_TABULAR_SECTION_DESCRIPTION__COMMENT,
                STANDARD_TABULAR_SECTION_DESCRIPTION__TOOL_TIP);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (monitor.isCanceled())
        {
            return;
        }

        if (object instanceof MdObject)
        {
            checkSynonym(((MdObject)object).getSynonym(), MD_OBJECT__SYNONYM, resultAceptor, monitor);
            checkComment(((MdObject)object).getComment(), MD_OBJECT__COMMENT, resultAceptor, monitor);
            checkToolTip(object, resultAceptor, monitor);
        }
        else if (object instanceof StandardAttribute)
        {
            checkSynonym(((StandardAttribute)object).getSynonym(), STANDARD_ATTRIBUTE__SYNONYM, resultAceptor, monitor);
            checkComment(((StandardAttribute)object).getComment(), STANDARD_ATTRIBUTE__COMMENT, resultAceptor, monitor);
            checkToolTip(object, resultAceptor, monitor);
        }
        else if (object instanceof StandardTabularSectionDescription)
        {
            checkSynonym(((StandardTabularSectionDescription)object).getSynonym(),
                STANDARD_TABULAR_SECTION_DESCRIPTION__SYNONYM, resultAceptor, monitor);
            checkComment(((StandardTabularSectionDescription)object).getComment(),
                STANDARD_TABULAR_SECTION_DESCRIPTION__COMMENT, resultAceptor, monitor);
            checkToolTip(object, resultAceptor, monitor);
        }
    }

    private void checkSynonym(EMap<String, String> synonym, EStructuralFeature feature, ResultAcceptor resultAceptor,
        IProgressMonitor monitor)
    {
        if (synonym == null)
        {
            return;
        }
        for (Map.Entry<String, String> entry : synonym.entrySet())
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (containsFrenchQuotes(entry.getValue()))
            {
                resultAceptor.addIssue(
                    MessageFormat.format(Messages.MdObjectFrenchQuotesCheck_message,
                        Messages.MdObjectFrenchQuotesCheck_Synonym), feature);
                return;
            }
        }
    }

    private void checkComment(String comment, EStructuralFeature feature, ResultAcceptor resultAceptor,
        IProgressMonitor monitor)
    {
        if (monitor.isCanceled() || !containsFrenchQuotes(comment))
        {
            return;
        }
        resultAceptor.addIssue(MessageFormat.format(Messages.MdObjectFrenchQuotesCheck_message,
            Messages.MdObjectFrenchQuotesCheck_Comment), feature);
    }

    private void checkToolTip(Object object, ResultAcceptor resultAceptor, IProgressMonitor monitor)
    {
        EObject eObject = (EObject)object;
        EStructuralFeature feature = eObject.eClass().getEStructuralFeature(TOOLTIP_FEATURE_NAME);
        if (feature == null)
        {
            return;
        }
        Object value = eObject.eGet(feature);
        if (!(value instanceof EMap))
        {
            return;
        }
        for (Map.Entry<?, ?> entry : ((EMap<?, ?>)value).entrySet())
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (containsFrenchQuotes(entry.getValue() instanceof String string ? string : null))
            {
                resultAceptor.addIssue(MessageFormat.format(Messages.MdObjectFrenchQuotesCheck_message,
                    Messages.MdObjectFrenchQuotesCheck_Tooltip), feature);
                return;
            }
        }
    }

    private boolean containsFrenchQuotes(String text)
    {
        if (text == null || text.isBlank())
        {
            return false;
        }
        return text.indexOf(QUOTE_LEFT) >= 0 || text.indexOf(QUOTE_RIGHT) >= 0;
    }
}
