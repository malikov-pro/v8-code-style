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
 *     malikov-pro - port of the APK check АПК_00205
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.ApkQueryEmptyResultCheck;

/**
 * Tests for {@link ApkQueryEmptyResultCheck} — port of the APK check АПК_00205
 * (standard 438: query result emptiness is checked with the Empty method,
 * not by iterating the selection in a condition).
 *
 * @author malikov-pro
 */
public class ApkQueryEmptyResultCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATING = FOLDER_RESOURCE + "apk-00205-query-empty-result.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-00205-query-empty-result-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public ApkQueryEmptyResultCheckTest()
    {
        super(ApkQueryEmptyResultCheck.class);
    }

    /**
     * A variable assigned from Query.Execute().Select() and tested with Next()
     * in an If or While condition is reported — one issue per condition.
     */
    @Test
    public void testSelectionIterationInConditionReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATING);

        List<Marker> markers = getModuleMarkers();
        assertEquals(2, markers.size());
        assertTrue(markers.stream().allMatch(m -> m.getMessage() != null && !m.getMessage().isEmpty()));
    }

    /**
     * The Empty() method and a Next() call outside an If/While condition
     * produce no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
