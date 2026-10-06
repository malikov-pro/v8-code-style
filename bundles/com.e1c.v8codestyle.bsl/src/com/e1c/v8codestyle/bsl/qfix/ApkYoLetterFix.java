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
 *     malikov-pro - port of the APK check АПК_00260
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
 * Quick fix для проверки «Буква ё не допускается в текстах модулей»:
 * заменяет ТОЛЬКО вхождение буквы «ё»/«Ё», на которое указано замечание.
 * Точечно, потому что в комментариях и строковых литералах «ё» может быть
 * осмысленной (имена собственные, цитаты). Перенос АПК_00260.
 *
 * @author malikov-pro
 */
@QuickFix(checkId = "apk-00260-no-yo-letter", supplierId = BslPlugin.PLUGIN_ID)
public class ApkYoLetterFix
    extends SingleVariantXtextBslModuleFix
{

    @Override
    protected void configureFix(FixConfigurer configurer)
    {
        // interactive(false) не поддерживается фреймворком: FixConfigurer
        // бросает IllegalArgumentException уже при регистрации фикса,
        // что роняет старт проектного контекста (находка 30.09.2026).
        configurer.interactive(true)
            .description(Messages.ApkYoLetterFix_Description)
            .details(Messages.ApkYoLetterFix_Details);
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
        if ("ё".equals(current))
        {
            return new ReplaceEdit(offset, length, "е");
        }
        if ("Ё".equals(current))
        {
            return new ReplaceEdit(offset, length, "Е");
        }
        // на позиции уже не «ё» (маркер устарел после правок) — фикс недоступен
        return null;
    }
}
