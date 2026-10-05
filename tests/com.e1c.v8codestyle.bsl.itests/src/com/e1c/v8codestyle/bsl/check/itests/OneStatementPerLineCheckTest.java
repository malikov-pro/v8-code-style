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
 *     malikov-pro - port of the BSL Language Server diagnostic OneStatementPerLine
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.OneStatementPerLineCheck;

/**
 * Tests for {@link OneStatementPerLineCheck} — port of the BSL Language
 * Server diagnostic OneStatementPerLine.
 *
 * @author malikov-pro
 */
public class OneStatementPerLineCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "one-statement-per-line.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "one-statement-per-line-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public OneStatementPerLineCheckTest()
    {
        super(OneStatementPerLineCheck.class);
    }

    /**
     * Each statement of a multi-statement line is reported (3 in the module).
     */
    @Test
    public void testMultipleStatementsReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(3, markers.size());
    }

    /**
     * A module with a single statement per line has no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(0, markers.size());
    }
}
