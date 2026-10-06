/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: 1C-Soft LLC
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the BSL Language Server diagnostic LineLength
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Module;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.Issue;
import com.e1c.g5.v8.dt.check.WrongParameterException;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: строки модуля не длиннее допустимого максимума
 * (по умолчанию 120 символов, настраивается параметром).
 * <p>
 * Упрощённый перенос диагностики BSL Language Server LineLength
 * (тип CODE_SMELL, серьёзность MINOR).
 *
 * @author malikov-pro
 */
public class LineLengthCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "line-length"; //$NON-NLS-1$

    private static final String PARAM_MAX_LINE_LENGTH = "maxLineLength"; //$NON-NLS-1$

    private static final Integer DEFAULT_MAX_LINE_LENGTH = Integer.valueOf(120);

    /**
     * Instantiates a new check.
     */
    public LineLengthCheck()
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
        builder.title(Messages.LineLengthCheck_title)
            .description(Messages.LineLengthCheck_description)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE)
            .parameter(PARAM_MAX_LINE_LENGTH, Integer.class, DEFAULT_MAX_LINE_LENGTH.toString(),
                Messages.LineLengthCheck_Maximum_line_length);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Module module = (Module)object;
        int maxLineLength;
        try
        {
            maxLineLength = parameters.getInt(PARAM_MAX_LINE_LENGTH);
        }
        catch (WrongParameterException e)
        {
            maxLineLength = DEFAULT_MAX_LINE_LENGTH.intValue();
        }

        String text = NodeModelUtils.findActualNodeFor(module).getText();
        int line = 1;
        int lineStart = 0;
        int index = 0;
        while (index <= text.length())
        {
            int newLinePos = text.indexOf('\n', index);
            int end = newLinePos < 0 ? text.length() : newLinePos;
            int length = end - lineStart;
            String lineText = text.substring(lineStart, end);
            if (lineText.endsWith("\r")) //$NON-NLS-1$
            {
                length--;
            }
            if (length > maxLineLength)
            {
                String message = MessageFormat.format(Messages.LineLengthCheck_Line_is_too_long, line, length,
                    maxLineLength);
                Issue issue = new BslDirectLocationIssue(message, new DirectLocation(lineStart, length, line, module));
                resultAceptor.addIssue(issue);
            }
            if (newLinePos < 0)
            {
                break;
            }
            lineStart = newLinePos + 1;
            index = lineStart;
            line++;
        }
    }
}
