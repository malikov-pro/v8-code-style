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
 *     malikov-pro - port of the АПК rule АПК_00156
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT;
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
 * The name prefix "Удалить" ("Delete") of an obsolete metadata object and the
 * "(не используется)" synonym prefix must be used together.
 * <p>
 * If an obsolete metadata object cannot be deleted yet, it is renamed with the
 * "Удалить" name prefix and the "(не используется)" prefix is added to its
 * synonym. An object whose name starts with the delete prefix must have
 * a synonym that starts with the "(не используется)" prefix, and vice versa.
 * An object with an empty synonym is not checked.
 * <p>
 * Checked are all metadata objects (top objects, attributes, tabular sections,
 * etc.), except standard attributes, predefined items and standard tabular
 * sections. Objects adopted in extension configurations are not checked.
 *
 * @author malikov-pro
 */
public class MdObjectDeleteSynonymPrefixCheck
    extends BasicCheck
{

    /** The check id of the АПК_00156 rule port. */
    public static final String CHECK_ID = "apk-00156-delete-synonym-prefix"; //$NON-NLS-1$

    private static final String DELETE_PREFIX_RU = "Удалить"; //$NON-NLS-1$
    private static final String DELETE_PREFIX_EN = "Delete"; //$NON-NLS-1$
    private static final String NOT_USED_PREFIX = "(не используется)"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public MdObjectDeleteSynonymPrefixCheck()
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
        builder.title(Messages.MdObjectDeleteSynonymPrefixCheck_title)
            .description(Messages.MdObjectDeleteSynonymPrefixCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .extension(new SkipAdoptedInExtensionMdObjectExtension())
            .topObject(MD_OBJECT)
            .checkTop()
            .containment(MD_OBJECT)
            .features(MD_OBJECT__NAME, MD_OBJECT__SYNONYM);
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

        EMap<String, String> synonym = mdObject.getSynonym();
        String synonymValue = firstNotBlank(synonym);
        if (synonymValue == null)
        {
            // Empty synonym is out of the scope of this check, as in the source АПК algorithm.
            return;
        }

        String name = mdObject.getName() == null ? "" : mdObject.getName().strip(); //$NON-NLS-1$
        boolean nameStartsWithDelete = startsWithIgnoreCase(name, DELETE_PREFIX_RU, DELETE_PREFIX_EN);
        boolean synonymStartsWithNotUsed = startsWithIgnoreCase(synonymValue.strip(), NOT_USED_PREFIX);

        if (nameStartsWithDelete && !synonymStartsWithNotUsed)
        {
            resultAceptor.addIssue(MessageFormat.format(
                Messages.MdObjectDeleteSynonymPrefixCheck_Name_prefix_without_synonym_prefix, name, synonymValue
                    .strip()));
        }
        else if (synonymStartsWithNotUsed && !nameStartsWithDelete)
        {
            resultAceptor.addIssue(MessageFormat.format(
                Messages.MdObjectDeleteSynonymPrefixCheck_Synonym_prefix_without_name_prefix, name, synonymValue
                    .strip()));
        }
    }

    private static String firstNotBlank(EMap<String, String> synonym)
    {
        if (synonym == null)
        {
            return null;
        }
        for (Map.Entry<String, String> entry : synonym.entrySet())
        {
            String value = entry.getValue();
            if (value != null && !value.isBlank())
            {
                return value;
            }
        }
        return null;
    }

    private static boolean startsWithIgnoreCase(String value, String... prefixes)
    {
        for (String prefix : prefixes)
        {
            if (value.regionMatches(true, 0, prefix, 0, prefix.length()))
            {
                return true;
            }
        }
        return false;
    }
}
