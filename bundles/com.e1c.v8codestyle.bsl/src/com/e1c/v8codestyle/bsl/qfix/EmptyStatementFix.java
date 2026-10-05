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
 *     malikov-pro - quick fix for the ported BSL LS diagnostic EmptyStatement
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
 * Quick fix для проверки «Пустой оператор»: удаляет лишнюю точку с запятой,
 * на которую указывает замечание. Замена механическая — семантика модуля
 * не меняется.
 *
 * @author malikov-pro
 */
@QuickFix(checkId = "empty-statement", supplierId = BslPlugin.PLUGIN_ID)
public class EmptyStatementFix
    extends SingleVariantXtextBslModuleFix
{

    @Override
    protected void configureFix(FixConfigurer configurer)
    {
        // interactive(false) не поддерживается фреймворком (находка 30.09.2026)
        configurer.interactive(true)
            .description(Messages.EmptyStatementFix_Description)
            .details(Messages.EmptyStatementFix_Details);
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
        if (";".equals(current)) //$NON-NLS-1$
        {
            return new DeleteEdit(offset, length);
        }
        // на позиции уже не «;» (маркер устарел после правок) — фикс недоступен
        return null;
    }
}
