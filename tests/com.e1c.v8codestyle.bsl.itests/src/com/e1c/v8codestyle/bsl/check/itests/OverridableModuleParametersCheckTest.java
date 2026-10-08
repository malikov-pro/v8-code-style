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
import java.util.stream.Collectors;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.OverridableModuleParametersCheck;

/**
 * Tests for {@link OverridableModuleParametersCheck} — port of the APK
 * check АПК_00460, sub-item 3 (standard 554: the call passes all parameters
 * of the overriding procedure in the same order).
 *
 * @author malikov-pro
 */
public class OverridableModuleParametersCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS_COUNT = FOLDER_RESOURCE + "apk-00460-parameters-match.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_VIOLATIONS_ORDER =
        FOLDER_RESOURCE + "apk-00460-parameters-match-order.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-00460-parameters-match-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public OverridableModuleParametersCheckTest()
    {
        super(OverridableModuleParametersCheck.class);
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
     * A call via a module variable passing fewer parameters is reported.
     */
    @Test
    public void testMissingParameterReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS_COUNT);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 1, markers.size());
    }

    /**
     * A call passing parameters in a different order is reported.
     */
    @Test
    public void testParametersOrderReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS_ORDER);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 1, markers.size());
    }

    /**
     * A call passing all parameters in the same order has no issues.
     */
    @Test
    public void testAllParametersPassedHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), markers.isEmpty());
    }
}
