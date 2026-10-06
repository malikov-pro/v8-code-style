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
 *     malikov-pro - port of the АПК rule АПК_00126
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT__COMMENT;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT__NAME;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT__SYNONYM;

import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EMap;

import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.md.CorePlugin;

/**
 * The letter "ё" (in any case) is not allowed in the name, synonym and comment
 * of a metadata object. The issue message tells which property contains the
 * letter; the issue is placed on the object itself.
 * <p>
 * Checked are all metadata objects (top objects, attributes, tabular sections,
 * etc.) in every synonym language, except standard attributes, predefined
 * items and standard tabular sections. Objects adopted in extension
 * configurations are not checked.
 * <p>
 * For the letter "ё" in module texts see the check {@code apk-00260-no-yo-letter}
 * (the BSL channel).
 *
 * @author malikov-pro
 */
public class MdObjectYoLetterCheck
    extends BasicCheck
{

    /** The check id of the АПК_00126 rule port. */
    public static final String CHECK_ID = "apk-00126-md-no-yo-letter"; //$NON-NLS-1$

    private static final String YO_LOWER = "ё"; //$NON-NLS-1$
    private static final String YO_UPPER = "Ё"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public MdObjectYoLetterCheck()
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
        builder.title(Messages.MdObjectYoLetterCheck_title)
            .description(Messages.MdObjectYoLetterCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.UI_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .extension(new SkipAdoptedInExtensionMdObjectExtension())
            .topObject(MD_OBJECT)
            .checkTop()
            .containment(MD_OBJECT)
            .features(MD_OBJECT__NAME, MD_OBJECT__SYNONYM, MD_OBJECT__COMMENT);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        MdObject mdObject = (MdObject)object;
        if (monitor.isCanceled())
        {
            return;
        }

        String name = mdObject.getName();
        if (containsYoLetter(name))
        {
            resultAceptor.addIssue(MessageFormat.format(Messages.MdObjectYoLetterCheck_Name_contains_yo, name));
        }

        if (hasYoLetterInSynonym(mdObject.getSynonym()))
        {
            resultAceptor.addIssue(MessageFormat.format(Messages.MdObjectYoLetterCheck_Synonym_contains_yo, name));
        }

        if (containsYoLetter(mdObject.getComment()))
        {
            resultAceptor.addIssue(MessageFormat.format(Messages.MdObjectYoLetterCheck_Comment_contains_yo, name));
        }
    }

    private static boolean hasYoLetterInSynonym(EMap<String, String> synonym)
    {
        if (synonym == null)
        {
            return false;
        }
        for (Map.Entry<String, String> entry : synonym.entrySet())
        {
            if (containsYoLetter(entry.getValue()))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean containsYoLetter(String value)
    {
        if (value == null)
        {
            return false;
        }
        return value.indexOf(YO_LOWER) >= 0 || value.indexOf(YO_UPPER) >= 0;
    }
}
