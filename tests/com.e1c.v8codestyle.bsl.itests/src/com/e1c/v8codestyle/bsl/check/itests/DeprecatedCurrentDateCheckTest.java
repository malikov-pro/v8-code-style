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
 *     malikov-pro - port of the BSL Language Server diagnostic DeprecatedCurrentDate
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.DeprecatedCurrentDateCheck;

/**
 * Tests for {@link DeprecatedCurrentDateCheck} — port of the BSL Language
 * Server diagnostic DeprecatedCurrentDate.
 *
 * @author malikov-pro
 */
public class DeprecatedCurrentDateCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "deprecated-current-date.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "deprecated-current-date-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public DeprecatedCurrentDateCheckTest()
    {
        super(DeprecatedCurrentDateCheck.class);
    }

    /**
     * The deprecated CurrentDate is reported; CurrentSessionDate is not
     * (1 marker).
     */
    @Test
    public void testDeprecatedCurrentDateReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 1, markers.size());
    }

    /**
     * CurrentSessionDate calls produce no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 0, markers.size());
    }
}
