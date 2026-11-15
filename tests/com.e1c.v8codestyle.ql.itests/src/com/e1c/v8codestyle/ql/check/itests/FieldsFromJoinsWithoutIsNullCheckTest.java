/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com.e1c.v8codestyle.ql.check.FieldsFromJoinsWithoutIsNullCheck;

/**
 * LS contract of FieldsFromJoinsWithoutIsNull: nullable side of an outer join,
 * ЕСТЬNULL function and IS NULL comparison guards, INNER join exemption and the
 * query-wide non-null filter exemption.
 */
public class FieldsFromJoinsWithoutIsNullCheckTest
    extends AbstractQueryTestBase
{
    public FieldsFromJoinsWithoutIsNullCheckTest()
    {
        super(FieldsFromJoinsWithoutIsNullCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "QlFullDemo"; //$NON-NLS-1$
    }

    /** SELECT of a left-joined field without a guard is reported on the join. */
    @Test
    public void testSelectFieldWithoutGuard() throws Exception
    {
        assertMarkers("ql-outer-join-without-isnull.ql", List.of(5)); //$NON-NLS-1$
        assertTrue(getQueryMarkers().get(0).getMessage(),
            getQueryMarkers().get(0).getMessage().contains("T.Description")); //$NON-NLS-1$
    }

    /** The ЕСТЬNULL/ISNULL function guards the field even though v1 of the LS corpus does not use it. */
    @Test
    public void testSelectFieldGuardedByFunction() throws Exception
    {
        assertMarkers("ql-outer-join-with-isnull.ql", List.of()); //$NON-NLS-1$
    }

    /** An INNER join has no nullable side. */
    @Test
    public void testInnerJoinIgnored() throws Exception
    {
        assertMarkers("ql-outer-join-inner.ql", List.of()); //$NON-NLS-1$
    }

    /** A WHERE condition of the nullable side is checked as well. */
    @Test
    public void testWhereWithoutGuard() throws Exception
    {
        assertMarkers("ql-outer-join-where.ql", List.of(4)); //$NON-NLS-1$
    }

    /** A non-null filter exempts the whole query, as the pinned LS does. */
    @Test
    public void testNonNullFilterExemptsQuery() throws Exception
    {
        assertMarkers("ql-outer-join-not-null-filter.ql", List.of()); //$NON-NLS-1$
    }

    /** A RIGHT join makes the parent data source nullable. */
    @Test
    public void testRightJoinParentSide() throws Exception
    {
        assertMarkers("ql-outer-join-right.ql", List.of(4)); //$NON-NLS-1$
    }

    /** Both sides of a FULL join are nullable, one marker with both fields. */
    @Test
    public void testFullJoinBothSides() throws Exception
    {
        assertMarkers("ql-outer-join-full.ql", List.of(5)); //$NON-NLS-1$
        String message = getQueryMarkers().get(0).getMessage();
        assertTrue(message, message.contains("P.Description") && message.contains("T.Description")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /** A nullable field used in another join condition is reported, as in the pinned LS. */
    @Test
    public void testFieldInAnotherJoinCondition() throws Exception
    {
        assertMarkers("ql-outer-join-condition.ql", List.of(4)); //$NON-NLS-1$
    }

    /** A correlated subquery is its own query: its WHERE does not flag the outer join. */
    @Test
    public void testNestedQueryBoundary() throws Exception
    {
        assertMarkers("ql-outer-join-nested-query.ql", List.of()); //$NON-NLS-1$
    }

    private void assertMarkers(String resource, List<Integer> expected) throws Exception
    {
        var schema = loadQueryAndValidate(FOLDER_RESOURCE + resource);
        for (var leaf : NodeModelUtils.getNode(schema).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(expected, getQueryMarkers().stream().map(marker -> marker.getLineNumber()).sorted().toList());
    }
}
