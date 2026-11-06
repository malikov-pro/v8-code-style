/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.md.commonmodule.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.COMMON_MODULE;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT__NAME;

import java.text.MessageFormat;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;

import com._1c.g5.v8.dt.core.platform.IConfigurationProject;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.md.CorePlugin;
import com.e1c.v8codestyle.md.check.SkipAdoptedInExtensionMdObjectExtension;
import com.google.inject.Inject;

/** Checks common module metadata names, including modules without BSL source. */
public final class CommonModuleNameWordsCheck extends BasicCheck
{
    public static final String CHECK_ID = "common-module-name-words"; //$NON-NLS-1$
    public static final String WORDS = "words"; //$NON-NLS-1$
    public static final String DEFAULT_WORDS = "процедуры|procedures|функции|functions|обработчики|handlers" //$NON-NLS-1$
        + "|модуль|module|функциональность|functionality"; //$NON-NLS-1$

    private final IV8ProjectManager projectManager;

    @Inject
    public CommonModuleNameWordsCheck(IV8ProjectManager projectManager)
    {
        this.projectManager = projectManager;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.CommonModuleNameWordsCheck_title)
            .description(Messages.CommonModuleNameWordsCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.TRIVIAL)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .extension(new SkipAdoptedInExtensionMdObjectExtension())
            .topObject(COMMON_MODULE)
            .checkTop()
            .features(MD_OBJECT__NAME)
            .parameter(WORDS, String.class, DEFAULT_WORDS, Messages.CommonModuleNameWordsCheck_words);
    }

    @Override
    protected void check(Object object, ResultAcceptor result, ICheckParameters parameters, IProgressMonitor monitor)
    {
        CommonModule module = (CommonModule)object;
        // Extension projects require their own acceptance matrix before inclusion.
        if (monitor.isCanceled() || !(projectManager.getProject(module) instanceof IConfigurationProject))
        {
            return;
        }
        Pattern pattern;
        try
        {
            pattern = Pattern.compile(parameters.getString(WORDS), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
        catch (PatternSyntaxException e)
        {
            CorePlugin.log(CorePlugin.createWarningStatus(MessageFormat.format(
                Messages.CommonModuleNameWordsCheck_invalid_regex, CHECK_ID, e.getDescription())));
            return;
        }
        String name = module.getName();
        if (name != null && pattern.matcher(name).find())
        {
            result.addIssue(MessageFormat.format(Messages.CommonModuleNameWordsCheck_message, name), MD_OBJECT__NAME);
        }
    }
}
