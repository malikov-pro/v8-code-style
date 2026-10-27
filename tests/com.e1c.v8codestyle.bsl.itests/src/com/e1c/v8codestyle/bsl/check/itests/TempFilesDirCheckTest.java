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
import com.e1c.v8codestyle.bsl.check.TempFilesDirCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class TempFilesDirCheckTest extends AbstractSingleModuleTestBase
{
    public TempFilesDirCheckTest()
    {
        super(TempFilesDirCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "PlatformApiModern"; //$NON-NLS-1$
    }

    @Test
    public void testGlobalScopeDefaultsRangesAndCycles() throws Exception
    {
        var defaults = checkRepository.getDefaultSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        assertFalse(defaults.isEnabled());
        assertEquals(IssueSeverity.MAJOR, defaults.getSeverity());
        assertTrue(defaults.getParameters().isEmpty());
        try
        {
            updateModule(FOLDER_RESOURCE + "temp-files-dir.bsl"); //$NON-NLS-1$
            assertLines(List.of(2, 3, 4, 5));
            updateModule(FOLDER_RESOURCE + "temp-files-dir-clean.bsl"); //$NON-NLS-1$
            assertLines(List.of());
            updateModule(FOLDER_RESOURCE + "temp-files-dir.bsl"); //$NON-NLS-1$
            assertLines(List.of(2, 3, 4, 5));
            setCheckEnable(false);
            assertLines(List.of());
            setCheckEnable(true);
            assertLines(List.of(2, 3, 4, 5));
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
        for (var marker : getModuleMarkers())
        {
            int offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            int length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            String name = node.getRootNode().getText().substring(offset, offset + length);
            assertTrue(name, "TempFilesDir".equalsIgnoreCase(name) //$NON-NLS-1$
                || "КаталогВременныхФайлов".equalsIgnoreCase(name)); //$NON-NLS-1$
        }
    }
}
