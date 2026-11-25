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
 *     malikov-pro - port of the BSL Language Server diagnostic MissingSpaceCheck
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.MissingSpaceCheck;

/**
 * Tests for {@link MissingSpaceCheck} — port of the BSL Language Server diagnostic MissingSpaceCheck.
 *
 * @author malikov-pro
 */
public class MissingSpaceCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "missing-space.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "missing-space-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public MissingSpaceCheckTest()
    {
        super(MissingSpaceCheck.class);
    }

    /**
     * Missing spaces around binary operators and after commas are reported; a unary minus in front of a number and
     * commas inside a string literal are not.
     */
    @Test
    public void testViolationsReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        // 3 «+», 4 «>» (две стороны), 5 «*», 8 «,», 13 «=» (две стороны), 16 «-» (бинарный).
        // Строки 11 и 12 — унарный минус, 19 — запятые внутри строкового литерала: маркеров нет.
        assertEquals(texts(markers), List.of(3, 4, 4, 5, 8, 13, 13, 16), lines(markers));
    }

    private static List<Integer> lines(List<Marker> markers)
    {
        return markers.stream()
            .map(marker -> (Integer)marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE))
            .sorted()
            .collect(Collectors.toList());
    }

    private static String texts(List<Marker> markers)
    {
        return markers.stream().map(Marker::getMessage).collect(Collectors.joining("; "));
    }

    /**
     * A compliant module has no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 0, markers.size());
    }
}
