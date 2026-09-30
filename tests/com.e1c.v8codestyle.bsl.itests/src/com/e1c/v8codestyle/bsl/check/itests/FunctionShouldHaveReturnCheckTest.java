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
 *     malikov-pro - port of the BSL Language Server diagnostic FunctionShouldHaveReturn
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.FunctionShouldHaveReturnCheck;

/**
 * Tests for {@link FunctionShouldHaveReturnCheck} — port of the BSL Language
 * Server diagnostic FunctionShouldHaveReturn (a function without a "Return"
 * statement).
 *
 * @author malikov-pro
 */
public class FunctionShouldHaveReturnCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_NO_RETURN = FOLDER_RESOURCE + "function-should-have-return.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "function-should-have-return-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public FunctionShouldHaveReturnCheckTest()
    {
        super(FunctionShouldHaveReturnCheck.class);
    }

    /**
     * A function without any "Return" statement is reported once;
     * functions with "Return" and procedures are not reported.
     */
    @Test
    public void testFunctionWithoutReturnReported() throws Exception
    {
        updateModule(RESOURCE_NO_RETURN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(1, markers.size());
        assertTrue(markers.stream().allMatch(m -> m.getMessage() != null && !m.getMessage().isEmpty()));
    }

    /**
     * A module where every function has "Return" produces no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
