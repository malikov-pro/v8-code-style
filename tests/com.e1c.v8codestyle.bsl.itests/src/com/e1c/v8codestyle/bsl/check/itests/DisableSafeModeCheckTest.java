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
import com.e1c.v8codestyle.bsl.check.DisableSafeModeCheck;

public class DisableSafeModeCheckTest
    extends AbstractSingleModuleTestBase
{
    public DisableSafeModeCheckTest()
    {
        super(DisableSafeModeCheck.class);
    }

    @Test
    public void testCorpus() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "disable-safe-mode.bsl"); //$NON-NLS-1$
        assertLines(List.of(2, 3, 5, 6, 8, 9, 12, 13, 16));
    }

    @Test
    public void testIncrementalFlag() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "disable-safe-mode-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(2));
        updateModule(FOLDER_RESOURCE + "disable-safe-mode-clean.bsl"); //$NON-NLS-1$
        assertLines(List.of());
        updateModule(FOLDER_RESOURCE + "disable-safe-mode-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(2));
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
            assertEquals(true, text.endsWith(")")); //$NON-NLS-1$
            assertEquals(false, text.startsWith("Объект.")); //$NON-NLS-1$
        }
    }
}
