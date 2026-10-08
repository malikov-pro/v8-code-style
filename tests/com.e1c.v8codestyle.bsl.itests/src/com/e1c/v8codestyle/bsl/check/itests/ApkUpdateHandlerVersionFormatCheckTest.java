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
 *     malikov-pro - port of the APK check АПК_01190
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.ApkUpdateHandlerVersionFormatCheck;

/**
 * Tests for {@link ApkUpdateHandlerVersionFormatCheck} — port of the APK check
 * АПК_01190 (standard 690: the infobase update handler version has the
 * "M.m.v.b" / "M.m.v" format or "*").
 *
 * @author malikov-pro
 */
public class ApkUpdateHandlerVersionFormatCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATING = FOLDER_RESOURCE + "apk-01190-update-version-format.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-01190-update-version-format-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public ApkUpdateHandlerVersionFormatCheckTest()
    {
        super(ApkUpdateHandlerVersionFormatCheck.class);
    }

    /**
     * Versions with less than three segments or non-digit segments are
     * reported; "*", empty version and valid numbers are not; assignments
     * outside the handler registration procedure are not reported.
     */
    @Test
    public void testInvalidVersionFormatReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATING);

        List<Marker> markers = getModuleMarkers();
        assertEquals(3, markers.size());
        assertTrue(markers.stream().allMatch(m -> m.getMessage() != null && !m.getMessage().isEmpty()));
    }

    /**
     * Valid handler versions produce no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
