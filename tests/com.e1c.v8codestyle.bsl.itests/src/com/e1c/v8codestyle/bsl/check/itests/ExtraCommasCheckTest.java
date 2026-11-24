/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
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
import com.e1c.v8codestyle.bsl.check.ExtraCommasCheck;

/** Trailing commas of the pinned LS corpus, empty middle parameters and incremental updates. */
public class ExtraCommasCheckTest
    extends AbstractSingleModuleTestBase
{
    public ExtraCommasCheckTest()
    {
        super(ExtraCommasCheck.class);
    }

    @Test
    public void testCorpus() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "extra-commas.bsl"); //$NON-NLS-1$
        assertMarkers(List.of(9, 10, 11, 12, 14, 18));
    }

    @Test
    public void testIncrementalUpdates() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "extra-commas-incremental-clean.bsl"); //$NON-NLS-1$
        assertMarkers(List.of());
        updateModule(FOLDER_RESOURCE + "extra-commas-incremental-bad.bsl"); //$NON-NLS-1$
        assertMarkers(List.of(2));
        updateModule(FOLDER_RESOURCE + "extra-commas-incremental-clean.bsl"); //$NON-NLS-1$
        assertMarkers(List.of());
        setCheckEnable(false);
        updateModule(FOLDER_RESOURCE + "extra-commas-incremental-bad.bsl"); //$NON-NLS-1$
        assertMarkers(List.of());
        setCheckEnable(true);
        assertMarkers(List.of(2));
    }

    private void assertMarkers(List<Integer> expected)
    {
        INode node = NodeModelUtils.findActualNodeFor(getModule());
        for (INode leaf : node.getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(expected, getModuleMarkers().stream()
            .map(marker -> (Integer)marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE))
            .sorted().toList());
        for (var marker : getModuleMarkers())
        {
            Integer offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            Integer length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            assertEquals(Integer.valueOf(1), length);
            assertEquals(",", node.getRootNode().getText().substring(offset, offset + length)); //$NON-NLS-1$
        }
    }
}
