/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.v8codestyle.bsl.check.UseSystemInformationCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class UseSystemInformationCheckTest extends AbstractSingleModuleTestBase
{
    public UseSystemInformationCheckTest()
    {
        super(UseSystemInformationCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "PlatformApiModern"; //$NON-NLS-1$
    }

    @Test
    public void testConstructorsDefaultsRangesAndCycles() throws Exception
    {
        var defaults = checkRepository.getDefaultSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        assertFalse(defaults.isEnabled());
        assertEquals(IssueSeverity.CRITICAL, defaults.getSeverity());
        assertTrue(defaults.getParameters().isEmpty());
        try
        {
            updateModule(FOLDER_RESOURCE + "use-system-information.bsl"); //$NON-NLS-1$
            assertLines(List.of(2, 3, 4, 5, 6, 7, 13, 15));
            updateModule(FOLDER_RESOURCE + "use-system-information-clean.bsl"); //$NON-NLS-1$
            assertLines(List.of());
            updateModule(FOLDER_RESOURCE + "use-system-information.bsl"); //$NON-NLS-1$
            assertLines(List.of(2, 3, 4, 5, 6, 7, 13, 15));
            setCheckEnable(false);
            assertLines(List.of());
            setCheckEnable(true);
            assertLines(List.of(2, 3, 4, 5, 6, 7, 13, 15));
        }
        finally
        {
            setCheckEnable(false);
        }
    }

    private void assertLines(List<Integer> expected)
    {
        var node = NodeModelUtils.findActualNodeFor(getModule());
        for (var leaf : node.getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(expected, getModuleMarkers().stream()
            .map(m -> (Integer)m.getExtraInfo().get(StandardExtraInfo.TEXT_LINE)).sorted().toList());
        String text = node.getRootNode().getText();
        for (var marker : getModuleMarkers())
        {
            int line = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
            int offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            int length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            String sourceLine = text.lines().skip(line - 1).findFirst().orElseThrow();
            int start = sourceLine.indexOf("New"); //$NON-NLS-1$
            String constructor = sourceLine.substring(start, sourceLine.lastIndexOf(';'));
            assertEquals(constructor, text.substring(offset, offset + length));
        }
    }
}
