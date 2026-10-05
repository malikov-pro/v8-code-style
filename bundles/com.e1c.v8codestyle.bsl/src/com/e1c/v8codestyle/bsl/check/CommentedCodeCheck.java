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
 *     malikov-pro - port of the BSL Language Server diagnostic CommentedCode
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Module;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.Issue;
import com.e1c.g5.v8.dt.check.WrongParameterException;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: закомментированные фрагменты кода. Программные модули не должны
 * иметь закомментированного кода — его место в системе контроля версий.
 * <p>
 * Перенос диагностики BSL Language Server CommentedCode
 * (тип CODE_SMELL, серьёзность MINOR). Смежные подряд строки комментариев
 * группируются; группа фиксируется, если распознаётся как код (эвристика
 * {@link CommentCodeRecognizer}, порог — параметр, по умолчанию 0.9, как в LS).
 * Как и в LS: документирующие шапки методов не фиксируются (в EDT — по
 * признаку «группа примыкает к объявлению Процедура/Функция/&Аннотация/Перем»);
 * префиксы исключений — параметр. Quick fix: см.
 * {@link com.e1c.v8codestyle.bsl.qfix.CommentedCodeFix} — удаляет группу.
 *
 * @author malikov-pro
 */
public class CommentedCodeCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "commented-code"; //$NON-NLS-1$

    private static final double DEFAULT_THRESHOLD = 0.9;

    // два «слова» подряд без операторов между ними — естественный язык, не код
    // (упрощение LS-проверки «два IDENTIFIER подряд» через токенайзер)
    private static final Pattern TWO_WORDS_IN_A_ROW =
        Pattern.compile("[\\p{L}_][\\p{L}\\p{Nd}_]*\\s+[\\p{L}_][\\p{L}\\p{Nd}_]*", Pattern.UNICODE_CASE);

    private static final Pattern MODULE_MEMBER_DECLARATION =
        Pattern.compile("^(?:&[А-Яа-яA-Za-z]+|Перем|Variable\\b)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final String PARAM_THRESHOLD = "threshold"; //$NON-NLS-1$

    private static final String PARAM_EXCLUSION_PREFIXES = "exclusionPrefixes"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public CommentedCodeCheck()
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
        builder.title(Messages.CommentedCodeCheck_title)
            .description(Messages.CommentedCodeCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE)
            .parameter(PARAM_THRESHOLD, String.class, String.valueOf(DEFAULT_THRESHOLD),
                Messages.CommentedCodeCheck_Threshold)
            .parameter(PARAM_EXCLUSION_PREFIXES, String.class, "", //$NON-NLS-1$
                Messages.CommentedCodeCheck_Exclusion_prefixes);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Module module = (Module)object;

        double threshold = threshold(parameters);
        List<String> exclusionPrefixes = exclusionPrefixes(parameters);
        CommentCodeRecognizer recognizer = new CommentCodeRecognizer(threshold);

        String moduleText = NodeModelUtils.findActualNodeFor(module).getText();

        List<ILeafNode> group = new ArrayList<>();
        int groupEndLine = -1;
        for (ILeafNode leaf : NodeModelUtils.findActualNodeFor(module).getLeafNodes())
        {
            String text = leaf.getText();
            if (!leaf.isHidden() || !text.startsWith("//") || leaf.getSyntaxErrorMessage() != null) //$NON-NLS-1$
            {
                continue;
            }
            int startLine = leaf.getStartLine();
            if (!group.isEmpty() && startLine > groupEndLine + 1)
            {
                checkGroup(group, module, recognizer, exclusionPrefixes, resultAceptor);
                group = new ArrayList<>();
            }
            group.add(leaf);
            groupEndLine = startLine + newlines(text);
        }
        checkGroup(group, module, recognizer, exclusionPrefixes, resultAceptor);
    }

    private void checkGroup(List<ILeafNode> group, Module module, CommentCodeRecognizer recognizer,
        List<String> exclusionPrefixes, ResultAcceptor resultAceptor)
    {
        if (group.isEmpty() || isMethodDescription(group, NodeModelUtils.findActualNodeFor(module).getText()))
        {
            return;
        }
        for (ILeafNode comment : group)
        {
            if (isTextParsedAsCode(comment.getText(), recognizer, exclusionPrefixes))
            {
                ILeafNode first = group.get(0);
                ILeafNode last = group.get(group.size() - 1);
                DirectLocation location = new DirectLocation(first.getOffset(),
                    last.getOffset() + last.getLength() - first.getOffset(), first.getStartLine(), module);
                Issue issue = new BslDirectLocationIssue(Messages.CommentedCodeCheck_Commented_out_code, location);
                resultAceptor.addIssue(issue);
                return;
            }
        }
    }

    /**
     * Группа комментариев непосредственно перед объявлением метода/переменной
     * или аннотацией — документирующая шапка, не фиксируется (упрощение LS:
     * там исключаются привязанные MethodDescription).
     */
    private static boolean isMethodDescription(List<ILeafNode> group, String moduleText)
    {
        ILeafNode last = group.get(group.size() - 1);
        int offset = last.getTotalEndOffset();
        while (offset < moduleText.length())
        {
            char c = moduleText.charAt(offset);
            if (c == '\n' || c == '\r' || Character.isWhitespace(c))
            {
                offset++;
                continue;
            }
            break;
        }
        if (offset >= moduleText.length())
        {
            return false;
        }
        int lineEnd = moduleText.indexOf('\n', offset);
        if (lineEnd < 0)
        {
            lineEnd = moduleText.length();
        }
        String nextLine = moduleText.substring(offset, lineEnd).trim();
        return MODULE_MEMBER_DECLARATION.matcher(nextLine).find() || nextLine.startsWith("//"); //$NON-NLS-1$
    }

    private static int newlines(String text)
    {
        int count = 0;
        for (int i = 0; i < text.length(); i++)
        {
            if (text.charAt(i) == '\n')
            {
                count++;
            }
        }
        return count;
    }

    /**
     * Распознан ли текст комментария как закомментированный код (порт
     * isTextParsedAsCode LS).
     */
    private static boolean isTextParsedAsCode(String text, CommentCodeRecognizer recognizer,
        List<String> exclusionPrefixes)
    {
        String uncommented = uncomment(text);

        for (String prefix : exclusionPrefixes)
        {
            if (uncommented.startsWith(prefix))
            {
                return false;
            }
        }
        if (!recognizer.meetsCondition(text))
        {
            return false;
        }
        // два слова подряд без оператора между ними — естественный язык, не код
        return !TWO_WORDS_IN_A_ROW.matcher(uncommented).find();
    }

    private static String uncomment(String comment)
    {
        if (comment.startsWith("//")) //$NON-NLS-1$
        {
            return uncomment(comment.substring(2));
        }
        return comment;
    }

    private static double threshold(ICheckParameters parameters)
    {
        String value;
        try
        {
            value = parameters.getString(PARAM_THRESHOLD);
        }
        catch (WrongParameterException e)
        {
            value = String.valueOf(DEFAULT_THRESHOLD);
        }
        try
        {
            return Double.parseDouble((value == null ? "" : value.trim()).replace(',', '.')); //$NON-NLS-1$
        }
        catch (NumberFormatException e)
        {
            return DEFAULT_THRESHOLD;
        }
    }

    private static List<String> exclusionPrefixes(ICheckParameters parameters)
    {
        String value;
        try
        {
            value = parameters.getString(PARAM_EXCLUSION_PREFIXES);
        }
        catch (WrongParameterException e)
        {
            value = ""; //$NON-NLS-1$
        }
        List<String> prefixes = new ArrayList<>();
        if (value != null && !value.isBlank())
        {
            for (String part : value.split(",")) //$NON-NLS-1$
            {
                if (!part.trim().isEmpty())
                {
                    prefixes.add(part.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return prefixes;
    }
}
