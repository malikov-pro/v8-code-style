/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.v8codestyle.bsl.check.MissedRequiredParameterCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class MissedRequiredParameterCheckTest
    extends AbstractSingleModuleTestBase
{
    public MissedRequiredParameterCheckTest()
    {
        super(MissedRequiredParameterCheck.class);
    }

    @Test
    public void testExplicitEmptyPositionsAndNativeArityExclusions() throws Exception
    {
        var settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        assertEquals(IssueSeverity.MAJOR, settings.getSeverity());
        assertEquals(0, settings.getParameters().size());
        updateModule(FOLDER_RESOURCE + "required-empty-corpus.bsl"); //$NON-NLS-1$
        assertLines(List.of(6, 7, 8, 13));
        var both = getModuleMarkers().stream().filter(marker -> Integer.valueOf(8).equals(
            marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE))).findFirst().orElseThrow();
        assertTrue(both.getMessage(), both.getMessage().contains("Первый") && both.getMessage().contains("Второй")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testEnglishNestedMultilineAndModuleBody() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "required-empty-boundaries.bsl"); //$NON-NLS-1$
        assertLines(List.of(6, 8, 12, 13, 16));
    }

    @Test
    public void testIncrementalArgumentReplacement() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "required-empty-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(5));
        updateModule(FOLDER_RESOURCE + "required-empty-clean.bsl"); //$NON-NLS-1$
        assertLines(List.of());
        updateModule(FOLDER_RESOURCE + "required-empty-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(5));
    }

    private void assertLines(List<Integer> expected)
    {
        var node = NodeModelUtils.findActualNodeFor(getModule());
        for (var leaf : node.getLeafNodes())
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
            assertTrue(text, (text.startsWith("Контракт(") || text.startsWith("Contract(")) && text.endsWith(")")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }
}
