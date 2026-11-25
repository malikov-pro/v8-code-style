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
import java.util.stream.Collectors;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
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
    private static final String RESOURCE_COMMENT_ONLY =
        FOLDER_RESOURCE + "apk-00260-no-yo-letter-comment-only.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public ApkYoLetterCheckTest()
    {
        super(ApkYoLetterCheck.class);
    }

    /**
     * Each occurrence of the letter "ё" in the code (an identifier and a string literal) is reported separately;
     * the letter in line comments is allowed and is not reported.
     */
    @Test
    public void testYoLetterOccurrencesReported() throws Exception
    {
        updateModule(RESOURCE_YO);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 2, markers.size());
        // 6 — «Ёлка» в строковом литерале, 7 — идентификатор «Ёлочка»; строки 1, 2 и комментарий на 7 не помечены.
        assertEquals(List.of(6, 7), markers.stream()
            .map(marker -> (Integer)marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE))
            .sorted()
            .collect(Collectors.toList()));
    }

    /**
     * A module where the letter "ё" appears only in comments has no issues.
     */
    @Test
    public void testYoLetterInCommentsOnlyHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_COMMENT_ONLY);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), markers.isEmpty());
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
