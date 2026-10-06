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
 *     malikov-pro - port of the APK check АПК_00460
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.OverridableModuleNonDeprecatedFunctionCheck;

/**
 * Tests for {@link OverridableModuleNonDeprecatedFunctionCheck} — port of
 * the APK check АПК_00460, sub-item 5 (standard 554: an overridable common
 * module has no functions except deprecated ones).
 *
 * @author malikov-pro
 */
public class OverridableModuleNonDeprecatedFunctionCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_FUNCTIONS = FOLDER_RESOURCE + "apk-00460-non-deprecated-functions.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-00460-non-deprecated-functions-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public OverridableModuleNonDeprecatedFunctionCheckTest()
    {
        super(OverridableModuleNonDeprecatedFunctionCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "OverridableCommonModules"; //$NON-NLS-1$
    }

    @Override
    protected String getModuleFileName()
    {
        return "/src/CommonModules/OverridableModule/Module.bsl"; //$NON-NLS-1$
    }

    /**
     * A non-deprecated function is reported; functions marked with
     * the "Deprecated." comment or placed in the "Deprecated" region
     * are not reported.
     */
    @Test
    public void testNonDeprecatedFunctionReported() throws Exception
    {
        updateModule(RESOURCE_FUNCTIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(1, markers.size());
    }

    /**
     * A module without functions produces no issues.
     */
    @Test
    public void testNoFunctionsHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
