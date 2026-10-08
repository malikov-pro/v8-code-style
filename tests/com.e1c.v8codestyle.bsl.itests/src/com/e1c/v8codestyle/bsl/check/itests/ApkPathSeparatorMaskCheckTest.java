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
 *     malikov-pro - port of the APK check 01167 (path separator and all-files mask)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.ApkPathSeparatorMaskCheck;

/**
 * Tests for {@link ApkPathSeparatorMaskCheck} — port of the APK check
 * АПК_01167 (standard 723, clause 2.5.2: do not specify the path separator
 * and the all-files mask manually, use GetPathSeparator/GetAllFilesMask).
 *
 * @author malikov-pro
 */
public class ApkPathSeparatorMaskCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "apk-01167-path-separator-mask.bsl"; //$NON-NLS-1$

    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-01167-path-separator-mask-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public ApkPathSeparatorMaskCheckTest()
    {
        super(ApkPathSeparatorMaskCheck.class);
    }

    /**
     * A manual separator ("\") or the "*.*" mask in literal string parameters
     * of FindFiles (1, 2), BeginFindingFiles (2, 3) and the "New File"
     * creator (1) is reported once per parameter.
     */
    @Test
    public void testManualSeparatorAndMaskReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 7, markers.size());
        assertTrue(markers.stream().allMatch(m -> m.getMessage() != null && !m.getMessage().isEmpty()));
    }

    /**
     * Parameters built from variables and platform functions
     * (GetPathSeparator/GetAllFilesMask) produce no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
