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
import com.e1c.v8codestyle.bsl.check.FunctionOutParameterCheck;

public class FunctionOutParameterCheckTest
    extends AbstractSingleModuleTestBase
{
    public FunctionOutParameterCheckTest()
    {
        super(FunctionOutParameterCheck.class);
    }

    @Test
    public void testCorpus() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "function-out-parameter.bsl"); //$NON-NLS-1$
        assertLines(List.of(2, 19));
    }

    @Test
    public void testIncrementalByValue() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "function-out-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(2));
        updateModule(FOLDER_RESOURCE + "function-out-clean.bsl"); //$NON-NLS-1$
        assertLines(List.of());
        updateModule(FOLDER_RESOURCE + "function-out-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(2));
    }

    @Test
    public void testEnableDisableUpdates() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "function-out-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(2));
        try
        {
            setCheckEnable(false);
            assertLines(List.of());
            setCheckEnable(true);
            assertLines(List.of(2));
        }
        finally
        {
            setCheckEnable(true);
        }
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
            Integer length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            Integer offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            assertEquals(Integer.valueOf(8), length);
            assertEquals("параметр", node.getRootNode().getText().substring(offset, offset + length) //$NON-NLS-1$
                .toLowerCase(java.util.Locale.ROOT));
        }
    }
}
