/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the APK check 01364 (Linux unsupported methods)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.ApkLinuxUnsupportedCheck;

/**
 * Tests for {@link ApkLinuxUnsupportedCheck} — port of the APK check
 * АПК_01364 (standard 723: methods and objects not supported on Linux).
 *
 * @author malikov-pro
 */
public class ApkLinuxUnsupportedCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "apk-01364-linux-unsupported.bsl"; //$NON-NLS-1$

    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-01364-linux-unsupported-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public ApkLinuxUnsupportedCheckTest()
    {
        super(ApkLinuxUnsupportedCheck.class);
    }

    /**
     * Each occurrence of an unsupported method, object or the COM add-in type
     * is reported, including occurrences in comments; similar but allowed
     * names ("Новый Файл", "AddInType.Native") are not reported.
     */
    @Test
    public void testUnsupportedOccurrencesReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 13, markers.size());
        assertTrue(markers.stream().allMatch(m -> m.getMessage() != null && !m.getMessage().isEmpty()));
    }

    /**
     * A module that only calls similar allowed methods has no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
