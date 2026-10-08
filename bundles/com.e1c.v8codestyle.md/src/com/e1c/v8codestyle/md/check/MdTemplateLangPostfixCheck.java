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
 *     malikov-pro - port of the АПК rule АПК_00505
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BASIC_TEMPLATE;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BASIC_TEMPLATE__TEMPLATE_TYPE;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT__NAME;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.dt.core.platform.IDependentProject;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.metadata.mdclass.BasicTemplate;
import com._1c.g5.v8.dt.metadata.mdclass.TemplateType;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.md.CorePlugin;
import com.google.inject.Inject;

/**
 * The name of a template that needs separate preparation for translation must
 * end with a postfix: an underscore and the code of the configuration default
 * language, for example "_ru". Such copies are created per language when
 * interface languages are added, instead of translating the template content.
 * <p>
 * Checked are binary data templates and HTML templates (common templates and
 * templates of applied objects). Other template types are translated without
 * preparatory actions and are not checked. Unlike the source АПК algorithm,
 * which checks only HTML templates containing images, all HTML templates are
 * checked.
 * <p>
 * If the configuration default language is not set, templates are not checked.
 * Objects adopted in extension configurations are not checked.
 *
 * @author malikov-pro
 */
public class MdTemplateLangPostfixCheck
    extends BasicCheck
{

    /** The check id of the АПК_00505 rule port. */
    public static final String CHECK_ID = "apk-00505-template-lang-postfix"; //$NON-NLS-1$

    private static final String UNDERSCORE = "_"; //$NON-NLS-1$

    private final IV8ProjectManager v8ProjectManager;

    /**
     * Instantiates a new check.
     *
     * @param v8ProjectManager the V8 project manager service, cannot be {@code null}.
     */
    @Inject
    public MdTemplateLangPostfixCheck(IV8ProjectManager v8ProjectManager)
    {
        super();
        this.v8ProjectManager = v8ProjectManager;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.MdTemplateLangPostfixCheck_title)
            .description(Messages.MdTemplateLangPostfixCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .extension(new SkipAdoptedInExtensionMdObjectExtension())
            .topObject(MD_OBJECT)
            .checkTop()
            .containment(BASIC_TEMPLATE)
            .features(BASIC_TEMPLATE__TEMPLATE_TYPE, MD_OBJECT__NAME);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (!(object instanceof BasicTemplate template) || monitor.isCanceled())
        {
            return;
        }

        TemplateType templateType = template.getTemplateType();
        if (templateType != TemplateType.BINARY_DATA && templateType != TemplateType.HTML_DOCUMENT)
        {
            return;
        }

        String languageCode = getDefaultLanguageCode(template);
        if (languageCode == null || languageCode.isBlank())
        {
            return;
        }

        String name = template.getName();
        if (name == null || name.isBlank())
        {
            return;
        }

        if (!name.endsWith(UNDERSCORE + languageCode))
        {
            resultAceptor.addIssue(MessageFormat.format(
                Messages.MdTemplateLangPostfixCheck_Template_name_without_language_postfix, name, languageCode));
        }
    }

    private String getDefaultLanguageCode(EObject context)
    {
        IV8Project project = v8ProjectManager.getProject(context);
        if (project == null)
        {
            return null;
        }
        if (project.getDefaultLanguage() == null && project instanceof IDependentProject dependentProject
            && dependentProject.getParent() != null)
        {
            return dependentProject.getParent().getDefaultLanguage()
                .getLanguageCode();
        }
        else if (project.getDefaultLanguage() != null)
        {
            return project.getDefaultLanguage().getLanguageCode();
        }
        return null;
    }
}
