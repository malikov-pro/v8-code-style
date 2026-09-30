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
 *     malikov-pro - port of the APK check АПК_00260
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.ApkYoLetterCheck;

/**
 * Tests for {@link ApkYoLetterCheck} — port of the APK check АПК_00260
 * (standard 598: refuse using the letter "ё" in module texts).
 *
 * @author malikov-pro
 */
public class ApkYoLetterCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_YO = FOLDER_RESOURCE + "apk-00260-no-yo-letter.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-00260-no-yo-letter-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public ApkYoLetterCheckTest()
    {
        super(ApkYoLetterCheck.class);
    }

    /**
     * Each occurrence of the letter "ё" (in a comment and in a string literal)
     * is reported separately.
     */
    @Test
    public void testYoLetterOccurrencesReported() throws Exception
    {
        updateModule(RESOURCE_YO);

        List<Marker> markers = getModuleMarkers();
        assertEquals(2, markers.size());
        assertTrue(markers.stream().allMatch(m -> m.getMessage() != null && !m.getMessage().isEmpty()));
    }

    /**
     * A module without the letter "ё" produces no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
