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
 *     1C-Soft LLC - initial API and implementation (upstream checks this port is based on)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Module;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.Issue;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;

/**
 * Проверка: в текстах модулей не допускается использовать букву «ё».
 * <p>
 * Перенос проверки АПК_00260 (статья 598 стандарта 1С). Каждое вхождение
 * буквы «ё» (в любом регистре) — в коде, строковых литералах и комментариях —
 * помечается отдельным замечанием, как в исходном алгоритме АПК.
 *
 * @author malikov-pro
 */
public class ApkYoLetterCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00260-no-yo-letter"; //$NON-NLS-1$

    private static final String LETTER_YO_LOWER = "ё"; //$NON-NLS-1$
    private static final String LETTER_YO_UPPER = "Ё"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.ApkYoLetterCheck_title)
            .description(Messages.ApkYoLetterCheck_description)
            .issueType(IssueType.CODE_STYLE)
            .severity(IssueSeverity.TRIVIAL)
            .module()
            .checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Module module = (Module)object;
        INode node = NodeModelUtils.findActualNodeFor(module);
        if (node == null)
        {
            return;
        }
        String moduleName = EcoreUtil.getURI(module).lastSegment();
        for (ILeafNode leafNode : node.getLeafNodes())
        {
            String text = leafNode.getText();
            if (text.isEmpty() || (text.indexOf(LETTER_YO_LOWER) < 0 && text.indexOf(LETTER_YO_UPPER) < 0))
            {
                continue;
            }
            int newLines = 0;
            for (int i = 0; i < text.length(); i++)
            {
                char ch = text.charAt(i);
                if (ch == '\n')
                {
                    newLines++;
                    continue;
                }
                if (ch == 'ё' || ch == 'Ё')
                {
                    String message = MessageFormat.format(
                        Messages.ApkYoLetterCheck_Yo_letter_is_not_allowed_in_module_text,
                        LETTER_YO_UPPER, moduleName, leafNode.getStartLine() + newLines);
                    DirectLocation location =
                        new DirectLocation(leafNode.getOffset() + i, 1, leafNode.getStartLine() + newLines, module);
                    Issue issue = new BslDirectLocationIssue(message, location);
                    resultAceptor.addIssue(issue);
                }
            }
        }
    }

    /**
     * Instantiates a new check.
     */
    public ApkYoLetterCheck()
    {
        super();
    }
}
