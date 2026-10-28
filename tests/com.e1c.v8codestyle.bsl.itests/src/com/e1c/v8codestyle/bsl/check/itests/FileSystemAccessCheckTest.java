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
import com.e1c.v8codestyle.bsl.check.FileSystemAccessCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class FileSystemAccessCheckTest extends AbstractSingleModuleTestBase
{
    private static final List<Integer> ALL = List.of(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 20, 21, 25, 26);

    public FileSystemAccessCheckTest()
    {
        super(FileSystemAccessCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "PlatformApiModern"; //$NON-NLS-1$
    }

    @Test
    public void testDefaultsRangesParametersAndCycles() throws Exception
    {
        var project = getProject().getWorkspaceProject();
        var uid = new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID);
        var defaults = checkRepository.getDefaultSettings(uid, project);
        assertFalse(defaults.isEnabled());
        assertEquals(com.e1c.g5.v8.dt.check.settings.IssueSeverity.MAJOR, defaults.getSeverity());
        assertEquals(2, defaults.getParameters().size());
        assertEquals(FileSystemAccessCheck.DEFAULT_GLOBAL_METHODS,
            defaults.getParameters().get(FileSystemAccessCheck.GLOBAL_METHODS).getValue());
        assertEquals(FileSystemAccessCheck.DEFAULT_NEW_EXPRESSION,
            defaults.getParameters().get(FileSystemAccessCheck.NEW_EXPRESSION).getValue());
        try
        {
            updateModule(FOLDER_RESOURCE + "file-system-access.bsl"); //$NON-NLS-1$
            assertLines(ALL);
            updateModule(FOLDER_RESOURCE + "file-system-access-clean.bsl"); //$NON-NLS-1$
            assertLines(List.of());
            updateModule(FOLDER_RESOURCE + "file-system-access.bsl"); //$NON-NLS-1$
            assertLines(ALL);
            setCheckEnable(false);
            assertLines(List.of());
            setCheckEnable(true);
            assertLines(ALL);
            setParameter(FileSystemAccessCheck.GLOBAL_METHODS, "deletefiles"); //$NON-NLS-1$
            assertLines(List.of(2, 3, 4, 5, 6, 7, 12, 20, 25));
            setParameter(FileSystemAccessCheck.NEW_EXPRESSION, "File"); //$NON-NLS-1$
            assertLines(List.of(2, 4, 5, 12, 20, 25));
            setParameter(FileSystemAccessCheck.GLOBAL_METHODS, ""); //$NON-NLS-1$
            assertLines(List.of(2, 4, 5, 20, 25));
            setParameter(FileSystemAccessCheck.NEW_EXPRESSION, "["); //$NON-NLS-1$
            assertLines(List.of());
            setParameter(FileSystemAccessCheck.GLOBAL_METHODS, "TempFilesDir"); //$NON-NLS-1$
            assertLines(List.of(8, 26));
            setParameter(FileSystemAccessCheck.NEW_EXPRESSION, FileSystemAccessCheck.DEFAULT_NEW_EXPRESSION);
            setParameter(FileSystemAccessCheck.GLOBAL_METHODS, "["); //$NON-NLS-1$
            assertLines(List.of(2, 3, 4, 5, 6, 7, 20, 25));
            setParameter(FileSystemAccessCheck.GLOBAL_METHODS, FileSystemAccessCheck.DEFAULT_GLOBAL_METHODS);
            assertLines(ALL);
            assertIndependentTempDir();
        }
        finally
        {
            setParameter(FileSystemAccessCheck.GLOBAL_METHODS, FileSystemAccessCheck.DEFAULT_GLOBAL_METHODS);
            setParameter(FileSystemAccessCheck.NEW_EXPRESSION, FileSystemAccessCheck.DEFAULT_NEW_EXPRESSION);
            setCheckEnable(false);
        }
    }

    private void setParameter(String name, String value)
    {
        var settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        settings.getParameters().get(name).setValue(value);
        checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
        waitForDD(getProject());
    }

    private void assertIndependentTempDir()
    {
        var project = getProject().getWorkspaceProject();
        var tempUid = new CheckUid("temp-files-dir", BslPlugin.PLUGIN_ID); //$NON-NLS-1$
        var settings = checkRepository.getSettings(tempUid, project);
        boolean enabled = settings.isEnabled();
        var severity = settings.getSeverity();
        try
        {
            settings.setEnabled(true);
            checkRepository.applyChanges(List.of(settings), project);
            waitForDD(getProject());
            assertLines(ALL);
            assertEquals(3, tempDirMarkerCount(tempUid));
            setCheckEnable(false);
            assertLines(List.of());
            assertTrue(checkRepository.getSettings(tempUid, project).isEnabled());
            assertEquals(severity, checkRepository.getSettings(tempUid, project).getSeverity());
            assertEquals(3, tempDirMarkerCount(tempUid));
            setCheckEnable(true);
            assertLines(ALL);
        }
        finally
        {
            var restore = checkRepository.getSettings(tempUid, project);
            restore.setEnabled(enabled);
            restore.setSeverity(severity);
            checkRepository.applyChanges(List.of(restore), project);
            waitForDD(getProject());
        }
    }

    private long tempDirMarkerCount(CheckUid uid)
    {
        var project = getProject().getWorkspaceProject();
        return List.of(markerManager.getMarkers(project, getModuleId())).stream()
            .filter(marker -> uid.equals(checkRepository.getUidForShortUid(marker.getCheckId(), project))).count();
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
            String expectedRange;
            if (start >= 0)
            {
                expectedRange = sourceLine.substring(start, sourceLine.lastIndexOf(';'));
            }
            else
            {
                String call = sourceLine.strip();
                if (call.contains(" = ")) //$NON-NLS-1$
                {
                    call = call.substring(call.indexOf(" = ") + 3); //$NON-NLS-1$
                }
                expectedRange = call.substring(0, call.indexOf('('));
            }
            assertEquals(expectedRange, text.substring(offset, offset + length));
        }
    }
}
