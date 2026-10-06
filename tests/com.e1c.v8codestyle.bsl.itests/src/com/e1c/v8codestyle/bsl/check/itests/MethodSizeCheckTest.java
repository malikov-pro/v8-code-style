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
 *     malikov-pro - port of the BSL Language Server diagnostic MethodSize
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.MethodSizeCheck;

/**
 * Tests for {@link MethodSizeCheck} — port of the BSL Language Server
 * diagnostic MethodSize (method body size limit).
 *
 * @author malikov-pro
 */
public class MethodSizeCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_LARGE = FOLDER_RESOURCE + "method-size-large.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "method-size-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public MethodSizeCheckTest()
    {
        super(MethodSizeCheck.class);
    }

    /**
     * A method whose body exceeds the maximum size (200 lines by default)
     * is reported once.
     */
    @Test
    public void testLargeMethodReported() throws Exception
    {
        updateModule(RESOURCE_LARGE);

        List<Marker> markers = getModuleMarkers();
        assertEquals(1, markers.size());
    }

    /**
     * A compact method produces no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(0, markers.size());
    }
}
