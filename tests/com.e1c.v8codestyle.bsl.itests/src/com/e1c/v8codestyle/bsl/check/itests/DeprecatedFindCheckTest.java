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
 *     malikov-pro - port of the BSL Language Server diagnostic DeprecatedFind
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.DeprecatedFindCheck;

/**
 * Tests for {@link DeprecatedFindCheck} — port of the BSL Language Server
 * diagnostic DeprecatedFind.
 *
 * @author malikov-pro
 */
public class DeprecatedFindCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "deprecated-find.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "deprecated-find-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public DeprecatedFindCheckTest()
    {
        super(DeprecatedFindCheck.class);
    }

    /**
     * The deprecated global Find is reported; StrFind is not (1 marker).
     */
    @Test
    public void testDeprecatedFindReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 1, markers.size());
    }

    /**
     * StrFind calls produce no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 0, markers.size());
    }
}
