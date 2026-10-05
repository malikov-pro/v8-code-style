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
 *     malikov-pro - port of the BSL Language Server diagnostic UsingHardcodeNetworkAddress
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.UsingHardcodeNetworkAddressCheck;

/**
 * Tests for {@link UsingHardcodeNetworkAddressCheck} — port of the BSL
 * Language Server diagnostic UsingHardcodeNetworkAddress.
 *
 * @author malikov-pro
 */
public class UsingHardcodeNetworkAddressCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "using-hardcode-network-address.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "using-hardcode-network-address-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public UsingHardcodeNetworkAddressCheckTest()
    {
        super(UsingHardcodeNetworkAddressCheck.class);
    }

    /**
     * IPv4 literals are reported; version-like values, statements with the
     * exclusion word and URLs are not (2 markers).
     */
    @Test
    public void testHardcodeAddressesReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 2, markers.size());
    }

    /**
     * Function results and version-like values produce no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 0, markers.size());
    }
}
