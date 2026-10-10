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
 *     malikov-pro - initial API and implementation
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.dcs.util.DcsUtil;
import com._1c.g5.v8.dt.metadata.dbview.DbViewElement;
import com._1c.g5.v8.dt.metadata.dbview.DbViewFieldDef;
import com._1c.g5.v8.dt.ql.model.CastOperationExpression;
import com._1c.g5.v8.dt.ql.model.FieldWithCasting;
import com._1c.g5.v8.dt.ql.model.MultiPartCommonExpression;
import com._1c.g5.v8.dt.ql.model.QuerySchema;
import com._1c.g5.v8.dt.ql.typesystem.IDynamicDbViewFieldComputer;
import com.e1c.g5.v8.dt.testing.check.CheckTestBase;
import com.e1c.v8codestyle.internal.ql.CorePlugin;

/** Research only: resolve the field being dereferenced, not its final string type. */
public class CompositeDereferenceResearchTest
    extends CheckTestBase
{
    @Override
    protected boolean enableCleanUp()
    {
        return false;
    }

    /** @throws Exception if the EDT project cannot be loaded */
    @Test
    public void testSourceFieldTypes() throws Exception
    {
        QuerySchema query = parse(
            "SELECT P.SingleTarget.Description, P.DoubleTarget.Description, P.TripleTarget.Description " //$NON-NLS-1$
                + "FROM Catalog.Products AS P"); //$NON-NLS-1$
        IDynamicDbViewFieldComputer computer = CorePlugin.getDefault().getInjector()
            .getInstance(IDynamicDbViewFieldComputer.class);
        Map<String, Integer> counts = new HashMap<>();
        for (MultiPartCommonExpression expression : EcoreUtil2.getAllContentsOfType(query,
            MultiPartCommonExpression.class))
        {
            if (!"Description".equalsIgnoreCase(expression.getContent())) //$NON-NLS-1$
            {
                continue;
            }
            assertNotNull(expression.getSourceTable());
            DbViewElement source = computer.computeDbView(expression.getSourceTable());
            assertTrue(expression.getFullContent(), source instanceof DbViewFieldDef);
            DbViewFieldDef field = (DbViewFieldDef)source;
            assertNotNull(field.getType());
            counts.put(expression.getSourceTable().getContent(), field.getType().getTypes().size());
            System.out.println("QL source types: " + expression.getFullContent() + " -> " //$NON-NLS-1$ //$NON-NLS-2$
                + field.getType().getTypes());
        }
        assertEquals(Map.of("SingleTarget", 1, "DoubleTarget", 2, "TripleTarget", 3), counts); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    /** @throws Exception if the EDT project cannot be loaded */
    @Test
    public void testVirtualTableAndRussianAlias() throws Exception
    {
        QuerySchema query = parse("ВЫБРАТЬ Остатки.Product.TripleTarget.Description " //$NON-NLS-1$
            + "ИЗ РегистрНакопления.Stocks.Остатки КАК Остатки"); //$NON-NLS-1$
        IDynamicDbViewFieldComputer computer = computer();
        MultiPartCommonExpression description = EcoreUtil2.getAllContentsOfType(query,
            MultiPartCommonExpression.class).stream()
            .filter(expression -> "Description".equals(expression.getContent())).findFirst().orElseThrow(); //$NON-NLS-1$
        DbViewElement source = computer.computeDbView(description.getSourceTable());
        assertTrue(source instanceof DbViewFieldDef);
        assertEquals(3, ((DbViewFieldDef)source).getType().getTypes().size());
        MultiPartCommonExpression target = (MultiPartCommonExpression)description.getSourceTable();
        DbViewElement product = computer.computeDbView(target.getSourceTable());
        assertTrue(product instanceof DbViewFieldDef);
        assertEquals(1, ((DbViewFieldDef)product).getType().getTypes().size());
    }

    /** @throws Exception if the EDT project cannot be loaded */
    @Test
    public void testCastHasDedicatedAstNode() throws Exception
    {
        QuerySchema query = parse("SELECT CAST(P.TripleTarget AS Catalog.Products).Description " //$NON-NLS-1$
            + "FROM Catalog.Products AS P"); //$NON-NLS-1$
        var casts = EcoreUtil2.getAllContentsOfType(query, CastOperationExpression.class);
        var fields = EcoreUtil2.getAllContentsOfType(query, FieldWithCasting.class);
        assertEquals(1, casts.size());
        assertEquals(1, fields.size());
        assertEquals(casts.get(0), fields.get(0).getCastOperation());
        DbViewElement source = computer().computeDbView(casts.get(0).getExpression());
        assertTrue(source instanceof DbViewFieldDef);
        assertEquals(3, ((DbViewFieldDef)source).getType().getTypes().size());
        assertTrue(EcoreUtil2.getAllContentsOfType(query, MultiPartCommonExpression.class).stream()
            .noneMatch(expression -> "Description".equals(expression.getContent()))); //$NON-NLS-1$
    }

    /** @throws Exception if the EDT project cannot be loaded */
    @Test
    public void testUnknownFieldCannotProveCompositeType() throws Exception
    {
        QuerySchema query = parse("SELECT P.UnknownTarget.Description FROM Catalog.Products AS P"); //$NON-NLS-1$
        MultiPartCommonExpression description = EcoreUtil2.getAllContentsOfType(query,
            MultiPartCommonExpression.class).stream()
            .filter(expression -> "Description".equals(expression.getContent())).findFirst().orElseThrow(); //$NON-NLS-1$
        DbViewElement source = computer().computeDbView(description.getSourceTable());
        assertTrue("An unresolved field must not be treated as a typed reference", //$NON-NLS-1$
            !(source instanceof DbViewFieldDef field) || field.getType() == null
                || field.getType().getTypes().isEmpty());
    }

    private IDynamicDbViewFieldComputer computer()
    {
        return CorePlugin.getDefault().getInjector().getInstance(IDynamicDbViewFieldComputer.class);
    }

    /** @throws Exception if the EDT project cannot be loaded */
    @Test
    public void testMixedAndAnyReferenceTypes() throws Exception
    {
        QuerySchema query = parse("SELECT P.MixedTarget.Description, P.AnyTarget.Description " //$NON-NLS-1$
            + "FROM Catalog.Products AS P"); //$NON-NLS-1$
        Map<String, Integer> counts = new HashMap<>();
        for (MultiPartCommonExpression expression : EcoreUtil2.getAllContentsOfType(query,
            MultiPartCommonExpression.class))
        {
            if (!"Description".equals(expression.getContent())) //$NON-NLS-1$
            {
                continue;
            }
            DbViewElement source = computer().computeDbView(expression.getSourceTable());
            assertTrue(source instanceof DbViewFieldDef);
            var types = ((DbViewFieldDef)source).getType().getTypes();
            counts.put(expression.getSourceTable().getContent(), types.size());
            for (var type : types)
            {
                assertTrue("Type must be resolved", !type.eIsProxy()); //$NON-NLS-1$
            }
            var names = types.stream().map(type -> type.getName()).toList();
            System.out.println("QL type names: " + expression.getFullContent() + " -> " + names); //$NON-NLS-1$ //$NON-NLS-2$
            if ("MixedTarget".equals(expression.getSourceTable().getContent())) //$NON-NLS-1$
            {
                assertEquals(1, names.stream().filter(name -> name.startsWith("CatalogRef.")).count()); //$NON-NLS-1$
                assertEquals(List.of("CatalogRef.Products", "String", "Number"), names); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            else
            {
                assertEquals(List.of("AnyRef"), names); //$NON-NLS-1$
            }
        }
        assertEquals(Map.of("MixedTarget", 3, "AnyTarget", 1), counts); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private QuerySchema parse(String source) throws Exception
    {
        var workspaceProject = testingWorkspace.getProject("QlFullDemo"); //$NON-NLS-1$
        IDtProject project = workspaceProject.exists() && workspaceProject.isAccessible()
            ? dtProjectManager.getDtProject(workspaceProject)
            : openProjectAndWaitForValidationFinish("QlFullDemo"); //$NON-NLS-1$
        assertNotNull(project);
        QuerySchema query = DcsUtil.getQuerySchema(source, project);
        assertNotNull(query);
        INode node = NodeModelUtils.findActualNodeFor(query);
        assertNotNull(node);
        for (INode leaf : node.getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertNotNull(query.eResource());
        assertTrue(query.eResource().getErrors().toString(), query.eResource().getErrors().isEmpty());
        return query;
    }
}
