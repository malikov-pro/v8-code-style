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
 *     malikov-pro - port of the BSL Language Server diagnostic RewriteMethodParameter
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.RewriteMethodParameterCheck;

/**
 * Tests for {@link RewriteMethodParameterCheck} — port of the BSL Language
 * Server diagnostic RewriteMethodParameter.
 *
 * @author malikov-pro
 */
public class RewriteMethodParameterCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "rewrite-method-parameter.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "rewrite-method-parameter-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public RewriteMethodParameterCheckTest()
    {
        super(RewriteMethodParameterCheck.class);
    }

    /**
     * Each by-value parameter rewritten before its first use is reported
     * (2 parameters in the module).
     */
    @Test
    public void testRewrittenParametersReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(2, markers.size());
    }

    /**
     * Rewrites after the first use and by-reference parameters are not
     * reported.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 0, markers.size());
    }
}
