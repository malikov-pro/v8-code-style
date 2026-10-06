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
 *     malikov-pro - quick fix for the ported BSL LS diagnostic UselessTernaryOperator
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.qfix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.validation.Issue;

import com.e1c.g5.v8.dt.bsl.check.qfix.IXtextBslModuleFixModel;
import com.e1c.g5.v8.dt.bsl.check.qfix.SingleVariantXtextBslModuleFix;
import com.e1c.g5.v8.dt.check.qfix.components.QuickFix;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Quick fix для проверки «Бесполезный тернарный оператор»: упрощает
 * {@code ?(Условие, Истина, Ложь)} до {@code Условие}, а
 * {@code ?(Условие, Ложь, Истина)} — до {@code НЕ (Условие)}. Случаи
 * «условие — константа» и «ветки одинаковы» механически не упрощаются —
 * там решение за пользователем.
 *
 * @author malikov-pro
 */
@QuickFix(checkId = "useless-ternary-operator", supplierId = BslPlugin.PLUGIN_ID)
public class UselessTernaryFix
    extends SingleVariantXtextBslModuleFix
{

    private static final String TRUE_RU = "истина"; //$NON-NLS-1$
    private static final String TRUE_EN = "true"; //$NON-NLS-1$
    private static final String FALSE_RU = "ложь"; //$NON-NLS-1$
    private static final String FALSE_EN = "false"; //$NON-NLS-1$

    @Override
    protected void configureFix(FixConfigurer configurer)
    {
        // interactive(false) не поддерживается фреймворком (находка 30.09.2026)
        configurer.interactive(true)
            .description(Messages.UselessTernaryFix_Description)
            .details(Messages.UselessTernaryFix_Details);
    }

    @Override
    protected TextEdit fixIssue(XtextResource state, IXtextBslModuleFixModel model) throws BadLocationException
    {
        Issue issue = model.getIssue();
        if (issue == null || issue.getOffset() == null || issue.getLength() == null)
        {
            return null;
        }
        int offset = issue.getOffset();
        int length = Math.max(1, issue.getLength());
        String text = model.getDocument().get(offset, length);
        if (!text.startsWith("?(") || !text.endsWith(")")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            // на позиции уже не тернарник (маркер устарел после правок)
            return null;
        }
        String inner = text.substring(2, text.length() - 1);
        List<String> parts = splitTopLevel(inner);
        if (parts.size() != 3)
        {
            return null;
        }
        String condition = parts.get(0).trim();
        String trueBranch = parts.get(1).trim().toLowerCase();
        String falseBranch = parts.get(2).trim().toLowerCase();

        if (isTrue(trueBranch) && isFalse(falseBranch))
        {
            return new ReplaceEdit(offset, length, condition);
        }
        if (isFalse(trueBranch) && isTrue(falseBranch))
        {
            return new ReplaceEdit(offset, length, "НЕ (" + condition + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        // условие-константа либо одинаковые ветки — механически не упрощается
        return null;
    }

    private static boolean isTrue(String text)
    {
        return TRUE_RU.equals(text) || TRUE_EN.equals(text);
    }

    private static boolean isFalse(String text)
    {
        return FALSE_RU.equals(text) || FALSE_EN.equals(text);
    }

    /**
     * Делит текст на части по запятым верхнего уровня: учитывается вложенность
     * круглых/квадратных скобок и строковые литералы (с экранированием "").
     *
     * @param text текст, не может быть {@code null}.
     * @return список частей, не пустой.
     */
    private static List<String> splitTopLevel(String text)
    {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inString = false;
        for (int i = 0; i < text.length(); i++)
        {
            char c = text.charAt(i);
            if (inString)
            {
                current.append(c);
                if (c == '"')
                {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"')
                    {
                        current.append('"');
                        i++;
                    }
                    else
                    {
                        inString = false;
                    }
                }
                continue;
            }
            if (c == '"')
            {
                inString = true;
            }
            else if (c == '(' || c == '[')
            {
                depth++;
            }
            else if (c == ')' || c == ']')
            {
                depth--;
            }
            else if (c == ',' && depth == 0)
            {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        parts.add(current.toString());
        return parts;
    }
}
