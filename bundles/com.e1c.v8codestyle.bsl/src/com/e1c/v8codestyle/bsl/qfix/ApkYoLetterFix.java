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

import java.text.MessageFormat;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.XtextResource;

import com._1c.g5.v8.dt.bsl.model.Module;
import com.e1c.g5.v8.dt.bsl.check.qfix.IXtextBslModuleFixModel;
import com.e1c.g5.v8.dt.bsl.check.qfix.SingleVariantXtextBslModuleFix;
import com.e1c.g5.v8.dt.check.qfix.components.QuickFix;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Quick fix для проверки «Буква ё не допускается в текстах модулей»:
 * заменяет каждое вхождение «ё»/«Ё» на «е»/«Е» во всём модуле
 * (код, строковые литералы, комментарии). Перенос АПК_00260.
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
        configurer.interactive(true)
            .description(Messages.ApkYoLetterFix_Description)
            .details(Messages.ApkYoLetterFix_Details);
    }

    @Override
    protected TextEdit fixIssue(XtextResource state, IXtextBslModuleFixModel model) throws BadLocationException
    {
        EObject element = model.getElement();
        Module module = (element instanceof Module m) ? m : null;
        if (module == null)
        {
            return null;
        }
        INode node = NodeModelUtils.findActualNodeFor(module);
        if (node == null)
        {
            return null;
        }
        MultiTextEdit result = new MultiTextEdit();
        for (ILeafNode leafNode : node.getLeafNodes())
        {
            String text = leafNode.getText();
            for (int i = 0; i < text.length(); i++)
            {
                char ch = text.charAt(i);
                if (ch == 'ё')
                {
                    result.addChild(new ReplaceEdit(leafNode.getOffset() + i, 1, "е")); //$NON-NLS-1$
                }
                else if (ch == 'Ё')
                {
                    result.addChild(new ReplaceEdit(leafNode.getOffset() + i, 1, "Е")); //$NON-NLS-1$
                }
            }
        }
        if (result.getChildrenSize() > 0)
        {
            return result;
        }
        return null;
    }
}
