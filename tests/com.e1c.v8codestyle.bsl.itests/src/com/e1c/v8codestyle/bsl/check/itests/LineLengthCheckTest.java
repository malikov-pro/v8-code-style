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
 *     malikov-pro - port of the BSL Language Server diagnostic LineLength
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.LineLengthCheck;

/**
 * Tests for {@link LineLengthCheck} — port of the BSL Language Server
 * diagnostic LineLength (line length limit, simplified).
 *
 * @author malikov-pro
 */
public class LineLengthCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_LONG = FOLDER_RESOURCE + "line-length.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "line-length-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public LineLengthCheckTest()
    {
        super(LineLengthCheck.class);
    }

    /**
     * A module with a line longer than the maximum (120 chars) is reported
     * once.
     */
    @Test
    public void testLongLineReported() throws Exception
    {
        updateModule(RESOURCE_LONG);

        List<Marker> markers = getModuleMarkers();
        assertEquals(1, markers.size());
    }

    /**
     * A module with short lines produces no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(0, markers.size());
    }
}
