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
import com.e1c.v8codestyle.bsl.check.OverridableModuleNonExportMethodCheck;

/**
 * Tests for {@link OverridableModuleNonExportMethodCheck} — port of
 * the APK check АПК_00460, sub-item 6 (standard 554: an overridable common
 * module has no non-export methods).
 *
 * @author malikov-pro
 */
public class OverridableModuleNonExportMethodCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_NON_EXPORT = FOLDER_RESOURCE + "apk-00460-non-export-methods.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-00460-non-export-methods-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public OverridableModuleNonExportMethodCheckTest()
    {
        super(OverridableModuleNonExportMethodCheck.class);
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
     * Each non-export method of the overridable common module is reported.
     */
    @Test
    public void testNonExportMethodReported() throws Exception
    {
        updateModule(RESOURCE_NON_EXPORT);

        List<Marker> markers = getModuleMarkers();
        assertEquals(1, markers.size());
    }

    /**
     * A module with export methods only produces no issues.
     */
    @Test
    public void testExportMethodsOnlyHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
