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
 *     malikov-pro - quick fix for the ported BSL LS diagnostic OneStatementPerLine
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.qfix;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.validation.Issue;

import com.e1c.g5.v8.dt.bsl.check.qfix.IXtextBslModuleFixModel;
import com.e1c.g5.v8.dt.bsl.check.qfix.SingleVariantXtextBslModuleFix;
import com.e1c.g5.v8.dt.check.qfix.components.QuickFix;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Quick fix для проверки «Одно выражение в одной строке»: переносит
 * оператор, на который указывает замечание, на новую строку с отступом
 * исходной строки. Для ПЕРВОГО оператора строки фикс недоступен — переносить
 * некуда (оператор уже в начале строки).
 *
 * @author malikov-pro
 */
@QuickFix(checkId = "one-statement-per-line", supplierId = BslPlugin.PLUGIN_ID)
public class OneStatementPerLineFix
    extends SingleVariantXtextBslModuleFix
{

    @Override
    protected void configureFix(FixConfigurer configurer)
    {
        // interactive(false) не поддерживается фреймворком (находка 30.09.2026)
        configurer.interactive(true)
            .description(Messages.OneStatementPerLineFix_Description)
            .details(Messages.OneStatementPerLineFix_Details);
    }

    @Override
    protected TextEdit fixIssue(XtextResource state, IXtextBslModuleFixModel model) throws BadLocationException
    {
        Issue issue = model.getIssue();
        if (issue == null || issue.getOffset() == null)
        {
            return null;
        }
        int offset = issue.getOffset();
        IDocument document = model.getDocument();

        int line = document.getLineOfOffset(offset);
        int lineStart = document.getLineOffset(line);

        if (isFirstStatementOnLine(document, lineStart, offset))
        {
            // первый оператор строки — переносить некуда
            return null;
        }
        String indent = leadingWhitespace(document, lineStart);
        return new InsertEdit(offset, TextUtilities.getDefaultLineDelimiter(document) + indent);
    }

    /**
     * Между началом строки и оператором — только пробелы/табы (и, возможно,
     * «;» от предыдущего оператора): оператор первый содержательный в строке
     * после точки с запятой. Фикс предлагается для операторов, идущих после
     * другого оператора в той же строке.
     */
    private static boolean isFirstStatementOnLine(IDocument document, int lineStart, int offset)
        throws BadLocationException
    {
        for (int i = lineStart; i < offset; i++)
        {
            char c = document.getChar(i);
            if (c != ' ' && c != '\t' && c != ';')
            {
                return false;
            }
        }
        return true;
    }

    private static String leadingWhitespace(IDocument document, int lineStart) throws BadLocationException
    {
        int i = lineStart;
        int length = document.getLength();
        while (i < length)
        {
            char c = document.getChar(i);
            if (c != ' ' && c != '\t')
            {
                break;
            }
            i++;
        }
        return document.get(lineStart, i - lineStart);
    }
}
