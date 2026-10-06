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
 *     malikov-pro - port of the АПК rule АПК_01195
 *******************************************************************************/
package com.e1c.v8codestyle.form.check;

import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.ABSTRACT_FORM_ATTRIBUTE;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.FORM;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.FORM_COMMAND;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.FORM_COMMAND__TOOL_TIP;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.FORM_ITEM;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.TITLED__TITLE;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.TOOLTIP_CONTAINER__TOOL_TIP;

import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.EStructuralFeature;

import com._1c.g5.v8.dt.form.model.AbstractFormAttribute;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormCommand;
import com._1c.g5.v8.dt.form.model.FormItem;
import com._1c.g5.v8.dt.form.model.Titled;
import com._1c.g5.v8.dt.form.model.TooltipContainer;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.form.CorePlugin;

/**
 * The check that interface texts of a form (the form title, titles and tooltips
 * of form items, titles of form attributes, titles and tooltips of form commands)
 * do not contain French quotes (guillemets). Straight double quotes should be
 * used in interface texts; French quotes are allowed only in help texts.
 * <p>
 * The issue message contains the path to the property, as in the source АПК
 * algorithm: "Form.Title", "Items.Name.Title", "Attributes.Name.Title",
 * "Commands.Name.Title" (localized). One issue is added per property; all
 * language variants of the multi-language properties are inspected. Form
 * parameters are not checked (they have no interface texts), names of items,
 * attributes and commands are not checked (as in the source algorithm).
 *
 * @author malikov-pro
 */
public class FormItemFrenchQuotesCheck
    extends BasicCheck
{

    /** The check id of the АПК_01195 rule port. */
    public static final String CHECK_ID = "apk-01195-french-quotes-form"; //$NON-NLS-1$

    private static final char QUOTE_LEFT = '«';
    private static final char QUOTE_RIGHT = '»';

    private static final String DOT = "."; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public FormItemFrenchQuotesCheck()
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
        builder.title(Messages.FormItemFrenchQuotesCheck_title)
            .description(Messages.FormItemFrenchQuotesCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.TRIVIAL)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID));

        builder.topObject(FORM)
            .checkTop()
            .features(TITLED__TITLE)
            .containment(FORM_ITEM)
            .features(TITLED__TITLE, TOOLTIP_CONTAINER__TOOL_TIP)
            .containment(ABSTRACT_FORM_ATTRIBUTE)
            .features(TITLED__TITLE)
            .containment(FORM_COMMAND)
            .features(TITLED__TITLE, FORM_COMMAND__TOOL_TIP);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (monitor.isCanceled())
        {
            return;
        }

        if (object instanceof Form form)
        {
            checkValues(form.getTitle(), Messages.FormItemFrenchQuotesCheck_Form, null,
                Messages.FormItemFrenchQuotesCheck_Title, TITLED__TITLE, resultAceptor, monitor);
        }
        else if (object instanceof FormItem item)
        {
            String name = item.getName();
            if (item instanceof Titled titled)
            {
                checkValues(titled.getTitle(), Messages.FormItemFrenchQuotesCheck_Items, name,
                    Messages.FormItemFrenchQuotesCheck_Title, TITLED__TITLE, resultAceptor, monitor);
            }
            if (item instanceof TooltipContainer container)
            {
                checkValues(container.getToolTip(), Messages.FormItemFrenchQuotesCheck_Items, name,
                    Messages.FormItemFrenchQuotesCheck_Tooltip, TOOLTIP_CONTAINER__TOOL_TIP, resultAceptor, monitor);
            }
        }
        else if (object instanceof AbstractFormAttribute attribute)
        {
            checkValues(attribute.getTitle(), Messages.FormItemFrenchQuotesCheck_Attributes, attribute.getName(),
                Messages.FormItemFrenchQuotesCheck_Title, TITLED__TITLE, resultAceptor, monitor);
        }
        else if (object instanceof FormCommand command)
        {
            String name = command.getName();
            checkValues(command.getTitle(), Messages.FormItemFrenchQuotesCheck_Commands, name,
                Messages.FormItemFrenchQuotesCheck_Title, TITLED__TITLE, resultAceptor, monitor);
            checkValues(command.getToolTip(), Messages.FormItemFrenchQuotesCheck_Commands, name,
                Messages.FormItemFrenchQuotesCheck_Tooltip, FORM_COMMAND__TOOL_TIP, resultAceptor, monitor);
        }
    }

    private void checkValues(EMap<String, String> values, String container, String name, String property,
        EStructuralFeature feature, ResultAcceptor resultAceptor, IProgressMonitor monitor)
    {
        if (values == null || values.isEmpty())
        {
            return;
        }
        for (Map.Entry<String, String> entry : values.entrySet())
        {
            if (monitor.isCanceled())
            {
                return;
            }
            String value = entry.getValue();
            if (value != null && !value.isBlank()
                && (value.indexOf(QUOTE_LEFT) >= 0 || value.indexOf(QUOTE_RIGHT) >= 0))
            {
                String path = name == null || name.isBlank()
                    ? MessageFormat.format("{0}{1}{2}", container, DOT, property) //$NON-NLS-1$
                    : buildPath(container, name, property);
                resultAceptor.addIssue(
                    MessageFormat.format(Messages.FormItemFrenchQuotesCheck_message, path), feature);
                return;
            }
        }
    }

    private String buildPath(String container, String name, String property)
    {
        return MessageFormat.format("{0}{1}{2}{3}{4}", container, DOT, name, DOT, property); //$NON-NLS-1$
    }
}
