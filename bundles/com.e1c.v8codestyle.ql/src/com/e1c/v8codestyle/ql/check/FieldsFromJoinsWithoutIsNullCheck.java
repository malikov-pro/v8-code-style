/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check;

import static com._1c.g5.v8.dt.ql.model.QlPackage.Literals.QUERY_SCHEMA_QUERY_SOURCE_JOIN__SOURCE;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.ql.model.AbstractExpression;
import com._1c.g5.v8.dt.ql.model.IsNullOperatorExpression;
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
 * ISNULL/ЕСТЬNULL guard. Opt-in, ERROR / CRITICAL, follows the pinned LS
 * invariant for the first vertical: a LEFT/RIGHT/FULL OUTER join alias used in
 * SELECT, WHERE, HAVING or grouping without an enclosing ISNULL node is
 * reported on the join. The ЕСТЬNULL function form is not covered in v1.
 */
public class FieldsFromJoinsWithoutIsNullCheck
    extends QlBasicDelegateCheck
{
    private static final String CHECK_ID = "ql-fields-from-joins-without-isnull"; //$NON-NLS-1$
    private static final String IS_NULL_FUNCTION = "ЕСТЬNULL|ISNULL"; //$NON-NLS-1$

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
        if (operator == null)
        {
            return;
        }
        List<QuerySchemaExpression> sources = new ArrayList<>(operator.getSelectFields());
        if (operator.getFilters() != null)
        {
            sources.add(operator.getFilters());
        }
        if (operator.getHavingExpression() != null)
        {
            sources.add(operator.getHavingExpression());
        }
        sources.addAll(operator.getGrouping());
        for (QuerySchemaExpression source : sources)
        {
            if (monitor.isCanceled())
            {
                return;
            }
            AbstractExpression expression = source.getExpression();
            if (expression == null)
            {
                continue;
            }
            for (MultiPartCommonExpression field : EcoreUtil2.getAllContentsOfType(expression,
                MultiPartCommonExpression.class))
            {
                String content = field.getFullContent();
                if (content == null || !startsWithAlias(content, aliases) || guarded(field, source))
                {
                    continue;
                }
                resultAceptor.addIssue(Messages.FieldsFromJoinsWithoutIsNullCheck_message, join,
                    QUERY_SCHEMA_QUERY_SOURCE_JOIN__SOURCE);
                return;
            }
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

    private static boolean startsWithAlias(String content, List<String> aliases)
    {
        String lower = content.toLowerCase(Locale.ROOT);
        for (String alias : aliases)
        {
            if (lower.startsWith(alias + ".")) //$NON-NLS-1$
            {
                return true;
            }
        }
        return false;
    }

    private static boolean guarded(EObject field, QuerySchemaExpression boundary)
    {
        for (EObject parent = field.eContainer(); parent != null && parent != boundary; parent = parent.eContainer())
        {
            if (parent instanceof IsNullOperatorExpression)
            {
                return true;
            }
        }
        return false;
    }
}
