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
 *     malikov-pro - port of the BSL Language Server diagnostic MagicNumberCheck
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.MagicNumberCheck;

/**
 * Tests for {@link MagicNumberCheck} — port of the BSL Language Server diagnostic MagicNumberCheck.
 *
 * @author malikov-pro
 */
public class MagicNumberCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "magic-number.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "magic-number-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public MagicNumberCheckTest()
    {
        super(MagicNumberCheck.class);
    }

    /**
     * The magic numbers 100, 7, -0.5 and -7 are reported with their sign; the authorized -1, the default parameter
     * value -2 and the index access are not.
     */
    @Test
    public void testViolationsReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(texts(markers), List.of(4, 10, 11, 12), lines(markers));
        assertEquals(List.of("Магическое число «-0.5»", "Магическое число «-7»", "Магическое число «100»",
            "Магическое число «7»"), messages(markers));
    }

    private static List<Integer> lines(List<Marker> markers)
    {
        return markers.stream()
            .map(marker -> (Integer)marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE))
            .sorted()
            .collect(Collectors.toList());
    }

    private static List<String> messages(List<Marker> markers)
    {
        return markers.stream().map(Marker::getMessage).sorted().collect(Collectors.toList());
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
