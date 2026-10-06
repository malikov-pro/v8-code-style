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
 *     malikov-pro - port of the APK check АПК_00714
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Module;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: в сообщениях для пользователя не употребляются местоимения
 * «Вы», «Вас», «Вам», «Вами», «Ваш» — сообщения составляются в форме
 * безличного предложения.
 * <p>
 * Перенос проверки АПК_00714 (статья 585 стандарта 1С). Как и в алгоритме
 * АПК, ищутся цельные слова (в любом регистре) в строковых литералах модуля;
 * слово внутри другого слова («Вашего», «Выполнить», «Василий») не
 * фиксируется. Замечание — одно на строку вхождения. Комментарии модуля
 * не проверяются (в отличие от АПК, сканировавшего весь текст модуля).
 *
 * @author malikov-pro
 */
public class ApkNoYouPronounCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00714-no-you-pronoun"; //$NON-NLS-1$

    // Цельное слово из набора: взгляд назад/вперёд на букву, цифру или «_»
    // исключает части слов. Границы слова через \b для кириллицы не годятся:
    // в Java \b учитывает только ASCII-буквы.
    private static final Pattern PRONOUN_PATTERN = Pattern
        .compile("(?<![\\p{L}\\p{Nd}_])(вы|вас|вам|вами|ваш)(?![\\p{L}\\p{Nd}_])", //$NON-NLS-1$
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /**
     * Instantiates a new check.
     */
    public ApkNoYouPronounCheck()
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
        builder.title(Messages.ApkNoYouPronounCheck_title)
            .description(Messages.ApkNoYouPronounCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
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
        Set<Integer> reportedLines = new HashSet<>();
        for (ILeafNode leafNode : node.getLeafNodes())
        {
            String text = leafNode.getText();
            // только строковые литералы: токен начинается с «"» или с «|»
            // переноса строки; комментарии и код не сканируются
            if (text.isEmpty() || !(text.startsWith("\"") || text.startsWith("|"))) //$NON-NLS-1$ //$NON-NLS-2$
            {
                continue;
            }
            Matcher matcher = PRONOUN_PATTERN.matcher(text);
            while (matcher.find())
            {
                int line = leafNode.getStartLine() + newLinesBefore(text, matcher.start());
                if (!reportedLines.add(line))
                {
                    continue; // замечание — одно на строку вхождения
                }
                String message = MessageFormat.format(
                    Messages.ApkNoYouPronounCheck_Message_addresses_user_with_pronoun, matcher.group()
                        .toUpperCase(Locale.ROOT),
                    moduleName, line);
                DirectLocation location = new DirectLocation(leafNode.getOffset() + matcher.start(),
                    matcher.end() - matcher.start(), line, module);
                resultAceptor.addIssue(new BslDirectLocationIssue(message, location));
            }
        }
    }

    private static int newLinesBefore(String text, int position)
    {
        int count = 0;
        for (int i = 0; i < position; i++)
        {
            if (text.charAt(i) == '\n')
            {
                count++;
            }
        }
        return count;
    }
}
