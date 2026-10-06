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
 *     malikov-pro - port of the APK check 00350 (reuse module return value)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Path;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.bsl.check.ApkReuseModuleReturnCheck;

/**
 * Tests for {@link ApkReuseModuleReturnCheck} — port of the APK check 00350
 * (return values of common modules with reuse).
 *
 * @author malikov-pro
 */
public class ApkReuseModuleReturnCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String PROJECT_NAME = "ApkReuseReturnCheck"; //$NON-NLS-1$

    private static final String VIOLATING_FILE_NAME = "/src/CommonModules/CachedViolating/Module.bsl"; //$NON-NLS-1$

    private static final String CLEAN_FILE_NAME = "/src/CommonModules/CachedClean/Module.bsl"; //$NON-NLS-1$

    private static final String NOT_CACHED_FILE_NAME = "/src/CommonModules/NotCached/Module.bsl"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * Export procedure and constant-return functions of a cached common module are reported.
     */
    @Test
    public void testViolationsReported()
    {
        List<Marker> markers = getMarkers(VIOLATING_FILE_NAME);
        assertEquals(describe(markers), 6, markers.size());
    }

    /**
     * A cached module with meaningful functions has no issues.
     */
    @Test
    public void testCleanCachedModule()
    {
        List<Marker> markers = getMarkers(CLEAN_FILE_NAME);
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 0, markers.size());
    }

    /**
     * A module without return value reuse is not checked at all.
     */
    @Test
    public void testNotCachedModuleSkipped()
    {
        List<Marker> markers = getMarkers(NOT_CACHED_FILE_NAME);
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 0, markers.size());
    }

    private List<Marker> getMarkers(String moduleFileName)
    {
        String moduleId = Path.ROOT.append(getTestConfigurationName()).append(moduleFileName).toString();
        List<Marker> markers = List.of(markerManager.getMarkers(getProject().getWorkspaceProject(), moduleId));

        return markers.stream()
            .filter(marker -> ApkReuseModuleReturnCheck.CHECK_ID.equals(getCheckIdFromMarker(marker, getProject())))
            .collect(Collectors.toList());
    }

    private static String describe(List<Marker> markers)
    {
        return markers.stream()
            .map(marker -> String.format("[line %s] %s", marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE),
                marker.getMessage()))
            .collect(Collectors.joining("; "));
    }
}
