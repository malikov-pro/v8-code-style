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
 *     malikov-pro - port of the APK check АПК_00715
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.ApkNoExclamationMarkCheck;

/**
 * Tests for {@link ApkNoExclamationMarkCheck} — port of the APK check
 * АПК_00715 (standard 585: messages should not contain exclamation marks).
 *
 * @author malikov-pro
 */
public class ApkNoExclamationMarkCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_EXCLAMATION = FOLDER_RESOURCE + "apk-00715-no-exclamation-mark.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-00715-no-exclamation-mark-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public ApkNoExclamationMarkCheckTest()
    {
        super(ApkNoExclamationMarkCheck.class);
    }

    /**
     * A message-ending exclamation mark is reported once per line, including
     * the NStr parts and a mark after a file extension.
     */
    @Test
    public void testExclamationLinesReported() throws Exception
    {
        updateModule(RESOURCE_EXCLAMATION);

        List<Marker> markers = getModuleMarkers();
        assertEquals(4, markers.size());
        assertTrue(markers.stream().allMatch(m -> m.getMessage() != null && !m.getMessage().isEmpty()));
    }

    /**
     * Allowed cases ("Внимание!"/"Attention!", the "!=" operator, bat-file
     * parameters) produce no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
