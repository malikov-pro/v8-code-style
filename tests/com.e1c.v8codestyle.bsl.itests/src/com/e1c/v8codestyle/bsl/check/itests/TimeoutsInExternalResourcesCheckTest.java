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
 *     malikov-pro - port of the BSL Language Server diagnostic TimeoutsInExternalResources
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
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com.e1c.v8codestyle.bsl.check.TimeoutsInExternalResourcesCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Tests for {@link TimeoutsInExternalResourcesCheck} — port of the BSL
 * Language Server diagnostic TimeoutsInExternalResources.
 *
 * @author malikov-pro
 */
public class TimeoutsInExternalResourcesCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "timeouts-in-external-resources.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "timeouts-in-external-resources-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public TimeoutsInExternalResourcesCheckTest()
    {
        super(TimeoutsInExternalResourcesCheck.class);
    }

    /**
     * Creations of HTTPConnection/InternetMailProfile/WSDefinitions without
     * a timeout are reported (3 markers).
     */
    @Test
    public void testMissingTimeoutReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 3, markers.size());
    }

    /**
     * Timeouts set in constructors or by a later property assignment produce
     * no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 0, markers.size());
    }

    /** Calls/property accesses are valid timeout expressions; WSProxy uses index 5, not WSDefinitions' 4. */
    @Test
    public void testExpressionsAndOrder() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "timeouts-expressions-order.bsl"); //$NON-NLS-1$
        assertEquals(List.of(15, 20, 24), markerLines());
    }

    /** Editing the existing timeout expression updates the marker without clean. */
    @Test
    public void testIncrementalTimeoutUpdates() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "timeouts-incremental-bad.bsl"); //$NON-NLS-1$
        assertEquals(List.of(2), markerLines());
        updateModule(FOLDER_RESOURCE + "timeouts-incremental-clean.bsl"); //$NON-NLS-1$
        assertEquals(List.of(), markerLines());
        updateModule(FOLDER_RESOURCE + "timeouts-incremental-bad.bsl"); //$NON-NLS-1$
        assertEquals(List.of(2), markerLines());
    }

    /** Pinned LS requires the setter's statement to start on a strictly later line. */
    @Test
    public void testSameLineSetter() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "timeouts-same-line.bsl"); //$NON-NLS-1$
        assertEquals(List.of(2), markerLines());
    }

    /** Changing the existing mail parameter must remove and restore the marker. */
    @Test
    public void testMailParameterUpdates() throws Exception
    {
        ICheckSettings settings = checkRepository.getSettings(
            new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID), getProject().getWorkspaceProject());
        String key = "analyzeInternetMailProfileZeroTimeout"; //$NON-NLS-1$
        String original = settings.getParameters().get(key).getValue();
        assertEquals("true", original); //$NON-NLS-1$
        try
        {
            updateModule(FOLDER_RESOURCE + "timeouts-incremental-bad.bsl"); //$NON-NLS-1$
            assertEquals(List.of(2), markerLines());
            settings.getParameters().get(key).setValue("false"); //$NON-NLS-1$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
            assertEquals(List.of(), markerLines());
            settings.getParameters().get(key).setValue("true"); //$NON-NLS-1$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
            assertEquals(List.of(2), markerLines());
        }
        finally
        {
            settings.getParameters().get(key).setValue(original);
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
        }
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
