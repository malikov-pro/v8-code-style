/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.StringLiteral;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.WrongParameterException;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Reports a line break in an inconvenient place: a line that ends with a binary/logical operator, or a line that starts
 * with a closing brace, a semicolon or a comma.
 * <p>
 * Port of the BSL LS {@code IncorrectLineBreak} diagnostic. The LS works on the raw text lines of the document, not on
 * the AST, and so does this port; the LS contract is reproduced:
 * <ul>
 * <li>the first-symbol rule matches {@code ^\s*(:?<listOfIncorrectFirstSymbol>)} — by default a line starting with
 * {@code )}, {@code ;}, {@code ,<non-space>} or {@code );};
 * <li>the last-symbol rule matches {@code \s+(:?<listOfIncorrectLastSymbol>)\s*(?://.*)?$} — by default a line ending
 * with {@code ИЛИ}, {@code И}, {@code OR}, {@code AND}, {@code +}, {@code -}, {@code /}, {@code %}, {@code *}; a
 * trailing line comment is allowed after the operator;
 * <li>both rules are case-insensitive and report the matched fragment of the line;
 * <li>a match that falls into a line comment is skipped, and a match inside a string literal is skipped (the LS tests
 * the line text with its own string-literal regular expression);
 * <li>a line before the first line of a multi-line string literal is skipped, so the concatenation of a query text
 * {@code Text = Text +} / {@code "SELECT ..."} is not reported.
 * </ul>
 * Parameters: {@code checkFirstSymbol} (default {@code true}), {@code listOfIncorrectFirstSymbol},
 * {@code checkLastSymbol} (default {@code true}), {@code listOfIncorrectLastSymbol}.
 * <p>
 * <b>Partial scope (v1):</b> the LS identifies a query through its parsed query model and skips the line before the
 * query start; the port skips the line before any <em>multi-line</em> string literal, which is wider (documented
 * deviation). Comment detection uses the module tokens, string detection reproduces the LS line-local regular
 * expression, so a match inside a string that starts on a previous line is recognized only as far as that expression
 * sees it.
 *
 * @author malikov-pro
 */
public final class InCorrectLineBreakCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "incorrect-line-break"; //$NON-NLS-1$

    private static final String PARAM_CHECK_FIRST_SYMBOL = "checkFirstSymbol"; //$NON-NLS-1$

    private static final String PARAM_FIRST_SYMBOL_LIST = "listOfIncorrectFirstSymbol"; //$NON-NLS-1$

    private static final String PARAM_CHECK_LAST_SYMBOL = "checkLastSymbol"; //$NON-NLS-1$

    private static final String PARAM_LAST_SYMBOL_LIST = "listOfIncorrectLastSymbol"; //$NON-NLS-1$

    private static final boolean DEFAULT_CHECK_FIRST_SYMBOL = true;

    /** The written LS value: {@code \)}, {@code ;}, {@code ,<non-space>} or {@code );}. */
    private static final String BACKSLASH = "\\"; //$NON-NLS-1$

    private static final String DEFAULT_FIRST_SYMBOL_LIST = BACKSLASH + ")" + "|;|," + BACKSLASH + "s*" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        + BACKSLASH + "S+|" + BACKSLASH + ");"; //$NON-NLS-1$ //$NON-NLS-2$

    private static final boolean DEFAULT_CHECK_LAST_SYMBOL = true;

    private static final String DEFAULT_LAST_SYMBOL_LIST = "ИЛИ|И|OR|AND|\\+|-|/|%|\\*"; //$NON-NLS-1$

    private static final Pattern STRING_LITERAL_PATTERN =
        Pattern.compile("[\"|][^\"\\n]+(?:\"|$)", Pattern.MULTILINE); //$NON-NLS-1$

    private static final int QUERY_START_LINE_OFFSET = 2;

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.InCorrectLineBreakCheck_title)
            .description(Messages.InCorrectLineBreakCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.TRIVIAL)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE)
            .parameter(PARAM_CHECK_FIRST_SYMBOL, Boolean.class, Boolean.toString(DEFAULT_CHECK_FIRST_SYMBOL),
                Messages.InCorrectLineBreakCheck_checkFirstSymbol)
            .parameter(PARAM_FIRST_SYMBOL_LIST, String.class, DEFAULT_FIRST_SYMBOL_LIST,
                Messages.InCorrectLineBreakCheck_listOfIncorrectFirstSymbol)
            .parameter(PARAM_CHECK_LAST_SYMBOL, Boolean.class, Boolean.toString(DEFAULT_CHECK_LAST_SYMBOL),
                Messages.InCorrectLineBreakCheck_checkLastSymbol)
            .parameter(PARAM_LAST_SYMBOL_LIST, String.class, DEFAULT_LAST_SYMBOL_LIST,
                Messages.InCorrectLineBreakCheck_listOfIncorrectLastSymbol);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Module module = (Module)object;
        INode root = NodeModelUtils.findActualNodeFor(module);
        if (root == null)
        {
            return;
        }
        List<Integer> lineStarts = new ArrayList<>();
        String text = moduleText(root, lineStarts);
        String[] lines = lines(text);
        if (lines.length == 0)
        {
            return;
        }
        Map<Integer, Integer> commentStarts = commentStarts(root, lineStarts);
        Set<Integer> multiLineStringStarts = multiLineStringStarts(module);
        if (booleanParameter(parameters, PARAM_CHECK_FIRST_SYMBOL, DEFAULT_CHECK_FIRST_SYMBOL))
        {
            String list = stringParameter(parameters, PARAM_FIRST_SYMBOL_LIST, DEFAULT_FIRST_SYMBOL_LIST);
            checkContent(module, lines, lineStarts, commentStarts, multiLineStringStarts,
                firstSymbolPattern(list, DEFAULT_FIRST_SYMBOL_LIST), acceptor, monitor);
        }
        if (monitor.isCanceled())
        {
            return;
        }
        if (booleanParameter(parameters, PARAM_CHECK_LAST_SYMBOL, DEFAULT_CHECK_LAST_SYMBOL))
        {
            String list = stringParameter(parameters, PARAM_LAST_SYMBOL_LIST, DEFAULT_LAST_SYMBOL_LIST);
            checkContent(module, lines, lineStarts, commentStarts, multiLineStringStarts,
                lastSymbolPattern(list, DEFAULT_LAST_SYMBOL_LIST), acceptor, monitor);
        }
    }

    private void checkContent(Module module, String[] lines, List<Integer> lineStarts,
        Map<Integer, Integer> commentStarts, Set<Integer> multiLineStringStarts, Pattern pattern,
        ResultAcceptor acceptor, IProgressMonitor monitor)
    {
        for (int line = 0; line < lines.length; line++)
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (multiLineStringStarts.contains(line + QUERY_START_LINE_OFFSET))
            {
                continue;
            }
            Matcher matcher = pattern.matcher(lines[line]);
            if (!matcher.find())
            {
                continue;
            }
            int start = matcher.start(1);
            int end = matcher.end(1);
            if (end >= commentStarts.getOrDefault(line, Integer.MAX_VALUE))
            {
                continue;
            }
            if (insideStringLiteral(lines[line], start, end))
            {
                continue;
            }
            acceptor.addIssue(new BslDirectLocationIssue(Messages.InCorrectLineBreakCheck_message,
                new DirectLocation(lineStarts.get(line) + start, end - start, line + 1, module)));
        }
    }

    private static Pattern firstSymbolPattern(String list, String defaultList)
    {
        return pattern("^\\s*(:?" + checkedList(list, defaultList) + ")"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static Pattern lastSymbolPattern(String list, String defaultList)
    {
        return pattern("\\s+(:?" + checkedList(list, defaultList) + ")\\s*(?://.*)?$"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static String checkedList(String list, String defaultList)
    {
        String value = list == null || list.isBlank() ? defaultList : list;
        try
        {
            Pattern.compile(value);
            return value;
        }
        catch (PatternSyntaxException e)
        {
            BslPlugin.log(BslPlugin.createWarningStatus(String.format(
                "Check %s: invalid symbol list '%s': %s", CHECK_ID, value, e.getMessage()))); //$NON-NLS-1$
            return defaultList;
        }
    }

    private static Pattern pattern(String regex)
    {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private static boolean insideStringLiteral(String line, int start, int end)
    {
        Matcher matcher = STRING_LITERAL_PATTERN.matcher(line);
        while (matcher.find())
        {
            if (matcher.start() <= start && matcher.end() >= end)
            {
                return true;
            }
        }
        return false;
    }

    private static Map<Integer, Integer> commentStarts(INode root, List<Integer> lineStarts)
    {
        Map<Integer, Integer> starts = new HashMap<>();
        for (ILeafNode leaf : root.getLeafNodes())
        {
            if (!leaf.isHidden() || !leaf.getText().strip().startsWith("//")) //$NON-NLS-1$
            {
                continue;
            }
            int line = leaf.getStartLine() - 1;
            if (line < 0 || line >= lineStarts.size())
            {
                continue;
            }
            int column = leaf.getOffset() - lineStarts.get(line);
            starts.merge(line, column, Math::min);
        }
        return starts;
    }

    private static Set<Integer> multiLineStringStarts(Module module)
    {
        Set<Integer> starts = new HashSet<>();
        for (StringLiteral literal : EcoreUtil2.getAllContentsOfType(module, StringLiteral.class))
        {
            INode node = NodeModelUtils.findActualNodeFor(literal);
            if (node != null && node.getStartLine() != node.getEndLine())
            {
                starts.add(node.getStartLine());
            }
        }
        return starts;
    }

    /**
     * Document text and absolute line starts, both taken from the leaf nodes. Leaf offsets are the authoritative
     * document offsets, so a byte order mark that the root node reports inconsistent with its text cannot shift the
     * computed positions (the LS offsets are document offsets).
     */
    private static String moduleText(INode root, List<Integer> lineStarts)
    {
        StringBuilder text = new StringBuilder();
        for (ILeafNode leaf : root.getLeafNodes())
        {
            if (lineStarts.isEmpty())
            {
                lineStarts.add(leaf.getOffset());
            }
            int absolute = leaf.getOffset();
            String leafText = leaf.getText();
            for (int index = 0; index < leafText.length(); index++)
            {
                text.append(leafText.charAt(index));
                if (leafText.charAt(index) == '\n')
                {
                    lineStarts.add(absolute + index + 1);
                }
            }
        }
        return text.toString();
    }

    private static String[] lines(String text)
    {
        String withoutCarriageReturns = text.replace("\r\n", "\n").replace('\r', '\n'); //$NON-NLS-1$ //$NON-NLS-2$
        return withoutCarriageReturns.split("\n", -1); //$NON-NLS-1$
    }

    private static boolean booleanParameter(ICheckParameters parameters, String name, boolean defaultValue)
    {
        try
        {
            return parameters.getBoolean(name);
        }
        catch (WrongParameterException e)
        {
            return defaultValue;
        }
    }

    private static String stringParameter(ICheckParameters parameters, String name, String defaultValue)
    {
        try
        {
            String value = parameters.getString(name);
            return value == null ? defaultValue : value;
        }
        catch (WrongParameterException e)
        {
            return defaultValue;
        }
    }
}
