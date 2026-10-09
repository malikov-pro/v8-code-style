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
 *     malikov-pro - port of the APK check АПК_01205
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.ApkMainLanguageCodeCheck;

/**
 * Tests for {@link ApkMainLanguageCodeCheck} — port of the APK check АПК_01205
 * (standard 784: the main language code is obtained by the MainLanguageCode
 * function of the Common common module, not via Metadata.MainLanguage).
 *
 * @author malikov-pro
 */
public class ApkMainLanguageCodeCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATING = FOLDER_RESOURCE + "apk-01205-main-language-code.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-01205-main-language-code-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public ApkMainLanguageCodeCheckTest()
    {
        super(ApkMainLanguageCodeCheck.class);
    }

    /**
     * Each access to the main language via the global context property Metadata
     * is reported, in Russian and English spellings, with or without the
     * trailing LanguageCode segment.
     */
    @Test
    public void testMainLanguageAccessReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATING);

        List<Marker> markers = getModuleMarkers();
        assertEquals(3, markers.size());
        assertTrue(markers.stream().allMatch(m -> m.getMessage() != null && !m.getMessage().isEmpty()));
    }

    /**
     * Getting the language code via the SSL function and other metadata
     * accesses produce no issues for this check.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
