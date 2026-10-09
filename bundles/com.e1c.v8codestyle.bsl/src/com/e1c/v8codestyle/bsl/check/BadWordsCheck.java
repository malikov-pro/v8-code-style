/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: malikov-pro
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the BSL Language Server diagnostic BadWords
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Module;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.Issue;
import com.e1c.g5.v8.dt.check.WrongParameterException;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.OptInCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: в тексте модуля не встречаются запрещённые слова. Список слов
 * задаётся регулярным выражением в параметре проверки {@code badWords};
 * поиск выполняется без учёта регистра.
 * <p>
 * Перенос диагностики BSL Language Server BadWords
 * (в LS серьёзность MAJOR, тип CODE_SMELL, выключена по умолчанию).
 * Как и в LS:
 * <ul>
 * <li>по умолчанию список слов пустой — пока пользователь не задал параметр,
 * проверка не находит ничего;</li>
 * <li>сканируется каждая строка текста модуля — комментарии и строковые
 * литералы включаются (в LS это поведение параметра {@code findInComments},
 * по умолчанию {@code true}); отдельный параметр «искать в комментариях»
 * не переносился;</li>
 * <li>каждое вхождение на строке — отдельное замечание на позицию вхождения,
 * совпадения внутри идентификаторов тоже учитываются.</li>
 * </ul>
 * Проверка выключена по умолчанию: список запрещённых слов — решение
 * пользователя (стандарты команды/проекта).
 *
 * @author malikov-pro
 */
public class BadWordsCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "bad-words"; //$NON-NLS-1$

    /** Имя параметра проверки со списком запрещённых слов (регулярное выражение). */
    public static final String PARAM_BAD_WORDS = "badWords"; //$NON-NLS-1$

    private static final String DEFAULT_BAD_WORDS = ""; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public BadWordsCheck()
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
        builder.title(Messages.BadWordsCheck_title)
            .description(Messages.BadWordsCheck_description)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new OptInCheckExtension())
            .module()
            .checkedObjectType(MODULE)
            .parameter(PARAM_BAD_WORDS, String.class, DEFAULT_BAD_WORDS, Messages.BadWordsCheck_Words);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Module module = (Module)object;

        String badWords;
        try
        {
            badWords = parameters.getString(PARAM_BAD_WORDS);
        }
        catch (WrongParameterException e)
        {
            badWords = DEFAULT_BAD_WORDS;
        }
        if (badWords == null || badWords.isBlank())
        {
            return;
        }

        Pattern pattern;
        try
        {
            // аналог CaseInsensitivePattern.compile(...) в BSL LS
            pattern = Pattern.compile(badWords, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
        catch (PatternSyntaxException e)
        {
            BslPlugin.log(BslPlugin.createWarningStatus(
                String.format("Check %s: badWords parameter is not a valid regular expression: %s", //$NON-NLS-1$
                    CHECK_ID, e.getMessage())));
            return;
        }

        String text = NodeModelUtils.findActualNodeFor(module).getText();
        int line = 1;
        int lineStart = 0;
        int index = 0;
        while (index <= text.length())
        {
            int newLinePos = text.indexOf('\n', index);
            int end = newLinePos < 0 ? text.length() : newLinePos;
            String lineText = text.substring(lineStart, end);

            Matcher matcher = pattern.matcher(lineText);
            while (matcher.find())
            {
                int offset = lineStart + matcher.start();
                int length = matcher.end() - matcher.start();
                String message =
                    MessageFormat.format(Messages.BadWordsCheck_Prohibited_word_found, matcher.group());
                Issue issue = new BslDirectLocationIssue(message, new DirectLocation(offset, length, line, module));
                resultAceptor.addIssue(issue);
            }

            if (newLinePos < 0)
            {
                break;
            }
            lineStart = newLinePos + 1;
            index = lineStart;
            line++;
        }
    }
}
