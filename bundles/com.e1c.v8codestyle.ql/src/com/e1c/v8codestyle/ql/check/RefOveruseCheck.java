/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check;

import static com._1c.g5.v8.dt.ql.model.QlPackage.Literals.MULTI_PART_COMMON_EXPRESSION__SOURCE_TABLE;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.metadata.dbview.DbViewElement;
import com._1c.g5.v8.dt.metadata.dbview.DbViewSelectDef;
import com._1c.g5.v8.dt.metadata.mdclass.BasicTabularSection;
import com._1c.g5.v8.dt.ql.model.AbstractQuerySchemaSource;
import com._1c.g5.v8.dt.ql.model.MultiPartCommonExpression;
import com._1c.g5.v8.dt.ql.model.NestedTableExpression;
import com._1c.g5.v8.dt.ql.model.QuerySchemaOperator;
import com._1c.g5.v8.dt.ql.model.QuerySchemaQuerySourceJoin;
import com._1c.g5.v8.dt.ql.model.QuerySchemaSelectQuery;
import com._1c.g5.v8.dt.ql.model.QuerySchemaSource;
import com._1c.g5.v8.dt.ql.model.QuerySchemaTable;
import com._1c.g5.v8.dt.ql.typesystem.IDynamicDbViewFieldComputer;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.g5.v8.dt.ql.check.QlBasicDelegateCheck;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.ql.CorePlugin;
import com.google.inject.Inject;

/**
 * Reports a redundant {@code .Ref}/{@code .Ссылка} step in a field path.
 * <p>
 * Port of the BSL Language Server diagnostic RefOveruse: reading {@code .Ref}
 * of a reference field makes the DBMS join the referenced table for nothing.
 * The field is found by the last {@code Ссылка|Ref|Reference} segment of the path:
 * <ul>
 * <li>a path that ends with it is reported unless the previous segment is a
 * data source of the same query ({@code Table.Ref} is the table reference
 * itself);</li>
 * <li>{@code Ref} as the first segment of a longer path is not reported;</li>
 * <li>{@code Table.Ref.Attribute} is not reported when the table is a tabular
 * section, a virtual table or an unresolved table ({@code Table.Ref} there is
 * the owner reference of the tabular section);</li>
 * <li>{@code Ref} in a later position is always reported.</li>
 * </ul>
 * Fields of nested queries are checked as their own query; fields inside an
 * inline table constructor are not checked, as in the pinned LS.
 */
public class RefOveruseCheck
    extends QlBasicDelegateCheck
{
    /** Identifier of the standards-oriented query check. */
    public static final String CHECK_ID = "ql-ref-overuse"; //$NON-NLS-1$

    private static final Pattern REF_PATTERN =
        Pattern.compile("Ссылка|Ref|Reference", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE); //$NON-NLS-1$

    private static final String DOT = "."; //$NON-NLS-1$

    private static final int MIN_PATH_SEGMENTS = 2;

    private final IDynamicDbViewFieldComputer computer;

    /**
     * @param computer field model computer used to classify a data source
     */
    @Inject
    public RefOveruseCheck(IDynamicDbViewFieldComputer computer)
    {
        this.computer = computer;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.RefOveruseCheck_title)
            .description(Messages.RefOveruseCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.PERFORMANCE)
            .extension(new CommonSenseCheckExtension(CHECK_ID, CorePlugin.PLUGIN_ID))
            .delegate(QuerySchemaSelectQuery.class);
    }

    @Override
    protected void checkQlObject(EObject object, QueryOwner owner, IQlResultAcceptor resultAcceptor,
        ICheckParameters parameters, IProgressMonitor monitor)
    {
        QuerySchemaSelectQuery query = (QuerySchemaSelectQuery)object;
        Map<String, QuerySchemaSource> sources = sources(query);
        for (MultiPartCommonExpression field : EcoreUtil2.getAllContentsOfType(query,
            MultiPartCommonExpression.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            // A dotted path is a chain of multi parts; the outermost one holds the whole path.
            if (field.eContainer() instanceof MultiPartCommonExpression)
            {
                continue;
            }
            if (EcoreUtil2.getContainerOfType(field, QuerySchemaSelectQuery.class) != query
                || EcoreUtil2.getContainerOfType(field, NestedTableExpression.class) != null)
            {
                continue;
            }
            String path = field.getFullContent();
            if (path == null || !overused(path, sources))
            {
                continue;
            }
            String message = MessageFormat.format(Messages.RefOveruseCheck_message, path);
            INode node = NodeModelUtils.findActualNodeFor(field);
            if (node != null)
            {
                resultAcceptor.addIssue(message, node.getStartLine(), node.getOffset(), node.getLength());
            }
            else
            {
                resultAcceptor.addIssue(message, field, MULTI_PART_COMMON_EXPRESSION__SOURCE_TABLE);
            }
        }
    }

    /**
     * Tells whether the path contains a redundant {@code .Ref} step.
     *
     * @param path full field path, not {@code null}
     * @param sources query data sources keyed by the identifier used in the query
     * @return {@code true} when the path overuses the reference field
     */
    private boolean overused(String path, Map<String, QuerySchemaSource> sources)
    {
        String[] segments = path.split("\\" + DOT, -1); //$NON-NLS-1$
        if (segments.length < MIN_PATH_SEGMENTS)
        {
            return false;
        }
        int lastRef = -1;
        for (int i = segments.length - 1; i >= 0; i--)
        {
            if (REF_PATTERN.matcher(unquote(segments[i])).matches())
            {
                lastRef = i;
                break;
            }
        }
        if (lastRef < 0)
        {
            return false;
        }
        int last = segments.length - 1;
        if (lastRef == last)
        {
            return !sources.containsKey(name(segments[last - 1]));
        }
        if (lastRef == 0)
        {
            return false;
        }
        if (lastRef == 1)
        {
            return !isTabularSectionLike(sources.get(name(segments[0])));
        }
        return true;
    }

    private static Map<String, QuerySchemaSource> sources(QuerySchemaSelectQuery query)
    {
        Map<String, QuerySchemaSource> sources = new HashMap<>();
        for (QuerySchemaOperator operator : query.getOperators())
        {
            List<QuerySchemaSource> queue = new ArrayList<>(operator.getSources());
            for (int i = 0; i < queue.size(); i++)
            {
                QuerySchemaSource source = queue.get(i);
                addSource(sources, source);
                for (var join : source.getJoins())
                {
                    if (join instanceof QuerySchemaQuerySourceJoin joined && joined.getSource() != null)
                    {
                        queue.add(joined.getSource());
                    }
                }
            }
        }
        return sources;
    }

    private static void addSource(Map<String, QuerySchemaSource> sources, QuerySchemaSource source)
    {
        AbstractQuerySchemaSource inner = source.getSource();
        if (inner == null)
        {
            return;
        }
        String name = inner.getAlias();
        if (name == null && inner instanceof QuerySchemaTable table && table.getTable() != null)
        {
            name = table.getTable().getTableName();
        }
        if (name != null)
        {
            sources.putIfAbsent(name(name), source);
        }
    }

    /**
     * Tells whether {@code .Ref} of the source is the owner reference rather than
     * a redundant dereference: a tabular section, a virtual table or a table
     * without resolved metadata (the pinned LS treats those the same way).
     *
     * @param source data source resolved by the first path segment, may be {@code null}
     * @return {@code true} when the source is tabular-section like
     */
    private boolean isTabularSectionLike(QuerySchemaSource source)
    {
        if (source == null)
        {
            return false;
        }
        AbstractQuerySchemaSource inner = source.getSource();
        if (!(inner instanceof QuerySchemaTable table) || table.getTable() == null)
        {
            // A parameter table or a nested query has no tabular sections.
            return false;
        }
        DbViewElement view;
        try
        {
            view = computer.computeDbView(table.getTable());
        }
        catch (RuntimeException e)
        {
            // An unresolved table may be a tabular section; do not report it.
            return true;
        }
        if (view == null)
        {
            return true;
        }
        return view instanceof DbViewSelectDef || view.getMdObject() instanceof BasicTabularSection;
    }

    private static String unquote(String segment)
    {
        return segment.replace("\"", "").trim(); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static String name(String segment)
    {
        return unquote(segment).toLowerCase(Locale.ROOT);
    }
}
