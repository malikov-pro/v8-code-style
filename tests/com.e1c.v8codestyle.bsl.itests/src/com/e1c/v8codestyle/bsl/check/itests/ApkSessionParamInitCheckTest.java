/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: malikov-pro
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the APK check 00074 (session parameters init)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Path;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.bsl.check.ApkSessionParamInitCheck;

/**
 * Tests for {@link ApkSessionParamInitCheck} — port of the APK check 00074
 * (session parameters shall be initialized in the session module).
 *
 * @author malikov-pro
 */
public class ApkSessionParamInitCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String PROJECT_NAME = "ApkSessionParamInit"; //$NON-NLS-1$

    private static final String VIOLATING_FILE_NAME = "/src/CommonModules/CommonModule/Module.bsl"; //$NON-NLS-1$

    private static final String CLEAN_FILE_NAME = "/src/CommonModules/CommonModuleClean/Module.bsl"; //$NON-NLS-1$

    private static final String SESSION_MODULE_FILE_NAME = "/src/Configuration/SessionModule.bsl"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * An assignment to a session parameter outside the session module is reported.
     */
    @Test
    public void testViolationsReported()
    {
        List<Marker> markers = getMarkers(VIOLATING_FILE_NAME);
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 1, markers.size());
    }

    /**
     * Reading a session parameter is not reported.
     */
    @Test
    public void testCleanModuleHasNoIssues()
    {
        List<Marker> markers = getMarkers(CLEAN_FILE_NAME);
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 0, markers.size());
    }

    /**
     * Assignments in the session module itself are allowed.
     */
    @Test
    public void testSessionModuleIsAllowed()
    {
        List<Marker> markers = getMarkers(SESSION_MODULE_FILE_NAME);
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 0, markers.size());
    }

    private List<Marker> getMarkers(String moduleFileName)
    {
        String moduleId = Path.ROOT.append(getTestConfigurationName()).append(moduleFileName).toString();
        List<Marker> markers = List.of(markerManager.getMarkers(getProject().getWorkspaceProject(), moduleId));

        return markers.stream()
            .filter(marker -> ApkSessionParamInitCheck.CHECK_ID.equals(getCheckIdFromMarker(marker, getProject())))
            .collect(Collectors.toList());
    }
}
