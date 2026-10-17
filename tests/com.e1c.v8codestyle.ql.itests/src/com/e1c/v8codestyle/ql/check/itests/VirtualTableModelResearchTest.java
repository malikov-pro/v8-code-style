/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check.itests;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.ql.model.QuerySchemaTable;
import com._1c.g5.v8.dt.metadata.dbview.DbViewSelectDef;
import com._1c.g5.v8.dt.metadata.dbview.DbViewTableDef;
import com._1c.g5.v8.dt.ql.typesystem.IDynamicDbViewFieldComputer;
import com.e1c.v8codestyle.internal.ql.CorePlugin;
import com.e1c.v8codestyle.ql.check.VirtualTableFiltersCheck;

/** One-question research: resolved virtual-table kind and preserved parameter positions. */
public class VirtualTableModelResearchTest extends AbstractQueryTestBase
{
    public VirtualTableModelResearchTest()
    {
        super(VirtualTableFiltersCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "QlFullDemo"; //$NON-NLS-1$
    }

    @Test
    public void testCriterionAndSubcontoRepresentation() throws Exception
    {
        var schema = loadQueryAndValidate(FOLDER_RESOURCE + "virtual-table-exemptions-research.ql"); //$NON-NLS-1$
        for (var leaf : NodeModelUtils.getNode(schema).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        var computer = CorePlugin.getDefault().getInjector().getInstance(IDynamicDbViewFieldComputer.class);
        var tables = EcoreUtil2.getAllContentsOfType(schema, QuerySchemaTable.class);
        assertEquals(4, tables.size());
        for (int i = 0; i < tables.size(); i++)
        {
            var table = tables.get(i);
            var db = computer.computeDbView(table.getTable());
            org.junit.Assert.assertTrue(i == 2 ? db instanceof DbViewTableDef : db instanceof DbViewSelectDef);
            if (i == 2)
            {
                assertEquals("extDimensionsView", db.eContainingFeature().getName()); //$NON-NLS-1$
            }
            else
            {
                var inner = table.getParameters().get(0).getExpression().getExpression();
                org.junit.Assert.assertTrue(i == 1
                    ? inner instanceof com._1c.g5.v8.dt.ql.model.ParameterExpression
                    : inner instanceof com._1c.g5.v8.dt.ql.model.EmptyExpression);
            }
        }
    }

    @Test
    public void testResolvedTableAndParameters() throws Exception
    {
        var schema = loadQueryAndValidate(FOLDER_RESOURCE + "virtual-table-model-research.ql"); //$NON-NLS-1$
        for (var leaf : NodeModelUtils.getNode(schema).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        var computer = CorePlugin.getDefault().getInjector().getInstance(IDynamicDbViewFieldComputer.class);
        var tables = EcoreUtil2.getAllContentsOfType(schema, QuerySchemaTable.class);
        assertEquals(9, tables.size());
        for (int i = 0; i < tables.size(); i++)
        {
            var table = tables.get(i);
            var db = computer.computeDbView(table.getTable());
            boolean physical = i == 4 || i == 7 || i == 8;
            org.junit.Assert.assertTrue(physical ? db instanceof DbViewTableDef : db instanceof DbViewSelectDef);
            assertEquals(i == 2 || i == 3 ? 2 : physical ? 0 : 1, table.getParameters().size());
        }
    }
}
