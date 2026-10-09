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
 *     malikov-pro - port of the upstream issue #746 (standard 761)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.NstrSyntaxCheck;

/**
 * Tests for the {@link NstrSyntaxCheck} check.
 *
 * @author malikov-pro
 */
public class NstrSyntaxCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATING = FOLDER_RESOURCE + "up-746-nstr-syntax.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "up-746-nstr-syntax-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public NstrSyntaxCheckTest()
    {
        super(NstrSyntaxCheck.class);
    }

    /**
     * A localized string in double quotes or without quotes, without a
     * language code, with a broken or unterminated message text, an empty
     * string and a trailing separator are each reported.
     */
    @Test
    public void testViolatingLocalizedStrings() throws Exception
    {
        updateModule(RESOURCE_VIOLATING);

        List<Marker> markers = getModuleMarkers();
        assertEquals(7, markers.size());
        assertTrue(markers.stream().allMatch(m -> m.getMessage() != null && !m.getMessage().isEmpty()));
    }

    /**
     * Correct localized strings with one or several languages, doubled
     * single quotes in the text, a region suffix in the language code, a
     * second function parameter and a multiline text produce no issues.
     */
    @Test
    public void testCleanLocalizedStrings() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
