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
 *     malikov-pro - port of the APK check АПК_01194
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.qfix;

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
 * Quick fix для проверки «Французские кавычки не допускаются в интерфейсных
 * текстах»: заменяет ТОЛЬКО кавычку « или », на которую указано замечание,
 * на прямую двойную кавычку. Внутри строкового литерала BSL прямая кавычка
 * экранируется удвоением ({@code ""}). Перенос АПК_01194.
 *
 * @author malikov-pro
 */
@QuickFix(checkId = "apk-01194-no-french-quotes", supplierId = BslPlugin.PLUGIN_ID)
public class ApkFrenchQuotesFix
    extends SingleVariantXtextBslModuleFix
{

    /**
     * Instantiates a new fix.
     */
    public ApkFrenchQuotesFix()
    {
        super();
    }

    @Override
    protected void configureFix(FixConfigurer configurer)
    {
        // interactive(false) не поддерживается фреймворком (см. ApkYoLetterFix)
        configurer.interactive(true)
            .description(Messages.ApkFrenchQuotesFix_Description)
            .details(Messages.ApkFrenchQuotesFix_Details);
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
        String current = model.getDocument().get(offset, length);
        if ("«".equals(current) || "»".equals(current))
        {
            // прямая кавычка внутри строкового литерала BSL экранируется удвоением
            return new ReplaceEdit(offset, length, "\"\"");
        }
        // на позиции уже не «ёлочка» (маркер устарел после правок) — фикс недоступен
        return null;
    }
}
