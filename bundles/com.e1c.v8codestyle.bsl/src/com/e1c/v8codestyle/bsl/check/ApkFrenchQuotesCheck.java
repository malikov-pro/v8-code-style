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
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.StringLiteral;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.Issue;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: в интерфейсных текстах (строковых литералах функции {@code НСтр})
 * не допускаются французские кавычки «ёлочки» — используйте прямые кавычки.
 * Кавычки вне {@code НСтр} (в том числе в комментариях и справке) не проверяются.
 * <p>
 * Перенос проверки АПК_01194 (статья 598 стандарта 1С). Каждое вхождение
 * кавычки « или » внутри {@code НСтр(...)} помечается отдельным замечанием,
 * как в исходном алгоритме АПК.
 *
 * @author malikov-pro
 */
public class ApkFrenchQuotesCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-01194-no-french-quotes"; //$NON-NLS-1$

    private static final String QUOTE_LEFT = "«"; //$NON-NLS-1$
    private static final String QUOTE_RIGHT = "»"; //$NON-NLS-1$

    private static final String NSTR_NAME = "NStr"; //$NON-NLS-1$
    private static final String NSTR_NAME_RU = "НСтр"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public ApkFrenchQuotesCheck()
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
        builder.title(Messages.ApkFrenchQuotesCheck_title)
            .description(Messages.ApkFrenchQuotesCheck_description)
            .issueType(IssueType.CODE_STYLE)
            .severity(IssueSeverity.TRIVIAL)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(INVOCATION);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Invocation invocation = (Invocation)object;
        if (invocation.getParams().isEmpty() || invocation.getMethodAccess() == null
            || !(NSTR_NAME_RU.equalsIgnoreCase(invocation.getMethodAccess().getName())
                || NSTR_NAME.equalsIgnoreCase(invocation.getMethodAccess().getName())))
        {
            return; // not an NStr call
        }

        Module module = (Module)EcoreUtil.getRootContainer(invocation);
        for (Expression param : invocation.getParams())
        {
            if (!(param instanceof StringLiteral))
            {
                continue;
            }
            checkStringLiteral((StringLiteral)param, module, resultAceptor);
        }
    }

    private void checkStringLiteral(StringLiteral literal, Module module, ResultAcceptor resultAceptor)
    {
        INode node = NodeModelUtils.findActualNodeFor(literal);
        if (node == null)
        {
            return;
        }
        for (ILeafNode leafNode : node.getLeafNodes())
        {
            String text = leafNode.getText();
            if (text.isEmpty() || (text.indexOf(QUOTE_LEFT) < 0 && text.indexOf(QUOTE_RIGHT) < 0))
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
                if (ch == '«' || ch == '»')
                {
                    String message = MessageFormat.format(
                        Messages.ApkFrenchQuotesCheck_French_quotes_are_not_allowed_in_interface_text,
                        QUOTE_LEFT, QUOTE_RIGHT);
                    DirectLocation location =
                        new DirectLocation(leafNode.getOffset() + i, 1, leafNode.getStartLine() + newLines, module);
                    Issue issue = new BslDirectLocationIssue(message, location);
                    resultAceptor.addIssue(issue);
                }
            }
        }
    }
}
