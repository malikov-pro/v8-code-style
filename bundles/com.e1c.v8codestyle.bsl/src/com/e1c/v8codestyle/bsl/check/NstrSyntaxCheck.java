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
 *     malikov-pro - port of the upstream issue #746 (standard 761)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.STRING_LITERAL__LINES;

import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;

import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.StringLiteral;
import com._1c.g5.v8.dt.bsl.model.util.BslUtil;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Checks the syntax of the localized string passed to the {@code NStr}
 * ({@code НСтр}) function. The string literal must contain one or more
 * entries of a language code (2-5 letters with an optional region suffix,
 * for example {@code ru}, {@code en}, {@code zh-CN}), the equal sign and the
 * message text enclosed in single quotes, separated by commas. Single quotes
 * inside the message text must be doubled.
 * <p>
 * For example, the correct localized string is
 * {@code НСтр("ru='Текст сообщения'")} or
 * {@code NStr("ru='Text', en='Text'")}. Violations are a value in double
 * quotes or without quotes, a missing language code, an unterminated message
 * text or a trailing separator.
 * <p>
 * The first parameter that is not a string literal (including a nested
 * {@code NStr} call — double localization) is reported by the
 * {@code bsl-nstr-string-literal-format} check and is not checked here.
 *
 * @author malikov-pro
 */
public class NstrSyntaxCheck
    extends BasicCheck
{

    /** The check id of the upstream issue #746 port. */
    public static final String CHECK_ID = "up-746-nstr-syntax"; //$NON-NLS-1$

    private static final String NSTR_NAME = "NStr"; //$NON-NLS-1$

    private static final String NSTR_NAME_RU = "НСтр"; //$NON-NLS-1$

    /**
     * The language code: 2-5 letters with an optional region suffix, for
     * example {@code ru}, {@code en}, {@code zh-CN}, {@code pt_BR}.
     */
    private static final Pattern LANGUAGE_CODE = Pattern.compile("[A-Za-z]{2,5}([-_][A-Za-z0-9]{2,5})?"); //$NON-NLS-1$

    private static final char QUOTE = '\'';

    private static final char DOUBLE_QUOTE = '"';

    private static final char EQUALS = '=';

    private static final char COMMA = ',';

    private static final char SEMICOLON = ';';

    /**
     * Instantiates a new NSTR syntax check.
     */
    public NstrSyntaxCheck()
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
        builder.title(Messages.NstrSyntaxCheck_title)
            .description(Messages.NstrSyntaxCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(INVOCATION);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        Invocation inv = (Invocation)object;
        if (inv.getParams().isEmpty() || inv.getMethodAccess() == null
            || !(NSTR_NAME_RU.equalsIgnoreCase(inv.getMethodAccess().getName())
                || NSTR_NAME.equalsIgnoreCase(inv.getMethodAccess().getName())))
        {
            return;
        }

        Expression first = inv.getParams().get(0);
        // A first parameter that is not a string literal (including a nested
        // NStr call) is reported by the bsl-nstr-string-literal-format check.
        if (!(first instanceof StringLiteral literal))
        {
            return;
        }

        if (!matchesLocalizedSyntax(content(literal)))
        {
            resultAceptor.addIssue(Messages.NstrSyntaxCheck_String_does_not_match_pattern, first,
                STRING_LITERAL__LINES);
        }
    }

    /**
     * Joins the literal lines into the localized string content: the service
     * double quotes and the multiline continuation symbol are removed by the
     * platform utility, each line is stripped to drop the hidden surrounding
     * whitespace.
     */
    private static String content(StringLiteral literal)
    {
        List<String> lines = BslUtil.getStringLiteralContent(literal, false);
        StringBuilder builder = new StringBuilder();
        for (String line : lines)
        {
            if (builder.length() > 0)
            {
                builder.append('\n');
            }
            builder.append(line == null ? "" : line.strip()); //$NON-NLS-1$
        }
        return builder.toString().strip();
    }

    /**
     * Checks that the content matches the localized string syntax: one or
     * more entries of {@code language-code='message-text'} separated by
     * commas, where single quotes inside the message text are doubled.
     *
     * @param content the localized string content, cannot be {@code null}
     * @return {@code true} if the content matches the syntax
     */
    private static boolean matchesLocalizedSyntax(String content)
    {
        if (content.isBlank())
        {
            return false;
        }
        int length = content.length();
        int i = 0;
        int entries = 0;
        while (true)
        {
            i = skipWhitespace(content, i);
            // language code
            if (i >= length || !isCodeStart(content.charAt(i)))
            {
                return false;
            }
            int codeEnd = i + 1;
            while (codeEnd < length && isCodePart(content.charAt(codeEnd)))
            {
                codeEnd++;
            }
            if (LANGUAGE_CODE.matcher(content.substring(i, codeEnd)).matches())
            {
                i = codeEnd;
            }
            else
            {
                return false;
            }

            i = skipWhitespace(content, i);
            if (i >= length || content.charAt(i) != EQUALS)
            {
                return false;
            }

            i = skipWhitespace(content, i + 1);
            if (i >= length || content.charAt(i) != QUOTE)
            {
                // The message text must be enclosed in single quotes: a
                // value in double quotes or without quotes is a violation.
                return false;
            }
            i++;
            boolean closed = false;
            while (i < length)
            {
                char current = content.charAt(i);
                if (current == QUOTE)
                {
                    if (i + 1 < length && content.charAt(i + 1) == QUOTE)
                    {
                        i += 2; // escaped single quote
                        continue;
                    }
                    closed = true;
                    i++;
                    break;
                }
                i++;
            }
            if (!closed)
            {
                return false; // unterminated message text
            }
            entries++;

            i = skipWhitespace(content, i);
            if (i >= length)
            {
                return entries > 0;
            }
            char separator = content.charAt(i);
            if (separator != COMMA && separator != SEMICOLON)
            {
                return false; // expected the separator of language entries
            }
            i++;
        }
    }

    private static int skipWhitespace(String content, int start)
    {
        int i = start;
        while (i < content.length() && Character.isWhitespace(content.charAt(i)))
        {
            i++;
        }
        return i;
    }

    private static boolean isCodeStart(char character)
    {
        return character >= 'A' && character <= 'Z' || character >= 'a' && character <= 'z';
    }

    private static boolean isCodePart(char character)
    {
        return isCodeStart(character)
            || character >= '0' && character <= '9'
            || character == '_'
            || character == '-';
    }

}
