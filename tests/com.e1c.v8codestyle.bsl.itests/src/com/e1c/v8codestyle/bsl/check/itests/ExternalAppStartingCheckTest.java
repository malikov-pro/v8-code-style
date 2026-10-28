/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.stream.IntStream;

import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.v8codestyle.bsl.check.ExternalAppStartingCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class ExternalAppStartingCheckTest extends AbstractSingleModuleTestBase
{
    private static final List<Integer> DEFAULT_LINES =
        IntStream.concat(IntStream.rangeClosed(2, 14), IntStream.of(23, 24, 26)).boxed().toList();

    public ExternalAppStartingCheckTest()
    {
        super(ExternalAppStartingCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "PlatformApiModern"; //$NON-NLS-1$
    }

    @Test
    public void testNameScopeDefaultsParametersRangesAndCycles() throws Exception
    {
        var defaults = checkRepository.getDefaultSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        assertFalse(defaults.isEnabled());
        assertEquals(IssueSeverity.MAJOR, defaults.getSeverity());
        assertEquals(2, defaults.getParameters().size());
        assertEquals(ExternalAppStartingCheck.DEFAULT_PATTERN,
            defaults.getParameters().get(ExternalAppStartingCheck.USER_PATTERN).getValue());
        assertEquals("false", defaults.getParameters().get(ExternalAppStartingCheck.CHECK_GOTO_URL).getValue()); //$NON-NLS-1$
        try
        {
            updateModule(FOLDER_RESOURCE + "external-app-starting.bsl"); //$NON-NLS-1$
            assertLines(DEFAULT_LINES);
            updateModule(FOLDER_RESOURCE + "external-app-starting-clean.bsl"); //$NON-NLS-1$
            assertLines(List.of());
            updateModule(FOLDER_RESOURCE + "external-app-starting.bsl"); //$NON-NLS-1$
            assertLines(DEFAULT_LINES);
            setCheckEnable(false);
            assertLines(List.of());
            setCheckEnable(true);
            assertLines(DEFAULT_LINES);
            setParameter(ExternalAppStartingCheck.CHECK_GOTO_URL, "true"); //$NON-NLS-1$
            assertLines(IntStream.concat(IntStream.rangeClosed(2, 17), IntStream.of(23, 24, 26)).boxed().toList());
            setParameter(ExternalAppStartingCheck.USER_PATTERN, "командасистемы"); //$NON-NLS-1$
            assertLines(List.of(5, 15, 16, 17));
            setParameter(ExternalAppStartingCheck.CHECK_GOTO_URL, "false"); //$NON-NLS-1$
            assertLines(List.of(5));
            setParameter(ExternalAppStartingCheck.USER_PATTERN, ""); //$NON-NLS-1$
            assertLines(List.of());
            setParameter(ExternalAppStartingCheck.CHECK_GOTO_URL, "true"); //$NON-NLS-1$
            assertLines(List.of(15, 16, 17));
            setParameter(ExternalAppStartingCheck.USER_PATTERN, "["); //$NON-NLS-1$
            assertLines(List.of());
            setParameter(ExternalAppStartingCheck.CHECK_GOTO_URL, "false"); //$NON-NLS-1$
            setParameter(ExternalAppStartingCheck.USER_PATTERN, ExternalAppStartingCheck.DEFAULT_PATTERN);
            assertLines(DEFAULT_LINES);
        }
        finally
        {
            setParameter(ExternalAppStartingCheck.CHECK_GOTO_URL, "false"); //$NON-NLS-1$
            setParameter(ExternalAppStartingCheck.USER_PATTERN, ExternalAppStartingCheck.DEFAULT_PATTERN);
            setCheckEnable(false);
        }
    }

    private void setParameter(String name, String value)
    {
        var project = getProject().getWorkspaceProject();
        var settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID), project);
        settings.getParameters().get(name).setValue(value);
        checkRepository.applyChanges(List.of(settings), project);
        waitForDD(getProject());
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
            String sourceLine = text.lines().skip(line - 1).findFirst().orElseThrow().strip();
            int begin = sourceLine.lastIndexOf('.') + 1;
            String name = sourceLine.substring(begin, sourceLine.indexOf('(', begin));
            assertEquals(name, text.substring(offset, offset + length));
        }
    }
}
