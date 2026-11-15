/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com.e1c.v8codestyle.ql.check.RefOveruseCheck;

/**
 * LS contract of RefOveruse: thresholds of the {@code Ссылка|Reference}
 * position in a path, source/tabular-section exemptions, nested query and
 * inline table boundaries.
 */
public class RefOveruseCheckTest
    extends AbstractQueryTestBase
{
    public RefOveruseCheckTest()
    {
        super(RefOveruseCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "QlFullDemo"; //$NON-NLS-1$
    }

    /** {@code Table.Ref.Attribute} of a plain table dereferences the reference for nothing. */
    @Test
    public void testTableRefAttribute() throws Exception
    {
        assertMarkers("ql-ref-overuse-table-attribute.ql", List.of(2)); //$NON-NLS-1$
        assertRange("ql-ref-overuse-table-attribute.ql", "Док.Ref.Description"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /** The owner reference of a tabular-section source is legitimate. */
    @Test
    public void testTabularSectionSource() throws Exception
    {
        assertMarkers("ql-ref-overuse-tabular-section.ql", List.of()); //$NON-NLS-1$
    }

    /** {@code Table.Ref} of a data source is the reference itself. */
    @Test
    public void testSourceReference() throws Exception
    {
        assertMarkers("ql-ref-overuse-source-ref.ql", List.of()); //$NON-NLS-1$
    }

    /** An unqualified {@code Ref.Ref} does not address a data source. */
    @Test
    public void testUnqualifiedRefRef() throws Exception
    {
        assertMarkers("ql-ref-overuse-direct-ref.ql", List.of(2)); //$NON-NLS-1$
    }

    /** A reference field as the first path segment is not reported. */
    @Test
    public void testFirstSegmentRef() throws Exception
    {
        assertMarkers("ql-ref-overuse-first-ref.ql", List.of()); //$NON-NLS-1$
    }

    /** A later {@code Ref} followed by another segment is always reported. */
    @Test
    public void testMiddleRef() throws Exception
    {
        assertMarkers("ql-ref-overuse-middle-ref.ql", List.of(2)); //$NON-NLS-1$
    }

    /** No reference step means nothing to report. */
    @Test
    public void testCleanQuery() throws Exception
    {
        assertMarkers("ql-ref-overuse-clean.ql", List.of()); //$NON-NLS-1$
    }

    /** A nested query is its own query: one marker, not a duplicate of the outer one. */
    @Test
    public void testNestedQueryBoundary() throws Exception
    {
        assertMarkers("ql-ref-overuse-nested-query.ql", List.of(8)); //$NON-NLS-1$
    }

    /** Fields of an inline table constructor are not checked, as in the pinned LS. */
    @Test
    public void testInlineTableBoundary() throws Exception
    {
        assertMarkers("ql-ref-overuse-inline-table.ql", List.of()); //$NON-NLS-1$
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

    private void assertRange(String resource, String expected) throws Exception
    {
        String text;
        try (var input = getClass().getResourceAsStream(FOLDER_RESOURCE + resource))
        {
            text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        var marker = getQueryMarkers().get(0);
        assertEquals(expected, text.substring(marker.getOffset(), marker.getOffset() + marker.getLength()));
    }
}
