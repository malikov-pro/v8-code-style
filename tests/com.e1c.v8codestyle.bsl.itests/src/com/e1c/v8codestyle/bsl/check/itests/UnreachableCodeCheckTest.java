/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.v8codestyle.bsl.check.UnreachableCodeCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class UnreachableCodeCheckTest extends AbstractSingleModuleTestBase
{
    public UnreachableCodeCheckTest()
    {
        super(UnreachableCodeCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "PlatformApiModern"; //$NON-NLS-1$
    }

    @Test
    public void testGapsAfterBreakContinueGoto() throws Exception
    {
        var module = updateAndGetModule(FOLDER_RESOURCE + "unreachable-code-gaps.bsl"); //$NON-NLS-1$
        for (var leaf : NodeModelUtils.findActualNodeFor(module).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        var settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        assertTrue(settings.isEnabled());
        assertEquals(IssueSeverity.MINOR, settings.getSeverity());
        // after Break (5), after Continue (8), after Goto (15); native-covered
        // cases after Return (22) and Raise (26) must NOT be flagged here
        assertLines(List.of(5, 8, 15));
    }

    @Test
    public void testSourceReplacementCycles() throws Exception
    {
        updateAndGetModule(FOLDER_RESOURCE + "unreachable-code-gaps.bsl"); //$NON-NLS-1$
        assertLines(List.of(5, 8, 15));
        writeSource("""
            Procedure InLoop(Array) Export
                For Each Item In Array Do
                    If Item > 0 Then
                        Break;
                    EndIf;
                    Continue;
                EndDo;
                AfterLoop = 4;
            EndProcedure
            """); //$NON-NLS-1$
        assertLines(List.of());
        updateModule(FOLDER_RESOURCE + "unreachable-code-gaps.bsl"); //$NON-NLS-1$
        assertLines(List.of(5, 8, 15));
    }

    private void writeSource(String source) throws Exception
    {
        var file = getProject().getWorkspaceProject().getFile(getModuleFileName());
        file.setContents(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), true, true,
            new NullProgressMonitor());
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
        waitForDD(getProject());
    }

    private void assertLines(List<Integer> expected)
    {
        assertEquals(expected, getModuleMarkers().stream()
            .map(marker -> (Integer)marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE)).sorted().toList());
    }
}
