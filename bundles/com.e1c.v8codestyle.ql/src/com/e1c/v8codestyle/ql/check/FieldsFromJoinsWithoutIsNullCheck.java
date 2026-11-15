/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check;

import static com._1c.g5.v8.dt.ql.model.QlPackage.Literals.QUERY_SCHEMA_QUERY_SOURCE_JOIN__SOURCE;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.mcore.NamedElement;
import com._1c.g5.v8.dt.ql.model.AbstractExpression;
import com._1c.g5.v8.dt.ql.model.AbstractQuerySchemaQuerySourceJoin;
import com._1c.g5.v8.dt.ql.model.FunctionInvocationExpression;
import com._1c.g5.v8.dt.ql.model.IsNullOperatorExpression;
import com._1c.g5.v8.dt.ql.model.LogicalNOTOperatorExpression;
import com._1c.g5.v8.dt.ql.model.MultiPartCommonExpression;
import com._1c.g5.v8.dt.ql.model.QuerySchemaExpression;
import com._1c.g5.v8.dt.ql.model.QuerySchemaJoinType;
import com._1c.g5.v8.dt.ql.model.QuerySchemaOperator;
import com._1c.g5.v8.dt.ql.model.QuerySchemaQuerySourceJoin;
import com._1c.g5.v8.dt.ql.model.QuerySchemaSource;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.g5.v8.dt.ql.check.QlBasicDelegateCheck;
import com.e1c.v8codestyle.check.OptInCheckExtension;

/**
 * Reports fields read from the nullable side of an outer join without an
 * ISNULL/ЕСТЬNULL guard.
 * <p>
 * Port of the BSL Language Server diagnostic FieldsFromJoinsWithoutIsNull
 * (opt-in, ERROR / CRITICAL). Boundaries follow the pinned LS diagnostic: the
 * selected fields and the WHERE condition of the query are checked together
 * with the conditions of the other joins of the same data source; HAVING and
 * grouping are not inspected upstream and are not inspected here. A field is
 * guarded by an enclosing ЕСТЬNULL/ISNULL function call or by an IS NULL /
 * ЕСТЬ NULL comparison; when the WHERE condition proves any non-null check
 * ({@code ЕСТЬ НЕ NULL}, {@code НЕ (... ЕСТЬ NULL)}) the whole query is
 * skipped, exactly as upstream does.
 * <p>
 * One marker per outer join is reported (LS reports one diagnostic with
 * related locations); the message lists the fields found.
 */
public class FieldsFromJoinsWithoutIsNullCheck
    extends QlBasicDelegateCheck
{
    private static final String CHECK_ID = "ql-fields-from-joins-without-isnull"; //$NON-NLS-1$

    private static final Set<String> IS_NULL_FUNCTIONS = Set.of("ЕСТЬNULL", "ISNULL"); //$NON-NLS-1$ //$NON-NLS-2$

    private static final int MESSAGE_FIELD_LIMIT = 3;

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.FieldsFromJoinsWithoutIsNullCheck_title)
            .description(Messages.FieldsFromJoinsWithoutIsNullCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.ERROR)
            .extension(new OptInCheckExtension())
            .delegate(QuerySchemaQuerySourceJoin.class);
    }

    @Override
    protected void checkQlObject(EObject object, QueryOwner owner, IQlResultAcceptor resultAceptor,
        ICheckParameters parameters, IProgressMonitor monitor)
    {
        QuerySchemaQuerySourceJoin join = (QuerySchemaQuerySourceJoin)object;
        QuerySchemaJoinType type = join.getJoinType();
        if (type != QuerySchemaJoinType.LEFT_OUTER && type != QuerySchemaJoinType.RIGHT_OUTER
            && type != QuerySchemaJoinType.FULL_OUTER)
        {
            return;
        }
        List<String> aliases = aliases(join, type);
        if (aliases.isEmpty())
        {
            return;
        }
        QuerySchemaOperator operator = EcoreUtil2.getContainerOfType(join, QuerySchemaOperator.class);
        if (operator == null || hasNonNullFilter(operator.getFilters(), operator))
        {
            return;
        }
        Set<String> fields = new LinkedHashSet<>();
        for (QuerySchemaExpression selected : operator.getSelectFields())
        {
            if (monitor.isCanceled())
            {
                return;
            }
            checkExpression(selected, operator, aliases, true, fields);
        }
        checkExpression(operator.getFilters(), operator, aliases, true, fields);
        for (QuerySchemaSource source : operator.getSources())
        {
            for (AbstractQuerySchemaQuerySourceJoin other : source.getJoins())
            {
                if (other != join && other instanceof QuerySchemaQuerySourceJoin otherJoin)
                {
                    checkExpression(otherJoin.getCondition(), operator, aliases, false, fields);
                }
            }
        }
        if (!fields.isEmpty() && !monitor.isCanceled())
        {
            resultAceptor.addIssue(message(fields), join, QUERY_SCHEMA_QUERY_SOURCE_JOIN__SOURCE);
        }
    }

    private static List<String> aliases(QuerySchemaQuerySourceJoin join, QuerySchemaJoinType type)
    {
        List<String> aliases = new ArrayList<>();
        if (type == QuerySchemaJoinType.LEFT_OUTER || type == QuerySchemaJoinType.FULL_OUTER)
        {
            addAlias(aliases, join.getSource());
        }
        if (type == QuerySchemaJoinType.RIGHT_OUTER || type == QuerySchemaJoinType.FULL_OUTER)
        {
            addAlias(aliases, join.eContainer() instanceof QuerySchemaSource parent ? parent : null);
        }
        return aliases;
    }

    private static void addAlias(List<String> aliases, QuerySchemaSource source)
    {
        if (source != null && source.getSource() != null && source.getSource().getAlias() != null)
        {
            aliases.add(source.getSource().getAlias().toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Collects unguarded field paths of the nullable side of the outer join.
     *
     * @param boundary expression wrapper that limits the guard search
     * @param operator the query whose fields are inspected; fields of nested
     *            queries are their own query (pinned LS semantics)
     * @param aliases nullable aliases in lower case
     * @param isNullCounts whether an IS NULL/EСTЬ NULL comparison guards the field
     *            (false inside join conditions, as in the pinned LS)
     * @param fields collected field paths
     */
    private static void checkExpression(QuerySchemaExpression boundary, QuerySchemaOperator operator,
        List<String> aliases, boolean isNullCounts, Set<String> fields)
    {
        if (boundary == null)
        {
            return;
        }
        AbstractExpression expression = boundary.getExpression();
        if (expression == null)
        {
            return;
        }
        List<MultiPartCommonExpression> candidates = new ArrayList<>();
        // A plain selected field (T.Description) is the expression root itself; the
        // contents traversal below does not return its root.
        if (expression instanceof MultiPartCommonExpression root)
        {
            candidates.add(root);
        }
        candidates.addAll(EcoreUtil2.getAllContentsOfType(expression, MultiPartCommonExpression.class));
        for (MultiPartCommonExpression field : candidates)
        {
            // A dotted path is a chain of multi parts; the outermost one holds the whole path.
            if (field.eContainer() instanceof MultiPartCommonExpression)
            {
                continue;
            }
            if (EcoreUtil2.getContainerOfType(field, QuerySchemaOperator.class) != operator)
            {
                continue;
            }
            String content = field.getFullContent();
            String alias = leadingAlias(content);
            if (alias == null || !aliases.contains(alias) || guarded(field, boundary, isNullCounts))
            {
                continue;
            }
            fields.add(content);
        }
    }

    private static String leadingAlias(String content)
    {
        if (content == null)
        {
            return null;
        }
        int dot = content.indexOf('.');
        if (dot <= 0 || dot == content.length() - 1)
        {
            return null;
        }
        return content.substring(0, dot).toLowerCase(Locale.ROOT);
    }

    private static boolean guarded(EObject field, EObject boundary, boolean isNullCounts)
    {
        for (EObject parent = field.eContainer(); parent != null && parent != boundary; parent = parent.eContainer())
        {
            if (parent instanceof FunctionInvocationExpression invocation && isNullFunction(invocation))
            {
                return true;
            }
            if (isNullCounts && parent instanceof IsNullOperatorExpression)
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isNullFunction(FunctionInvocationExpression invocation)
    {
        NamedElement function = invocation.getFunctionType();
        if (function == null)
        {
            return false;
        }
        String name = function.getName();
        return name != null && IS_NULL_FUNCTIONS.contains(name.toUpperCase(Locale.ROOT));
    }

    /**
     * Tells whether the WHERE condition proves a non-null check of some field:
     * {@code ЕСТЬ НЕ NULL} or a negation of an {@code ЕСТЬ NULL} comparison. The
     * pinned LS skips the whole query in this case.
     *
     * @param filters WHERE condition of the query, may be {@code null}
     * @param operator the query that owns the condition
     * @return {@code true} when the query must not be reported
     */
    private static boolean hasNonNullFilter(QuerySchemaExpression filters, QuerySchemaOperator operator)
    {
        AbstractExpression expression = filters == null ? null : filters.getExpression();
        if (expression == null)
        {
            return false;
        }
        List<IsNullOperatorExpression> checks = new ArrayList<>();
        // A sole comparison is the expression root itself; the contents
        // traversal below does not return its root.
        if (expression instanceof IsNullOperatorExpression root)
        {
            checks.add(root);
        }
        checks.addAll(EcoreUtil2.getAllContentsOfType(expression, IsNullOperatorExpression.class));
        for (IsNullOperatorExpression isNull : checks)
        {
            if (EcoreUtil2.getContainerOfType(isNull, QuerySchemaOperator.class) != operator)
            {
                // A non-null check of a nested query does not exempt this one.
                continue;
            }
            if (isNull.isIsNot())
            {
                return true;
            }
            for (EObject parent = isNull.eContainer(); parent != null
                && parent != filters; parent = parent.eContainer())
            {
                if (parent instanceof LogicalNOTOperatorExpression)
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static String message(Set<String> fields)
    {
        List<String> names = new ArrayList<>(fields);
        String listed = String.join(", ", names.subList(0, Math.min(MESSAGE_FIELD_LIMIT, names.size()))); //$NON-NLS-1$
        if (names.size() > MESSAGE_FIELD_LIMIT)
        {
            listed = MessageFormat.format("{0}, ...", listed); //$NON-NLS-1$
        }
        return MessageFormat.format(Messages.FieldsFromJoinsWithoutIsNullCheck_message, listed);
    }
}
