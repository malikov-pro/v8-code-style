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
 *     malikov-pro - port of the АПК rule АПК_00139
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.TYPE_DESCRIPTION;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.TYPE_DESCRIPTION__STRING_QUALIFIERS;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.TYPE_DESCRIPTION__TYPES;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BASIC_DB_OBJECT;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;

import com._1c.g5.v8.dt.mcore.StringQualifiers;
import com._1c.g5.v8.dt.mcore.TypeDescription;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.mcore.util.McoreUtil;
import com._1c.g5.v8.dt.metadata.mdclass.BasicFeature;
import com._1c.g5.v8.dt.platform.IEObjectTypeNames;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.md.CorePlugin;

/**
 * The check that string attribute of DB object has fixed allowed length.
 * <p>
 * For string attributes use variable length (allowed length property is
 * VARIABLE) and specify the maximum length. Fixed length strings pad values
 * with trailing spaces and should be used only when the guarantee of the
 * exact string length is really needed.
 * <p>
 * Attributes adopted in extension configurations are not checked.
 * Attributes typed with a defined type are not resolved to the defined
 * type qualifiers (as in the source АПК algorithm).
 *
 * @author malikov-pro
 */
public class DbObjectStringAllowedLengthCheck
    extends BasicCheck
{

    /** The check id of the АПК_00139 rule port. */
    public static final String CHECK_ID = "apk-00139-string-attr-length"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public DbObjectStringAllowedLengthCheck()
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
        builder.title(Messages.DbObjectStringAllowedLengthCheck_title)
            .description(Messages.DbObjectStringAllowedLengthCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.PERFORMANCE)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .extension(new SkipAdoptedInExtensionMdObjectExtension())
            .topObject(BASIC_DB_OBJECT)
            .containment(TYPE_DESCRIPTION)
            .features(TYPE_DESCRIPTION__TYPES, TYPE_DESCRIPTION__STRING_QUALIFIERS);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        TypeDescription typeDescription = (TypeDescription)object;
        if (!(typeDescription.eContainer() instanceof BasicFeature))
        {
            return;
        }

        StringQualifiers qualifiers = typeDescription.getStringQualifiers();
        if (qualifiers == null || !qualifiers.isFixed() || !hasStringType(typeDescription, monitor))
        {
            return;
        }

        String attributeName = ((BasicFeature)typeDescription.eContainer()).getName();
        resultAceptor.addIssue(MessageFormat.format(Messages.DbObjectStringAllowedLengthCheck_message, attributeName),
            typeDescription, TYPE_DESCRIPTION__TYPES);
    }

    private boolean hasStringType(TypeDescription typeDescription, IProgressMonitor monitor)
    {
        for (TypeItem typeItem : typeDescription.getTypes())
        {
            if (monitor.isCanceled())
            {
                return false;
            }
            if (IEObjectTypeNames.STRING.equals(McoreUtil.getTypeName(typeItem)))
            {
                return true;
            }
        }
        return false;
    }
}
