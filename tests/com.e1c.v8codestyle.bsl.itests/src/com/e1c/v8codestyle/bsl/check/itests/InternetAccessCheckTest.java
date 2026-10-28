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
import java.util.stream.IntStream;

import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.v8codestyle.bsl.check.InternetAccessCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class InternetAccessCheckTest extends AbstractSingleModuleTestBase
{
    public InternetAccessCheckTest()
    {
        super(InternetAccessCheck.class);
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
        assertEquals(IssueSeverity.MAJOR, defaults.getSeverity());
        assertTrue(defaults.getParameters().isEmpty());
        List<Integer> expected = IntStream.concat(IntStream.rangeClosed(2, 23), IntStream.of(30)).boxed().toList();
        try
        {
            updateModule(FOLDER_RESOURCE + "internet-access.bsl"); //$NON-NLS-1$
            assertLines(expected);
            updateModule(FOLDER_RESOURCE + "internet-access-clean.bsl"); //$NON-NLS-1$
            assertLines(List.of());
            updateModule(FOLDER_RESOURCE + "internet-access.bsl"); //$NON-NLS-1$
            assertLines(expected);
            setCheckEnable(false);
            assertLines(List.of());
            setCheckEnable(true);
            assertLines(expected);
            assertIndependentTimeoutCheck();
        }
        finally
        {
            setCheckEnable(false);
        }
    }

    private void assertIndependentTimeoutCheck() throws Exception
    {
        var project = getProject().getWorkspaceProject();
        var timeoutUid = new CheckUid("timeouts-in-external-resources", BslPlugin.PLUGIN_ID); //$NON-NLS-1$
        var settings = checkRepository.getSettings(timeoutUid, project);
        boolean originalEnabled = settings.isEnabled();
        var originalSeverity = settings.getSeverity();
        try
        {
            settings.setEnabled(true);
            checkRepository.applyChanges(List.of(settings), project);
            waitForDD(getProject());
            updateModule(FOLDER_RESOURCE + "internet-access-coexist.bsl"); //$NON-NLS-1$
            assertLines(List.of(2));
            assertEquals(1, timeoutMarkers(timeoutUid).size());
            setCheckEnable(false);
            assertLines(List.of());
            assertTrue(checkRepository.getSettings(timeoutUid, project).isEnabled());
            assertEquals(originalSeverity, checkRepository.getSettings(timeoutUid, project).getSeverity());
            assertEquals(1, timeoutMarkers(timeoutUid).size());
            setCheckEnable(true);
            assertLines(List.of(2));
            assertEquals(1, timeoutMarkers(timeoutUid).size());
        }
        finally
        {
            var restore = checkRepository.getSettings(timeoutUid, project);
            restore.setEnabled(originalEnabled);
            restore.setSeverity(originalSeverity);
            checkRepository.applyChanges(List.of(restore), project);
            waitForDD(getProject());
        }
    }

    private List<Marker> timeoutMarkers(CheckUid uid)
    {
        var project = getProject().getWorkspaceProject();
        return List.of(markerManager.getMarkers(project, getModuleId())).stream()
            .filter(marker -> uid.equals(checkRepository.getUidForShortUid(marker.getCheckId(), project))).toList();
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
            String constructor = sourceLine.substring(sourceLine.indexOf("New"), sourceLine.lastIndexOf(';')); //$NON-NLS-1$
            assertEquals(constructor, text.substring(offset, offset + length));
        }
    }
}
