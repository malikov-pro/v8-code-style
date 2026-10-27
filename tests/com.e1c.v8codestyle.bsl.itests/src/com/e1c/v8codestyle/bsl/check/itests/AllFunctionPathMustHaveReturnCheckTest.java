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
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.v8codestyle.bsl.check.AllFunctionPathMustHaveReturnCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class AllFunctionPathMustHaveReturnCheckTest extends AbstractSingleModuleTestBase
{
    public AllFunctionPathMustHaveReturnCheckTest()
    {
        super(AllFunctionPathMustHaveReturnCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "PlatformApiModern"; //$NON-NLS-1$
    }

    @Test
    public void testFallThroughFunctionsFlagged() throws Exception
    {
        var module = updateAndGetModule(FOLDER_RESOURCE + "all-function-path-must-have-return.bsl"); //$NON-NLS-1$
        assertSyntaxClean(module);
        var settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        assertEquals(true, settings.isEnabled());
        assertEquals(IssueSeverity.MAJOR, settings.getSeverity());
        // marker is placed on the function name: FallThroughNoElse line 9, ElsIfNoElse line 16
        assertLines(List.of(9, 16));
    }

    @Test
    public void testPinnedCorpusDefault() throws Exception
    {
        var module = updateAndGetModule(FOLDER_RESOURCE + "all-function-path-corpus.bsl"); //$NON-NLS-1$
        assertSyntaxClean(module);
        // exact pinned LS reference lines (default parameters)
        assertLines(List.of(1, 26, 94, 103, 132));
    }

    @Test
    public void testPinnedCorpusParameters() throws Exception
    {
        var project = getProject().getWorkspaceProject();
        CheckUid cuid = new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID);
        ICheckSettings settings = checkRepository.getSettings(cuid, project);
        settings.getParameters().get(AllFunctionPathMustHaveReturnCheck.PARAM_LOOPS_EXECUTED_AT_LEAST_ONCE)
            .setValue(Boolean.FALSE.toString());
        checkRepository.applyChanges(Collections.singleton(settings), project);
        try
        {
            updateAndGetModule(FOLDER_RESOURCE + "all-function-path-corpus.bsl"); //$NON-NLS-1$
            assertLines(List.of(1, 26, 37, 94, 103, 132));
            settings = checkRepository.getSettings(cuid, project);
            settings.getParameters().get(AllFunctionPathMustHaveReturnCheck.PARAM_LOOPS_EXECUTED_AT_LEAST_ONCE)
                .setValue(Boolean.TRUE.toString());
            settings.getParameters().get(AllFunctionPathMustHaveReturnCheck.PARAM_IGNORE_MISSING_ELSE_ON_EXIT)
                .setValue(Boolean.TRUE.toString());
            checkRepository.applyChanges(Collections.singleton(settings), project);
            waitForDD(getProject());
            assertLines(List.of(26, 94, 103));
        }
        finally
        {
            settings = checkRepository.getSettings(cuid, project);
            settings.getParameters().get(AllFunctionPathMustHaveReturnCheck.PARAM_LOOPS_EXECUTED_AT_LEAST_ONCE)
                .setValue(Boolean.TRUE.toString());
            settings.getParameters().get(AllFunctionPathMustHaveReturnCheck.PARAM_IGNORE_MISSING_ELSE_ON_EXIT)
                .setValue(Boolean.FALSE.toString());
            checkRepository.applyChanges(Collections.singleton(settings), project);
        }
    }

    @Test
    public void testLoopGotoAndTryBoundaries() throws Exception
    {
        var module = updateAndGetModule(FOLDER_RESOURCE + "all-function-path-boundaries.bsl"); //$NON-NLS-1$
        assertSyntaxClean(module);
        // LoopFall (1) is clean when loops are executed at least once; EndlessWhile (7)
        // is clean in any mode; GotoPath (13) falls through the label; TryClean (22) is clean
        assertLines(List.of(13));
        var project = getProject().getWorkspaceProject();
        CheckUid cuid = new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID);
        ICheckSettings settings = checkRepository.getSettings(cuid, project);
        settings.getParameters().get(AllFunctionPathMustHaveReturnCheck.PARAM_LOOPS_EXECUTED_AT_LEAST_ONCE)
            .setValue(Boolean.FALSE.toString());
        checkRepository.applyChanges(Collections.singleton(settings), project);
        waitForDD(getProject());
        try
        {
            assertLines(List.of(1, 13));
        }
        finally
        {
            settings = checkRepository.getSettings(cuid, project);
            settings.getParameters().get(AllFunctionPathMustHaveReturnCheck.PARAM_LOOPS_EXECUTED_AT_LEAST_ONCE)
                .setValue(Boolean.TRUE.toString());
            checkRepository.applyChanges(Collections.singleton(settings), project);
        }
    }

    @Test
    public void testSourceReplacementCycles() throws Exception
    {
        updateAndGetModule(FOLDER_RESOURCE + "all-function-path-must-have-return.bsl"); //$NON-NLS-1$
        assertLines(List.of(9, 16));
        writeSource("""
            Function Clean(Flag) Export
                If Flag Then
                    Return 1;
                Else
                    Return 2;
                EndIf;
            EndFunction
            """); //$NON-NLS-1$
        assertLines(List.of());
        updateModule(FOLDER_RESOURCE + "all-function-path-must-have-return.bsl"); //$NON-NLS-1$
        assertLines(List.of(9, 16));
    }

    @Test
    public void testRecoveryFunctionSkipped() throws Exception
    {
        // broken function without EndFunction must be skipped, not flagged or crashed
        updateModule(FOLDER_RESOURCE + "all-function-path-recovery.bsl"); //$NON-NLS-1$
        assertLines(List.of());
    }

    private void assertSyntaxClean(com._1c.g5.v8.dt.bsl.model.Module module)
    {
        for (var leaf : NodeModelUtils.findActualNodeFor(module).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
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
