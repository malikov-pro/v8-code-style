/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.metadata.dbview.DbViewElement;
import com._1c.g5.v8.dt.ql.model.AbstractQuerySchemaSource;
import com._1c.g5.v8.dt.ql.model.AbstractQuerySchemaTable;
import com._1c.g5.v8.dt.ql.model.MultiPartCommonExpression;
import com._1c.g5.v8.dt.ql.model.NestedTableExpression;
import com._1c.g5.v8.dt.ql.model.QuerySchemaOperator;
import com._1c.g5.v8.dt.ql.model.QuerySchemaSelectQuery;
import com._1c.g5.v8.dt.ql.model.QuerySchemaSource;
import com._1c.g5.v8.dt.ql.model.QuerySchemaTable;
import com._1c.g5.v8.dt.ql.typesystem.IDynamicDbViewFieldComputer;
import com.e1c.v8codestyle.internal.ql.CorePlugin;
import com.e1c.v8codestyle.ql.check.UnionAllCheck;

/**
 * One-question research for RefOveruse: which identifiers address a data source
 * and how a tabular-section or virtual-table source is distinguishable.
 */
public class RefOveruseModelResearchTest
    extends AbstractQueryTestBase
{
    public RefOveruseModelResearchTest()
    {
        // Any registered ql check starts the base class; the research only reads the model.
        super(UnionAllCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "QlFullDemo"; //$NON-NLS-1$
    }

    @Test
    public void testSourceResolution() throws Exception
    {
        var schema = loadQueryAndValidate(FOLDER_RESOURCE + "ql-ref-overuse-model-research.ql"); //$NON-NLS-1$
        for (var leaf : NodeModelUtils.getNode(schema).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        var computer = CorePlugin.getDefault().getInjector().getInstance(IDynamicDbViewFieldComputer.class);
        var operator = EcoreUtil2.getAllContentsOfType(schema, QuerySchemaOperator.class).get(0);
        assertNotNull(operator);
        java.util.List<QuerySchemaSource> sources = new java.util.ArrayList<>(operator.getSources());
        for (int i = 0; i < sources.size(); i++)
        {
            for (var join : sources.get(i).getJoins())
            {
                if (join instanceof com._1c.g5.v8.dt.ql.model.QuerySchemaQuerySourceJoin joined)
                {
                    sources.add(joined.getSource());
                }
            }
        }
        assertEquals(3, sources.size());
        for (QuerySchemaSource source : sources)
        {
            AbstractQuerySchemaSource inner = source.getSource();
            assertNotNull(inner);
            StringBuilder line = new StringBuilder("QL source: "); //$NON-NLS-1$
            line.append(inner.getClass().getSimpleName()).append(" alias=").append(inner.getAlias()); //$NON-NLS-1$
            if (inner instanceof QuerySchemaTable table)
            {
                AbstractQuerySchemaTable model = table.getTable();
                line.append(" tableModel=").append(model == null ? "null" : model.getClass().getSimpleName()); //$NON-NLS-1$ //$NON-NLS-2$
                if (model != null)
                {
                    line.append(" tableName=").append(model.getTableName()) //$NON-NLS-1$
                        .append(" fullTableName=").append(model.getFullTableName()); //$NON-NLS-1$
                    DbViewElement direct = model.getTableDbView();
                    line.append(" tableDbView=").append(direct == null ? "null" : direct.eClass().getName()); //$NON-NLS-1$ //$NON-NLS-2$
                    if (direct != null && direct.getMdObject() != null)
                    {
                        line.append(" mdObject=").append(direct.getMdObject().eClass().getName()); //$NON-NLS-1$
                    }
                    try
                    {
                        DbViewElement computed = computer.computeDbView(model);
                        line.append(" computed=").append(computed == null ? "null" : computed.eClass().getName()); //$NON-NLS-1$ //$NON-NLS-2$
                        if (computed != null && computed.getMdObject() != null)
                        {
                            line.append(" computedMd=").append(computed.getMdObject().eClass().getName()); //$NON-NLS-1$
                        }
                    }
                    catch (RuntimeException e)
                    {
                        line.append(" computed=EX(").append(e.getClass().getSimpleName()).append(')'); //$NON-NLS-1$
                    }
                }
            }
            System.out.println(line);
        }
        var query = EcoreUtil2.getContainerOfType(operator, QuerySchemaSelectQuery.class);
        assertNotNull(query);
        assertTrue(!operator.getSources().isEmpty());
    }

    @Test
    public void testInlineTableAndNestedQueryBoundaries() throws Exception
    {
        var inline = loadQueryAndValidate(FOLDER_RESOURCE + "ql-ref-overuse-inline-table.ql"); //$NON-NLS-1$
        var inlineFields = EcoreUtil2.getAllContentsOfType(inline, MultiPartCommonExpression.class);
        assertTrue(inlineFields.stream().anyMatch(field -> field.getFullContent() != null
            && field.getFullContent().contains("Ref"))); //$NON-NLS-1$
        assertTrue(inlineFields.stream().allMatch(
            field -> EcoreUtil2.getContainerOfType(field, NestedTableExpression.class) != null));

        var nested = loadQueryAndValidate(FOLDER_RESOURCE + "ql-ref-overuse-nested-query.ql"); //$NON-NLS-1$
        var queries = EcoreUtil2.getAllContentsOfType(nested, QuerySchemaSelectQuery.class);
        assertEquals(2, queries.size());
        var innerField = EcoreUtil2.getAllContentsOfType(nested, MultiPartCommonExpression.class).stream()
            .filter(field -> "Док2.Ref.Description".equals(field.getFullContent())) //$NON-NLS-1$
            .findFirst()
            .orElseThrow();
        assertTrue(EcoreUtil2.getContainerOfType(innerField, QuerySchemaSelectQuery.class) != queries.get(0));
    }
}
