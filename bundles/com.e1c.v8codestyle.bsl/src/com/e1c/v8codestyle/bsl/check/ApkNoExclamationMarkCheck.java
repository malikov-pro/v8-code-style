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
 *     malikov-pro - port of the APK check АПК_00715
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

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
 * Проверка: сообщения не должны содержать восклицательных знаков (за
 * исключением предупреждений об опасных или критических действиях).
 * <p>
 * Перенос проверки АПК_00715 (статья 585 стандарта 1С) со всеми исключениями
 * исходного алгоритма:
 * <ul>
 * <li>слово «Внимание!» (англ. «Attention!») — допустимо, вырезается до
 * поиска;</li>
 * <li>после «!» допустимы операторы и xsl-коды ({@code !=} и пр.) — не
 * ошибка;</li>
 * <li>параметры bat-файлов {@code !args!}, {@code !setup.exe!},
 * {@code !directory!} — не ошибка;</li>
 * <li>«!» после кавычки-разделителя строк считается ошибкой (замена
 * {@code "!} → {@code 0!}), а конструкция {@code "!"} — вырезается;</li>
 * <li>знак фиксируется, только если перед ним буква, цифра или «()» вызова
 * метода.</li>
 * </ul>
 * Сканируются строковые литералы модуля (пробелы и табы вырезаются, как в
 * АПК); комментарии не проверяются. Замечание — одно на строку вхождения.
 *
 * @author malikov-pro
 */
public class ApkNoExclamationMarkCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00715-no-exclamation-mark"; //$NON-NLS-1$

    // АПК: НедопустимыеСимволыДоЗнака — «!» означает восклицание, только если
    // перед ним буква (рус/лат), цифра или скобка вызова метода
    private static final String INVALID_CHARS_BEFORE = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" //$NON-NLS-1$
        + "QWERTYUIOPASDFGHJKLZXCVBNM" //$NON-NLS-1$
        + "0123456789" //$NON-NLS-1$
        + "()"; //$NON-NLS-1$

    // АПК: ДопустимыеСимволыПослеЗнака — операторы и xsl-коды («!=» и пр.)
    private static final String ALLOWED_CHARS_AFTER = "#$%^&*()+`~\\|/=<>"; //$NON-NLS-1$

    private static final String ATTENTION_RU = "ВНИМАНИЕ!"; //$NON-NLS-1$
    private static final String ATTENTION_EN = "ATTENTION!"; //$NON-NLS-1$

    private static final String BAT_ARGS = "!ARGS!"; //$NON-NLS-1$
    private static final String BAT_SETUP = "!SETUP.EXE!"; //$NON-NLS-1$
    private static final String BAT_DIRECTORY = "!DIRECTORY!"; //$NON-NLS-1$

    private static final String EXCLAMATION = "!"; //$NON-NLS-1$
    private static final char QUOTE_CHAR = '"';
    private static final char EXCLAMATION_CHAR = '!';

    /**
     * Instantiates a new check.
     */
    public ApkNoExclamationMarkCheck()
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
        builder.title(Messages.ApkNoExclamationMarkCheck_title)
            .description(Messages.ApkNoExclamationMarkCheck_description)
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
            checkStringLiteral(leafNode, text, module, moduleName, reportedLines, resultAceptor);
        }
    }

    private void checkStringLiteral(ILeafNode leafNode, String text, Module module, String moduleName,
        Set<Integer> reportedLines, ResultAcceptor resultAceptor)
    {
        int length = text.length();
        char[] chars = new char[length];
        for (int i = 0; i < length; i++)
        {
            chars[i] = Character.toUpperCase(text.charAt(i));
        }

        // как в АПК: вырезанные символы остаются на местах, но помечаются
        boolean[] removed = new boolean[length];
        boolean[] digitBeforeExclamation = new boolean[length];

        // табы и пробелы вырезаются до поиска (как в АПК)
        for (int i = 0; i < length; i++)
        {
            char ch = chars[i];
            if (ch == ' ' || ch == '\t')
            {
                removed[i] = true;
            }
        }
        // «"!» — не ошибка, вырезается целиком
        removeSequence(chars, removed, digitBeforeExclamation, "\"!" + "\"", false); //$NON-NLS-1$ //$NON-NLS-2$
        // «"!» — ошибка: кавычка перед «!» заменяется на «0» (цифру)
        removeSequence(chars, removed, digitBeforeExclamation, "\"!", true); //$NON-NLS-1$
        // допустимо использовать в сообщениях слово «Внимание!» (англ. «Attention!»)
        removeSequence(chars, removed, digitBeforeExclamation, ATTENTION_RU, false);
        removeSequence(chars, removed, digitBeforeExclamation, ATTENTION_EN, false);
        // ошибка в склейке строк: НСтр("...модуль " + Имя + "!'")
        removeSequence(chars, removed, digitBeforeExclamation, "+" + "\"!", true); //$NON-NLS-1$ //$NON-NLS-2$
        // остальные кавычки не должны скрывать «!»
        for (int i = 0; i < length; i++)
        {
            if (chars[i] == QUOTE_CHAR)
            {
                removed[i] = true;
            }
        }
        // параметры bat-файлов в тексте — не ошибка
        removeSequence(chars, removed, digitBeforeExclamation, BAT_ARGS, false);
        removeSequence(chars, removed, digitBeforeExclamation, BAT_SETUP, false);
        removeSequence(chars, removed, digitBeforeExclamation, BAT_DIRECTORY, false);

        for (int i = 0; i < length; i++)
        {
            if (chars[i] != EXCLAMATION_CHAR || removed[i])
            {
                continue;
            }
            char prev = digitBeforeExclamation[i] ? '0' : nearestKeptChar(chars, removed, i - 1, -1);
            char next = nearestKeptChar(chars, removed, i + 1, 1);
            // ошибка: перед знаком буква/цифра/скобка, а после — не оператор
            if (INVALID_CHARS_BEFORE.indexOf(prev) < 0 || ALLOWED_CHARS_AFTER.indexOf(next) >= 0)
            {
                continue;
            }
            int line = leafNode.getStartLine() + newLinesBefore(text, i);
            if (!reportedLines.add(line))
            {
                continue; // замечание — одно на строку вхождения
            }
            String message = MessageFormat.format(Messages.ApkNoExclamationMarkCheck_Message_contains_exclamation_mark,
                moduleName, line);
            DirectLocation location = new DirectLocation(leafNode.getOffset() + i, 1, line, module);
            resultAceptor.addIssue(new BslDirectLocationIssue(message, location));
        }
    }

    /**
     * Ищет последовательность подряд идущих невырезанных символов (как
     * СтрЗаменить по тексту после вырезания пробелов) и помечает её: при
     * {@code keepExclamation} символ «!» остаётся и помечается как стоящий
     * после «0» (ошибка), иначе вырезается вся последовательность.
     */
    private static void removeSequence(char[] chars, boolean[] removed, boolean[] digitBeforeExclamation,
        String sequence, boolean keepExclamation)
    {
        int seqLength = sequence.length();
        int start = 0;
        while (start < chars.length)
        {
            if (removed[start] || chars[start] != sequence.charAt(0))
            {
                start++;
                continue;
            }
            int matched = 1;
            int end = start + 1;
            while (end < chars.length && matched < seqLength)
            {
                if (removed[end])
                {
                    end++;
                    continue;
                }
                if (chars[end] != sequence.charAt(matched))
                {
                    break;
                }
                matched++;
                end++;
            }
            if (matched < seqLength)
            {
                start++;
                continue;
            }
            for (int i = start; i < end; i++)
            {
                if (keepExclamation && chars[i] == EXCLAMATION_CHAR)
                {
                    digitBeforeExclamation[i] = true;
                }
                else
                {
                    removed[i] = true;
                }
            }
            start = end;
        }
    }

    private static char nearestKeptChar(char[] chars, boolean[] removed, int from, int step)
    {
        for (int i = from; i >= 0 && i < chars.length; i += step)
        {
            if (!removed[i])
            {
                return chars[i];
            }
        }
        return '\0'; // за границей литерала: не буква/цифра и не допустимый символ
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
