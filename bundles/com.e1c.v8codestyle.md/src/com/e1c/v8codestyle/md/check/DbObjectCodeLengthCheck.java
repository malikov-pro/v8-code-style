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
 *     malikov-pro - port of the APK rule 00137
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BUSINESS_PROCESS;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BUSINESS_PROCESS__NUMBER_LENGTH;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.CATALOG;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.CATALOG__CODE_LENGTH;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.DOCUMENT;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.DOCUMENT__NUMBER_LENGTH;

import java.text.MessageFormat;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EStructuralFeature;

import com._1c.g5.v8.dt.metadata.mdclass.BusinessProcess;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.Document;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.md.CorePlugin;

/**
 * Check that the code (number) length of a configuration object is 0, 3, 5, 9, or 11.
 * <p>
 * Zero means that coding (numbering) is not used for the object. Lengths 3, 5, 9, 11
 * are recommended (but not mandatory) for typical configurations. When numbering,
 * consider the length of numbering prefixes (infobase prefix, organization prefix, etc.).
 * <p>
 * The check covers {@link Catalog#getCodeLength()} and, for objects that have numbering
 * instead of coding, {@link Document#getNumberLength()} and
 * {@link BusinessProcess#getNumberLength()} — both properties are checked by the same
 * rule (see APK 00137/00138, v8std 473).
 *
 * @author malikov-pro
 */
public class DbObjectCodeLengthCheck
    extends BasicCheck
{

    /** The check id of the APK rule 00137. */
    public static final String CHECK_ID = "apk-00137-code-length"; //$NON-NLS-1$

    private static final Set<Integer> ALLOWED_LENGTHS = Set.of(0, 3, 5, 9, 11);

    /**
     * Instantiates a new check.
     */
    public DbObjectCodeLengthCheck()
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
        builder.title(Messages.DbObjectCodeLengthCheck_title)
            .description(Messages.DbObjectCodeLengthCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.PORTABILITY)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .extension(new SkipAdoptedInExtensionMdObjectExtension());

        builder.topObject(CATALOG)
            .checkTop()
            .features(CATALOG__CODE_LENGTH);

        builder.topObject(DOCUMENT)
            .checkTop()
            .features(DOCUMENT__NUMBER_LENGTH);

        builder.topObject(BUSINESS_PROCESS)
            .checkTop()
            .features(BUSINESS_PROCESS__NUMBER_LENGTH);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        EStructuralFeature feature;
        int length;
        if (object instanceof Catalog catalog)
        {
            length = catalog.getCodeLength();
            feature = CATALOG__CODE_LENGTH;
        }
        else if (object instanceof Document document)
        {
            length = document.getNumberLength();
            feature = DOCUMENT__NUMBER_LENGTH;
        }
        else if (object instanceof BusinessProcess businessProcess)
        {
            length = businessProcess.getNumberLength();
            feature = BUSINESS_PROCESS__NUMBER_LENGTH;
        }
        else
        {
            return;
        }

        if (!ALLOWED_LENGTHS.contains(length))
        {
            String name = object instanceof Catalog catalog ? catalog.getName()
                : object instanceof Document document ? document.getName()
                    : object instanceof BusinessProcess businessProcess ? businessProcess.getName()
                        : ""; //$NON-NLS-1$
            String message = MessageFormat.format(Messages.DbObjectCodeLengthCheck_message, name, length);
            resultAceptor.addIssue(message, feature);
        }
    }
}
