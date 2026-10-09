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
 *     malikov-pro - port of the upstream issue #747 (standard 761)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.STRING_LITERAL__LINES;

import java.util.List;

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
 * Checks that the localized string passed to the {@code NStr}
 * ({@code НСтр}) function does not start or end with an unprintable
 * character (a space, tab or line feed). Every language entry of the form
 * {@code code='message text'} is checked; non-language characters at the
 * edges of the message must be moved into separate string literals
 * concatenated with the localized one, otherwise they can be easily lost
 * or misplaced by a translator who does not see the whole context.
 * <p>
 * Entries that do not match the localized string syntax (a value in double
 * quotes or without quotes, a missing language code, an unterminated text,
 * a trailing separator) are reported by the {@code up-746-nstr-syntax}
 * check; the first parameter that is not a string literal is reported by
 * the {@code bsl-nstr-string-literal-format} check — both are not checked
 * here. One issue is reported per literal.
 *
 * @author malikov-pro
 */
public class NstrUnprintableCharsCheck
    extends BasicCheck
{

    /** The check id of the upstream issue #747 port. */
    public static final String CHECK_ID = "up-747-nstr-unprintable-chars"; //$NON-NLS-1$

    private static final String NSTR_NAME = "NStr"; //$NON-NLS-1$

    private static final String NSTR_NAME_RU = "НСтр"; //$NON-NLS-1$

    private static final char QUOTE = '\'';

    private static final char EQUALS = '=';

    private static final char COMMA = ',';

    private static final char SEMICOLON = ';';

    /**
     * Instantiates a new NSTR unprintable chars check.
     */
    public NstrUnprintableCharsCheck()
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
        builder.title(Messages.NstrUnprintableCharsCheck_title)
            .description(Messages.NstrUnprintableCharsCheck_description)
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
        if (!(first instanceof StringLiteral literal))
        {
            return;
        }

        if (hasUnprintableEdge(content(literal)))
        {
            resultAceptor.addIssue(Messages.NstrUnprintableCharsCheck_Unprintable_character_at_edge, first,
                STRING_LITERAL__LINES);
        }
    }

    /**
     * Joins the literal lines into the localized string content: the service
     * double quotes and the multiline continuation symbol are removed by the
     * platform utility, the lines are joined as-is — the surrounding
     * whitespace of every value is preserved for the edge check.
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
            builder.append(line == null ? "" : line); //$NON-NLS-1$
        }
        return builder.toString();
    }

    /**
     * Checks the language entries of the localized string: every entry of
     * the form {@code code='message text'} must neither start nor end with
     * an unprintable character. Entries in another form (a value in double
     * quotes or without quotes, a missing language code, an unterminated
     * text, a trailing separator) are the subject of the syntax check and
     * stop the scan here.
     *
     * @param content the localized string content, cannot be {@code null}
     * @return {@code true} if some language entry starts or ends with an unprintable character
     */
    private static boolean hasUnprintableEdge(String content)
    {
        int length = content.length();
        int i = 0;
        while (true)
        {
            i = skipWhitespace(content, i);
            if (i >= length)
            {
                return false;
            }
            if (!isCodeStart(content.charAt(i)))
            {
                return false;
            }
            int codeEnd = i + 1;
            while (codeEnd < length && isCodePart(content.charAt(codeEnd)))
            {
                codeEnd++;
            }
            i = codeEnd;

            i = skipWhitespace(content, i);
            if (i >= length || content.charAt(i) != EQUALS)
            {
                return false;
            }

            i = skipWhitespace(content, i + 1);
            if (i >= length || content.charAt(i) != QUOTE)
            {
                // The message text in double quotes or without quotes is
                // reported by the up-746-nstr-syntax check.
                return false;
            }
            i++;
            int valueStart = i;
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
                    break;
                }
                i++;
            }
            if (!closed)
            {
                return false; // unterminated message text
            }
            if (isUnprintableAtEdge(content, valueStart, i))
            {
                return true;
            }
            i++;

            i = skipWhitespace(content, i);
            if (i >= length)
            {
                return false;
            }
            char separator = content.charAt(i);
            if (separator != COMMA && separator != SEMICOLON)
            {
                return false; // expected the separator of language entries
            }
            i++;
        }
    }

    /**
     * Checks whether the substring between the single quotes starts or ends
     * with an unprintable character.
     */
    private static boolean isUnprintableAtEdge(String content, int start, int end)
    {
        return start < end && (Character.isWhitespace(content.charAt(start))
            || Character.isWhitespace(content.charAt(end - 1)));
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
