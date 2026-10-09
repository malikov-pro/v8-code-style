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
 *     malikov-pro - port of the upstream issue #747 (standard 761)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.NstrUnprintableCharsCheck;

/**
 * Tests for the {@link NstrUnprintableCharsCheck} check.
 *
 * @author malikov-pro
 */
public class NstrUnprintableCharsCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATING = FOLDER_RESOURCE + "up-747-nstr-unprintable-chars.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "up-747-nstr-unprintable-chars-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public NstrUnprintableCharsCheckTest()
    {
        super(NstrUnprintableCharsCheck.class);
    }

    /**
     * A message text of a language entry that starts or ends with a space,
     * a tab or a line feed (including a multiline text with an edge line
     * feed) is reported, one issue per string literal.
     */
    @Test
    public void testViolatingLocalizedStrings() throws Exception
    {
        updateModule(RESOURCE_VIOLATING);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 6, markers.size());
        assertTrue(markers.stream().allMatch(m -> m.getMessage() != null && !m.getMessage().isEmpty()));
    }

    /**
     * Localized strings without edge whitespace, with interior whitespace,
     * with several languages and non-language characters moved into
     * separate string literals produce no issues; malformed entries and
     * non-literal first parameters are not checked here.
     */
    @Test
    public void testCleanLocalizedStrings() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
