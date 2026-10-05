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
 *     malikov-pro - port of the BSL Language Server diagnostic IdenticalExpressions
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.IdenticalExpressionsCheck;

/**
 * Tests for {@link IdenticalExpressionsCheck} — port of the BSL Language
 * Server diagnostic IdenticalExpressions.
 *
 * @author malikov-pro
 */
public class IdenticalExpressionsCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "identical-expressions.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "identical-expressions-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public IdenticalExpressionsCheckTest()
    {
        super(IdenticalExpressionsCheck.class);
    }

    /**
     * "Value = Value" in the If condition and the duplicated right side of And
     * are reported; division by the popular divisor 60 is not (2 markers).
     */
    @Test
    public void testIdenticalExpressionsReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(2, markers.size());
    }

    /**
     * Distinct operands produce no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(0, markers.size());
    }
}
