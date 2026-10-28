/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: 1C-Soft LLC
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the BSL Language Server diagnostic NestedStatementsCheck
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.v8codestyle.bsl.check.NestedStatementsCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Tests for {@link NestedStatementsCheck} — port of the BSL Language Server diagnostic NestedStatementsCheck.
 *
 * @author malikov-pro
 */
public class NestedStatementsCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "nested-statements.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "nested-statements-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public NestedStatementsCheckTest()
    {
        super(NestedStatementsCheck.class);
    }

    /**
     * The statement on level 5 (max 4) is reported.
     */
    @Test
    public void testViolationsReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 1, markers.size());
    }

    /**
     * A compliant module has no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 0, markers.size());
    }

    @Test
    public void testLeafAndModuleScope() throws Exception
    {
        var settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        String original = settings.getParameters().get("maxAllowedLevel").getValue(); //$NON-NLS-1$
        assertEquals("4", original); //$NON-NLS-1$
        try
        {
            settings.getParameters().get("maxAllowedLevel").setValue("1"); //$NON-NLS-1$ //$NON-NLS-2$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
            updateModule(FOLDER_RESOURCE + "nesting-leaf-module.bsl"); //$NON-NLS-1$
            assertLines(List.of(4, 13));
            settings.getParameters().get("maxAllowedLevel").setValue("3"); //$NON-NLS-1$ //$NON-NLS-2$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
            assertLines(List.of());
        }
        finally
        {
            settings.getParameters().get("maxAllowedLevel").setValue(original); //$NON-NLS-1$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
        }
    }

    @Test
    public void testIncrementalLeafNesting() throws Exception
    {
        var settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        String original = settings.getParameters().get("maxAllowedLevel").getValue(); //$NON-NLS-1$
        try
        {
            settings.getParameters().get("maxAllowedLevel").setValue("2"); //$NON-NLS-1$ //$NON-NLS-2$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
            updateModule(FOLDER_RESOURCE + "nesting-incremental-bad.bsl"); //$NON-NLS-1$
            assertLines(List.of(4));
            updateModule(FOLDER_RESOURCE + "nesting-incremental-clean.bsl"); //$NON-NLS-1$
            assertLines(List.of());
            updateModule(FOLDER_RESOURCE + "nesting-incremental-bad.bsl"); //$NON-NLS-1$
            assertLines(List.of(4));
        }
        finally
        {
            settings.getParameters().get("maxAllowedLevel").setValue(original); //$NON-NLS-1$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
        }
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
            assertEquals("Если", node.getRootNode().getText().substring(offset, offset + length)); //$NON-NLS-1$
        }
    }

    @Test
    public void testSiblingAndLabeledCompoundBlocks() throws Exception
    {
        var settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        String original = settings.getParameters().get("maxAllowedLevel").getValue(); //$NON-NLS-1$
        try
        {
            settings.getParameters().get("maxAllowedLevel").setValue("1"); //$NON-NLS-1$ //$NON-NLS-2$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
            updateModule(FOLDER_RESOURCE + "nesting-sibling-label.bsl"); //$NON-NLS-1$
            assertLines(List.of(4, 7, 14));
        }
        finally
        {
            settings.getParameters().get("maxAllowedLevel").setValue(original); //$NON-NLS-1$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
        }
    }

    @Test
    public void testDeepNestingAcrossPreprocessorBranch() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "nesting-preprocessor-deep.bsl"); //$NON-NLS-1$
        assertLines(List.of(7));
        updateModule(FOLDER_RESOURCE + "nesting-preprocessor-clean.bsl"); //$NON-NLS-1$
        assertLines(List.of());
        updateModule(FOLDER_RESOURCE + "nesting-preprocessor-deep.bsl"); //$NON-NLS-1$
        assertLines(List.of(7));
    }

    @Test
    public void testRegionBranchesAndFollowingStatements() throws Exception
    {
        var settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        String original = settings.getParameters().get("maxAllowedLevel").getValue(); //$NON-NLS-1$
        try
        {
            settings.getParameters().get("maxAllowedLevel").setValue("1"); //$NON-NLS-1$ //$NON-NLS-2$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
            updateModule(FOLDER_RESOURCE + "nesting-preprocessor-boundaries.bsl"); //$NON-NLS-1$
            assertLines(List.of(4, 8, 12, 16, 20));
            settings.getParameters().get("maxAllowedLevel").setValue("2"); //$NON-NLS-1$ //$NON-NLS-2$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
            assertLines(List.of());
        }
        finally
        {
            settings.getParameters().get("maxAllowedLevel").setValue(original); //$NON-NLS-1$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
        }
    }
}
