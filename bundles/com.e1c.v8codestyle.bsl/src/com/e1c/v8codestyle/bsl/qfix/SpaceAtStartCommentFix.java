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
 *     malikov-pro - quick fix for the ported BSL LS diagnostic SpaceAtStartComment
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.qfix;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.validation.Issue;

import com.e1c.g5.v8.dt.bsl.check.qfix.IXtextBslModuleFixModel;
import com.e1c.g5.v8.dt.bsl.check.qfix.SingleVariantXtextBslModuleFix;
import com.e1c.g5.v8.dt.check.qfix.components.QuickFix;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Quick fix для проверки «Пробел в начале комментария»: вставляет пробел
 * сразу после «//», как в BSL Language Server.
 *
 * @author malikov-pro
 */
@QuickFix(checkId = "space-at-start-comment", supplierId = BslPlugin.PLUGIN_ID)
public class SpaceAtStartCommentFix
    extends SingleVariantXtextBslModuleFix
{

    private static final int COMMENT_LENGTH = 2;

    @Override
    protected void configureFix(FixConfigurer configurer)
    {
        // interactive(false) не поддерживается фреймворком (находка 30.09.2026)
        configurer.interactive(true)
            .description(Messages.SpaceAtStartCommentFix_Description)
            .details(Messages.SpaceAtStartCommentFix_Details);
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
        String current = model.getDocument().get(offset, COMMENT_LENGTH);
        if (!"//".equals(current)) //$NON-NLS-1$
        {
            // на позиции уже не комментарий (маркер устарел после правок)
            return null;
        }
        return new InsertEdit(offset + COMMENT_LENGTH, " "); //$NON-NLS-1$
    }
}
