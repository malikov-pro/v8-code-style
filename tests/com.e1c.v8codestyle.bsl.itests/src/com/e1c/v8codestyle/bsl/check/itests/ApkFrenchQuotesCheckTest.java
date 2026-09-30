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
 *     malikov-pro - port of the APK check АПК_01194
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.ApkFrenchQuotesCheck;

/**
 * Tests for {@link ApkFrenchQuotesCheck} — port of the APK check АПК_01194
 * (standard 598: French quotes are not allowed in interface texts, NStr literals).
 *
 * @author malikov-pro
 */
public class ApkFrenchQuotesCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_QUOTES = FOLDER_RESOURCE + "apk-01194-no-french-quotes.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-01194-no-french-quotes-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public ApkFrenchQuotesCheckTest()
    {
        super(ApkFrenchQuotesCheck.class);
    }

    /**
     * Each French quote inside an NStr literal (both language parameters)
     * is reported separately; quotes outside NStr and in comments are not reported.
     */
    @Test
    public void testFrenchQuotesInNStrReported() throws Exception
    {
        updateModule(RESOURCE_QUOTES);

        List<Marker> markers = getModuleMarkers();
        assertEquals(4, markers.size());
        assertTrue(markers.stream().allMatch(m -> m.getMessage() != null && !m.getMessage().isEmpty()));
    }

    /**
     * A module with straight quotes inside NStr and French quotes outside NStr
     * produces no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
