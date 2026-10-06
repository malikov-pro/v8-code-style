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
 *     malikov-pro - port of the BSL Language Server diagnostic TernaryOperatorUsage
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.TernaryOperatorUsageCheck;

/**
 * Tests for {@link TernaryOperatorUsageCheck} — port of the BSL Language
 * Server diagnostic TernaryOperatorUsage. The check is disabled by default
 * (as in BSL LS); the test base enables the check under test explicitly.
 *
 * @author malikov-pro
 */
public class TernaryOperatorUsageCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "ternary-operator-usage.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "ternary-operator-usage-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public TernaryOperatorUsageCheckTest()
    {
        super(TernaryOperatorUsageCheck.class);
    }

    /**
     * Each ternary operator is reported (3 in the module).
     */
    @Test
    public void testTernariesReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(3, markers.size());
    }

    /**
     * A module without ternary operators has no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(0, markers.size());
    }
}
