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
 *     malikov-pro - port of the APK rule АПК_00576
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.ApkZeroDocumentFieldsCheck;

/**
 * Tests for {@link ApkZeroDocumentFieldsCheck} — port of the APK check
 * АПК_00576 (standard 548: zero margins of a spreadsheet document).
 *
 * @author malikov-pro
 */
public class ApkZeroDocumentFieldsCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "apk-00576-zero-document-fields.bsl"; //$NON-NLS-1$

    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-00576-zero-document-fields-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public ApkZeroDocumentFieldsCheckTest()
    {
        super(ApkZeroDocumentFieldsCheck.class);
    }

    /**
     * Each assignment of zero to a spreadsheet document margin is reported:
     * Russian and English property names, with and without whitespace around
     * the assignment sign, and occurrences in comments.
     */
    @Test
    public void testZeroFieldOccurrencesReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 9, markers.size());
        assertTrue(markers.stream().allMatch(m -> m.getMessage() != null && !m.getMessage().isEmpty()));
    }

    /**
     * A module that only sets non-zero margins (or compares them) has no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
