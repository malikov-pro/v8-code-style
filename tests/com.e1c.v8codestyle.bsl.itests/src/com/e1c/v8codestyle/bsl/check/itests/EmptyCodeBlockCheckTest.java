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
 *     malikov-pro - port of the BSL Language Server diagnostic EmptyCodeBlock
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com.e1c.v8codestyle.bsl.check.EmptyCodeBlockCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Tests for {@link EmptyCodeBlockCheck} — port of the BSL Language Server
 * diagnostic EmptyCodeBlock (empty conditional and loop code blocks).
 *
 * @author malikov-pro
 */
public class EmptyCodeBlockCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_EMPTY = FOLDER_RESOURCE + "empty-code-block.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "empty-code-block-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public EmptyCodeBlockCheckTest()
    {
        super(EmptyCodeBlockCheck.class);
    }

    /**
     * Clean module first (filled blocks, empty "Except" block covered by a
     * separate check, empty method bodies — no issues), then the violating
     * module: 6 empty blocks of If/ElsIf/Else and loop bodies are reported.
     */
    @Test
    public void testEmptyBlocks() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> cleanMarkers = getModuleMarkers();
        String cleanDump = cleanMarkers.stream().map(m -> m.getMessage()).collect(Collectors.joining("\n")); //$NON-NLS-1$
        assertTrue("Expected 0 markers, got " + cleanMarkers.size() + ":\n" + cleanDump, cleanMarkers.isEmpty()); //$NON-NLS-1$ //$NON-NLS-2$

        updateModule(RESOURCE_EMPTY);

        List<Marker> markers = getModuleMarkers();
        String dump = markers.stream().map(m -> m.getMessage()).collect(Collectors.joining("\n")); //$NON-NLS-1$
        assertTrue("Expected 6 markers, got " + markers.size() + ":\n" + dump, markers.size() == 6); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testEmptyTryAndSectionComments() throws Exception
    {
        ICheckSettings settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        String original = settings.getParameters().get("commentAsCode").getValue(); //$NON-NLS-1$
        assertEquals("false", original); //$NON-NLS-1$
        try
        {
            updateModule(FOLDER_RESOURCE + "empty-try-sections.bsl"); //$NON-NLS-1$
            assertCount(2);
            setComment(settings, "true"); //$NON-NLS-1$
            assertCount(1);
            updateModule(FOLDER_RESOURCE + "empty-comment-sections.bsl"); //$NON-NLS-1$
            assertCount(2);
        }
        finally
        {
            setComment(settings, original);
        }
    }

    @Test
    public void testIncrementalTryContents() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "empty-try-bad.bsl"); //$NON-NLS-1$
        assertCount(1);
        updateModule(FOLDER_RESOURCE + "empty-try-clean.bsl"); //$NON-NLS-1$
        assertCount(0);
        updateModule(FOLDER_RESOURCE + "empty-try-bad.bsl"); //$NON-NLS-1$
        assertCount(1);
    }

    private void setComment(ICheckSettings settings, String value)
    {
        settings.getParameters().get("commentAsCode").setValue(value); //$NON-NLS-1$
        checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
        waitForDD(getProject());
    }

    @Test
    public void testNestedAndCommentSectionBoundaries() throws Exception
    {
        ICheckSettings settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        String original = settings.getParameters().get("commentAsCode").getValue(); //$NON-NLS-1$
        try
        {
            updateModule(FOLDER_RESOURCE + "empty-section-boundaries.bsl"); //$NON-NLS-1$
            assertCount(5);
            setComment(settings, "true"); //$NON-NLS-1$
            assertCount(2);
            for (var marker : getModuleMarkers())
            {
                if (marker.getMessage().contains("Попытк") || marker.getMessage().contains("Try")) //$NON-NLS-1$ //$NON-NLS-2$
                {
                    Integer line = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
                    Integer offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
                    Integer length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
                    assertEquals(Integer.valueOf(11), line);
                    assertEquals("Попытка", NodeModelUtils.findActualNodeFor(getModule()).getRootNode().getText() //$NON-NLS-1$
                        .substring(offset, offset + length));
                }
            }
            setComment(settings, "false"); //$NON-NLS-1$
            assertCount(5);
        }
        finally
        {
            setComment(settings, original);
        }
    }

    private void assertCount(int expected)
    {
        for (INode leaf : NodeModelUtils.findActualNodeFor(getModule()).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(getModuleMarkers().stream().map(Marker::getMessage).collect(Collectors.joining("; ")), //$NON-NLS-1$
            expected, getModuleMarkers().size());
    }
}
