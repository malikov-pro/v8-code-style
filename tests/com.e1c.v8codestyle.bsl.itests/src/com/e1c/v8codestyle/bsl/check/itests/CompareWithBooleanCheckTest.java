/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.CompareWithBooleanCheck;

public class CompareWithBooleanCheckTest
    extends AbstractSingleModuleTestBase
{
    public CompareWithBooleanCheckTest()
    {
        super(CompareWithBooleanCheck.class);
    }

    @Test
    public void testTypedCorpus() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "compare-with-boolean.bsl"); //$NON-NLS-1$
        assertLines(List.of(3, 6, 9, 19, 24));
    }

    @Test
    public void testIncrementalTypeDependency() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "compare-boolean-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(3));
        updateModule(FOLDER_RESOURCE + "compare-boolean-clean.bsl"); //$NON-NLS-1$
        assertLines(List.of());
        updateModule(FOLDER_RESOURCE + "compare-boolean-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(3));
    }

    @Test
    public void testExpressionTypeBoundaries() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "compare-boolean-boundaries.bsl"); //$NON-NLS-1$
        assertLines(List.of(2, 5));
    }

    private void assertLines(List<Integer> expected)
    {
        INode node = NodeModelUtils.findActualNodeFor(getModule());
        for (INode leaf : node.getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(expected, getModuleMarkers().stream().map(marker -> {
            Integer line = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
            return line;
        }).sorted().toList());
        for (var marker : getModuleMarkers())
        {
            Integer offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            Integer length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            String text = node.getRootNode().getText().substring(offset, offset + length);
            assertEquals(true, text.contains("=") || text.contains("<>")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(false, text.contains("Тогда")); //$NON-NLS-1$
        }
    }
}
