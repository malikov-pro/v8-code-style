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
 *     malikov-pro - port of the BSL Language Server diagnostic UsingHardcodeSecretInformation
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.UsingHardcodeSecretInformationCheck;

/**
 * Tests for {@link UsingHardcodeSecretInformationCheck} — port of the BSL
 * Language Server diagnostic UsingHardcodeSecretInformation.
 *
 * @author malikov-pro
 */
public class UsingHardcodeSecretInformationCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "using-hardcode-secret-information.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "using-hardcode-secret-information-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public UsingHardcodeSecretInformationCheckTest()
    {
        super(UsingHardcodeSecretInformationCheck.class);
    }

    /**
     * All storage forms are reported: variable, Insert, map key, property,
     * Structure constructor, connection password (5 markers).
     */
    @Test
    public void testHardcodeSecretsReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 5, markers.size());
    }

    /**
     * Masked values, non-secret names and function results produce no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 0, markers.size());
    }
}
