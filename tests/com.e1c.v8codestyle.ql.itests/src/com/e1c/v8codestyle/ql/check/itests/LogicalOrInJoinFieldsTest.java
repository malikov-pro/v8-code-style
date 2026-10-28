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
import static org.junit.Assert.assertNull;

import java.util.List;

import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.ql.model.QuerySchema;
import com.e1c.v8codestyle.ql.check.LogicalOrInJoinCheck;

/** LS distinct-column exemptions and logical-expression/query boundaries. */
public class LogicalOrInJoinFieldsTest
    extends AbstractQueryTestBase
{
    public LogicalOrInJoinFieldsTest()
    {
        super(LogicalOrInJoinCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "QlFullDemo"; //$NON-NLS-1$
    }

    /** A separate two-column equality must not contaminate the parenthesized OR. */
    @Test
    public void testSameFieldOr() throws Exception
    {
        assertLines("ql-join-or-same-field.ql", List.of()); //$NON-NLS-1$
    }

    /** Literal-only conditions contain no columns. */
    @Test
    public void testNoFieldsOr() throws Exception
    {
        assertLines("ql-join-or-no-fields.ql", List.of()); //$NON-NLS-1$
    }

    /** Equal field names on different aliases are distinct column texts. */
    @Test
    public void testDifferentAliasesOr() throws Exception
    {
        assertLines("ql-or-position.ql", List.of(6)); //$NON-NLS-1$
    }

    /** Each OR in the same multi-column expression is reported, including a same-field subexpression. */
    @Test
    public void testMultipleFieldsOrChain() throws Exception
    {
        assertLines("ql-join-or-multiple-fields.ql", List.of(7, 8)); //$NON-NLS-1$
    }

    /** An OR in a joined subquery WHERE is not an OR in the enclosing join condition. */
    @Test
    public void testNestedQueryBoundary() throws Exception
    {
        assertLines("ql-join-or-nested-query.ql", List.of()); //$NON-NLS-1$
    }

    private void assertLines(String resource, List<Integer> expected) throws Exception
    {
        QuerySchema schema = loadQueryAndValidate(FOLDER_RESOURCE + resource);
        for (INode leaf : NodeModelUtils.findActualNodeFor(schema).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(expected, getQueryMarkers().stream().map(marker -> marker.getLineNumber()).sorted().toList());
        for (var marker : getQueryMarkers())
        {
            assertEquals("ИЛИ", NodeModelUtils.findActualNodeFor(schema).getRootNode().getText() //$NON-NLS-1$
                .substring(marker.getOffset(), marker.getOffset() + marker.getLength()));
        }
    }
}
