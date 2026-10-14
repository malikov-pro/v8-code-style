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
 *     malikov-pro - port of the BSL Language Server diagnostic MissingTempStorageDeletionCheck
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.MissingTempStorageDeletionCheck;

/**
 * Tests for {@link MissingTempStorageDeletionCheck} — port of the BSL Language Server diagnostic MissingTempStorageDeletionCheck.
 *
 * @author malikov-pro
 */
public class MissingTempStorageDeletionCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "missing-temp-storage-deletion.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "missing-temp-storage-deletion-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public MissingTempStorageDeletionCheckTest()
    {
        super(MissingTempStorageDeletionCheck.class);
    }

    /**
     * GetFromTempStorage without any DeleteFromTempStorage is reported.
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

    /** Deletion must match the call arguments and start on a later line, as in pinned LS. */
    @Test
    public void testAddressAndOrder() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "temp-storage-address-order.bsl"); //$NON-NLS-1$
        assertEquals(List.of(2, 8, 13, 20, 24), markerLines());
    }

    /** Changing the existing deletion argument updates the marker without clean. */
    @Test
    public void testIncrementalAddressUpdates() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "temp-storage-incremental-bad.bsl"); //$NON-NLS-1$
        assertEquals(List.of(2), markerLines());
        updateModule(FOLDER_RESOURCE + "temp-storage-incremental-clean.bsl"); //$NON-NLS-1$
        assertEquals(List.of(), markerLines());
        updateModule(FOLDER_RESOURCE + "temp-storage-incremental-bad.bsl"); //$NON-NLS-1$
        assertEquals(List.of(2), markerLines());
    }

    /** String addresses remain case-sensitive; identifiers ignore case and hidden tokens. */
    @Test
    public void testTokenBoundaries() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "temp-storage-token-boundaries.bsl"); //$NON-NLS-1$
        assertEquals(List.of(2, 14), markerLines());
    }

    private List<Integer> markerLines()
    {
        for (INode leaf : NodeModelUtils.findActualNodeFor(getModule()).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        return getModuleMarkers().stream().map(marker -> {
            Integer line = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
            return line;
        }).sorted().toList();
    }
}
