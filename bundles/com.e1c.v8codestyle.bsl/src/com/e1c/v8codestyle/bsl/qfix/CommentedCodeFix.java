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
 *     malikov-pro - quick fix for the ported BSL LS diagnostic CommentedCode
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.qfix;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.validation.Issue;

import com.e1c.g5.v8.dt.bsl.check.qfix.IXtextBslModuleFixModel;
import com.e1c.g5.v8.dt.bsl.check.qfix.SingleVariantXtextBslModuleFix;
import com.e1c.g5.v8.dt.check.qfix.components.QuickFix;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Quick fix для проверки «Закомментированный фрагмент кода»: удаляет
 * закомментированную группу строк целиком (маркер покрывает всю группу),
 * как в BSL Language Server.
 *
 * @author malikov-pro
 */
@QuickFix(checkId = "commented-code", supplierId = BslPlugin.PLUGIN_ID)
public class CommentedCodeFix
    extends SingleVariantXtextBslModuleFix
{

    @Override
    protected void configureFix(FixConfigurer configurer)
    {
        // interactive(false) не поддерживается фреймворком (находка 30.09.2026)
        configurer.interactive(true)
            .description(Messages.CommentedCodeFix_Description)
            .details(Messages.CommentedCodeFix_Details);
    }

    @Override
    protected TextEdit fixIssue(XtextResource state, IXtextBslModuleFixModel model) throws BadLocationException
    {
        Issue issue = model.getIssue();
        if (issue == null || issue.getOffset() == null || issue.getLength() == null || issue.getLength() <= 0)
        {
            return null;
        }
        int offset = issue.getOffset();
        int length = issue.getLength();
        String current = model.getDocument().get(offset, length);
        // на позиции не комментарий (маркер устарел после правок) — фикс недоступен
        return current.startsWith("//") ? new DeleteEdit(offset, length) : null; //$NON-NLS-1$
    }
}
