/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.BreakStatement;
import com._1c.g5.v8.dt.bsl.model.Conditional;
import com._1c.g5.v8.dt.bsl.model.ContinueStatement;
import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.EmptyExpression;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.IfStatement;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.LoopStatement;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.RaiseStatement;
import com._1c.g5.v8.dt.bsl.model.ReturnStatement;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.TryExceptStatement;
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
 * Reports repeated insertion of the same value into the same collection when nothing between the insertions can have
 * changed the value or the collection.
 * <p>
 * Port of the BSL LS {@code DuplicatedInsertionIntoCollection} diagnostic. Contract reproduced from the LS sources:
 * <ul>
 * <li>only a call statement of the form {@code Collection.Method(...)} ({@code DynamicFeatureAccess}) counts; a bare
 * {@code Method(...)} call has no collection and is ignored;
 * <li>the method name must be exactly {@code Вставить}/{@code Добавить}/{@code Insert}/{@code Add} (case-insensitive);
 * the LS parameter {@code isAllowedMethodADD} is not ported, the default LS behaviour (analyze {@code Add}) is used;
 * <li>the value is the <b>first</b> actual parameter, compared by its LS-like token text (whitespace-insensitive),
 * case-insensitively outside string literals; an empty parameter, a blank string literal and the ignored literals
 * {@code Неопределено}/{@code Undefined}/{@code 0}/{@code Символы.*}/{@code Chars.*} never take part;
 * <li>the collection key is the token text of the receiver, so {@code Объект.Коллекция} and {@code Объект.Метод().Коллекция}
 * are different collections;
 * <li>insertions are grouped only inside one LS "code block": direct statements of the module, of a method, of an
 * if/elsif/else branch, of a loop or of a try/except branch. Insertions in different branches are never compared;
 * <li>a pair of consecutive insertions is "broken" by an assignment between them that touches the collection (or its
 * parent) or the value (or any of its parent prefixes), by {@code Возврат}/{@code Прервать}/{@code Продолжить}/
 * {@code ВызватьИсключение} between them, and by another call between them that passes the collection or the value as
 * an argument (possible side effect).
 * </ul>
 * One marker is placed on the second insertion of each unbroken sequence (the LS places it on the second call statement
 * as well, but keeps the whole sequence in related information, which is not reproduced here).
 * <p>
 * <b>Partial scope (v1):</b> there is no data-flow or CFG analysis, so the port only proves "no visible change between
 * two statements of one block". A parameter or a global variable changed inside a called method is not tracked, an
 * indirect value change is recognised only through the argument check above, and preprocessor branches
 * ({@code #Если}) are not merged the way the LS parser merges them. A {@code Прервать}/{@code Продолжить} inside an
 * inner loop between the insertions is treated as a change, while the LS ignores a breaker that belongs to an enclosing
 * loop. The LS parameter {@code isAllowedMethodADD} is not implemented.
 *
 * @author malikov-pro
 */
public final class DuplicatedInsertionIntoCollectionCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "duplicated-insertion-into-collection"; //$NON-NLS-1$

    private static final Pattern INSERT_METHOD_PATTERN = Pattern.compile("вставить|добавить|insert|add", //$NON-NLS-1$
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern IGNORED_VALUE_PATTERN = Pattern.compile(
        "неопределено|undefined|0|символы\\.[\\wа-яё]+|chars\\.[\\wа-яё]+", //$NON-NLS-1$
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final String KEY_SEPARATOR = "\u0000"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.DuplicatedInsertionIntoCollectionCheck_title)
            .description(Messages.DuplicatedInsertionIntoCollectionCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Module module = (Module)object;
        for (StatementList block : blocks(module))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            analyse(module, block.owner, block.statements, acceptor);
        }
    }

    private void analyse(Module module, EObject owner, List<Statement> statements, ResultAcceptor acceptor)
    {
        List<Insertion> insertions = new ArrayList<>();
        for (Statement statement : statements)
        {
            Insertion insertion = insertion(statement);
            if (insertion != null)
            {
                insertions.add(insertion);
            }
        }
        if (insertions.size() < 2)
        {
            return;
        }
        Map<String, List<Insertion>> groups = new LinkedHashMap<>();
        for (Insertion insertion : insertions)
        {
            groups.computeIfAbsent(insertion.groupKey, unused -> new ArrayList<>()).add(insertion);
        }
        for (List<Insertion> group : groups.values())
        {
            if (group.size() < 2)
            {
                continue;
            }
            List<List<Insertion>> segments = new ArrayList<>();
            List<Insertion> segment = new ArrayList<>();
            segment.add(group.get(0));
            for (int i = 1; i < group.size(); i++)
            {
                Insertion previous = group.get(i - 1);
                Insertion current = group.get(i);
                if (hasChange(owner, previous, current))
                {
                    segments.add(segment);
                    segment = new ArrayList<>();
                }
                segment.add(current);
            }
            segments.add(segment);
            for (List<Insertion> duplicates : segments)
            {
                if (duplicates.size() > 1)
                {
                    fireIssue(module, duplicates.get(1), acceptor);
                }
            }
        }
    }

    private void fireIssue(Module module, Insertion insertion, ResultAcceptor acceptor)
    {
        INode node = NodeModelUtils.findActualNodeFor(insertion.statement);
        if (node == null)
        {
            return;
        }
        String message = MessageFormat.format(Messages.DuplicatedInsertionIntoCollectionCheck_message,
            insertion.valueText, insertion.collectionText);
        acceptor.addIssue(new BslDirectLocationIssue(message,
            new DirectLocation(node.getOffset(), node.getLength(), node.getStartLine(), module)));
    }

    private Insertion insertion(Statement statement)
    {
        if (!(statement instanceof SimpleStatement simpleStatement) || simpleStatement.getRight() != null)
        {
            return null;
        }
        if (!(simpleStatement.getLeft() instanceof Invocation call))
        {
            return null;
        }
        if (!(call.getMethodAccess() instanceof DynamicFeatureAccess collectionAccess))
        {
            return null;
        }
        String methodName = collectionAccess.getName();
        if (methodName == null || !INSERT_METHOD_PATTERN.matcher(methodName).matches())
        {
            return null;
        }
        Expression collection = collectionAccess.getSource();
        if (collection == null)
        {
            return null;
        }
        List<Expression> arguments = call.getParams();
        if (arguments.isEmpty() || arguments.get(0) instanceof EmptyExpression)
        {
            return null;
        }
        Expression value = arguments.get(0);
        String valueText = tokens(value);
        String valueKey = normalized(value);
        if (valueText.isEmpty() || valueKey.isEmpty() || isBlankString(valueKey)
            || IGNORED_VALUE_PATTERN.matcher(valueKey).matches())
        {
            return null;
        }
        String collectionText = tokens(collection);
        String collectionKey = normalized(collection);
        if (collectionText.isEmpty() || collectionKey.isEmpty())
        {
            return null;
        }
        INode node = NodeModelUtils.findActualNodeFor(statement);
        int start = node == null ? -1 : node.getOffset();
        int end = node == null ? -1 : node.getOffset() + node.getLength();
        String groupKey = collectionKey + KEY_SEPARATOR + methodName.toLowerCase(Locale.ROOT) + KEY_SEPARATOR + valueKey;
        return new Insertion(statement, call, collectionText, collectionKey, valueText, valuePrefixes(value), groupKey,
            start, end);
    }

    /**
     * A pair of consecutive insertions must not be reported if something between them can change the value or the
     * collection: an assignment to them (or to their parent prefixes), a control-flow interruption, or another call
     * that receives them as an argument.
     */
    private boolean hasChange(EObject owner, Insertion first, Insertion second)
    {
        if (first.end < 0 || second.start < 0)
        {
            return true;
        }
        if (first.end >= second.start)
        {
            return true;
        }
        for (SimpleStatement statement : EcoreUtil2.getAllContentsOfType(owner, SimpleStatement.class))
        {
            if (statement.getRight() == null || statement.getLeft() == null
                || !isBetween(statement, first.end, second.start))
            {
                continue;
            }
            String left = normalized(statement.getLeft());
            if (left.isEmpty())
            {
                continue;
            }
            if (isCollectionOrParent(first.collectionKey, left) || relatesToValue(first.valuePrefixes, left))
            {
                return true;
            }
        }
        for (Statement statement : EcoreUtil2.getAllContentsOfType(owner, Statement.class))
        {
            if ((statement instanceof ReturnStatement || statement instanceof BreakStatement
                || statement instanceof ContinueStatement || statement instanceof RaiseStatement)
                && isBetween(statement, first.end, second.start))
            {
                return true;
            }
        }
        for (Invocation invocation : EcoreUtil2.getAllContentsOfType(owner, Invocation.class))
        {
            if (invocation == first.call || invocation == second.call
                || !isBetween(invocation, first.end, second.start))
            {
                continue;
            }
            for (Expression argument : invocation.getParams())
            {
                String root = rootIdentifier(argument);
                if (root == null)
                {
                    continue;
                }
                if (isCollectionOrParent(first.collectionKey, root) || relatesToValue(first.valuePrefixes, root))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isBetween(EObject object, int from, int to)
    {
        INode node = NodeModelUtils.findActualNodeFor(object);
        if (node == null)
        {
            return false;
        }
        int start = node.getOffset();
        return start > from && start < to;
    }

    private static boolean isCollectionOrParent(String collectionKey, String expression)
    {
        return collectionKey.equals(expression) || collectionKey.startsWith(expression + "."); //$NON-NLS-1$
    }

    private static boolean relatesToValue(Set<String> valuePrefixes, String expression)
    {
        for (String prefix : valuePrefixes)
        {
            if (prefix.equals(expression) || prefix.startsWith(expression + ".") //$NON-NLS-1$
                || expression.startsWith(prefix + ".")) //$NON-NLS-1$
            {
                return true;
            }
        }
        return false;
    }

    /**
     * All dotted prefixes of the value expression, as the LS collects them for a complex identifier
     * ({@code Данные.Метод().Коллекция} gives {@code Данные}, {@code Данные.Метод}, {@code Данные.Метод()},
     * {@code Данные.Метод().Коллекция}).
     */
    private static Set<String> valuePrefixes(Expression value)
    {
        Set<String> prefixes = new TreeSet<>();
        String full = normalized(value);
        if (!full.isEmpty())
        {
            prefixes.add(full);
        }
        for (FeatureAccess access : EcoreUtil2.getAllContentsOfType(value, FeatureAccess.class))
        {
            String text = normalized(access);
            if (!text.isEmpty())
            {
                prefixes.add(text);
            }
        }
        for (Invocation invocation : EcoreUtil2.getAllContentsOfType(value, Invocation.class))
        {
            String text = normalized(invocation);
            if (!text.isEmpty())
            {
                prefixes.add(text);
            }
        }
        return prefixes;
    }

    private static String rootIdentifier(Expression argument)
    {
        if (argument instanceof Invocation invocation)
        {
            return invocation.getMethodAccess() == null ? null : rootIdentifier(invocation.getMethodAccess());
        }
        if (argument instanceof DynamicFeatureAccess dynamicAccess)
        {
            return dynamicAccess.getSource() == null ? null : rootIdentifier(dynamicAccess.getSource());
        }
        if (argument instanceof StaticFeatureAccess staticAccess)
        {
            return normalized(staticAccess);
        }
        return null;
    }

    /**
     * LS-like text of an expression: significant tokens only, so whitespace and line breaks do not matter.
     */
    private static String tokens(EObject object)
    {
        INode node = NodeModelUtils.findActualNodeFor(object);
        if (node == null)
        {
            return ""; //$NON-NLS-1$
        }
        StringBuilder result = new StringBuilder();
        for (var leaf : node.getLeafNodes())
        {
            if (!leaf.isHidden())
            {
                result.append(leaf.getText());
            }
        }
        return result.toString();
    }

    /**
     * Comparison key of an expression: {@link #tokens(EObject)} with case folded outside string literals, because
     * identifiers are case-insensitive in BSL while string contents are not.
     */
    private static String normalized(EObject object)
    {
        INode node = NodeModelUtils.findActualNodeFor(object);
        if (node == null)
        {
            return ""; //$NON-NLS-1$
        }
        StringBuilder result = new StringBuilder();
        for (var leaf : node.getLeafNodes())
        {
            if (leaf.isHidden())
            {
                continue;
            }
            String text = leaf.getText();
            result.append(isStringLiteral(text) ? text : text.toLowerCase(Locale.ROOT));
        }
        return result.toString();
    }

    private static boolean isStringLiteral(String text)
    {
        return text.length() > 1 && text.charAt(0) == '"' && text.charAt(text.length() - 1) == '"';
    }

    private static boolean isBlankString(String text)
    {
        int length = text.length();
        return length >= 2 && text.charAt(0) == '"' && text.charAt(length - 1) == '"'
            && text.substring(1, length - 1).isBlank();
    }

    /**
     * Statement lists that correspond to LS code blocks: the module body, method bodies, if/elsif/else branches, loop
     * bodies and try/except bodies. Statements of different lists are never compared.
     */
    private static List<StatementList> blocks(Module module)
    {
        List<StatementList> blocks = new ArrayList<>();
        blocks.add(new StatementList(module, module.getStatements()));
        for (Method method : EcoreUtil2.getAllContentsOfType(module, Method.class))
        {
            blocks.add(new StatementList(method, method.getStatements()));
        }
        for (Conditional conditional : EcoreUtil2.getAllContentsOfType(module, Conditional.class))
        {
            blocks.add(new StatementList(conditional, conditional.getStatements()));
        }
        for (IfStatement ifStatement : EcoreUtil2.getAllContentsOfType(module, IfStatement.class))
        {
            blocks.add(new StatementList(ifStatement, ifStatement.getElseStatements()));
        }
        for (LoopStatement loop : EcoreUtil2.getAllContentsOfType(module, LoopStatement.class))
        {
            blocks.add(new StatementList(loop, loop.getStatements()));
        }
        for (TryExceptStatement tryExcept : EcoreUtil2.getAllContentsOfType(module, TryExceptStatement.class))
        {
            blocks.add(new StatementList(tryExcept, tryExcept.getTryStatements()));
            blocks.add(new StatementList(tryExcept, tryExcept.getExceptStatements()));
        }
        return blocks;
    }

    private static final class StatementList
    {
        final EObject owner;

        final List<Statement> statements;

        StatementList(EObject owner, List<Statement> statements)
        {
            this.owner = owner;
            this.statements = statements;
        }
    }

    private static final class Insertion
    {
        final Statement statement;

        final Invocation call;

        final String collectionText;

        final String collectionKey;

        final String valueText;

        final Set<String> valuePrefixes;

        final String groupKey;

        final int start;

        final int end;

        Insertion(Statement statement, Invocation call, String collectionText, String collectionKey, String valueText,
            Set<String> valuePrefixes, String groupKey, int start, int end)
        {
            this.statement = statement;
            this.call = call;
            this.collectionText = collectionText;
            this.collectionKey = collectionKey;
            this.valueText = valueText;
            this.valuePrefixes = valuePrefixes;
            this.groupKey = groupKey;
            this.start = start;
            this.end = end;
        }
    }
}
