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
import com.e1c.v8codestyle.bsl.check.OverridableModuleProcedureNameCheck;

/**
 * Tests for {@link OverridableModuleProcedureNameCheck} — port of the APK
 * check АПК_00460, sub-item 2 (standard 554: the called method name equals
 * the overriding procedure name).
 *
 * @author malikov-pro
 */
public class OverridableModuleProcedureNameCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "apk-00460-procedure-name-match.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_VIOLATIONS_IMPLICIT =
        FOLDER_RESOURCE + "apk-00460-procedure-name-match-implicit.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-00460-procedure-name-match-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public OverridableModuleProcedureNameCheckTest()
    {
        super(OverridableModuleProcedureNameCheck.class);
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
     * A direct call of a differently named method of the library module is reported.
     */
    @Test
    public void testDifferentNameDirectCallReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 1, markers.size());
    }

    /**
     * A call of a differently named method via Common.CommonModule("Module") is reported.
     */
    @Test
    public void testDifferentNameImplicitCallReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS_IMPLICIT);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 1, markers.size());
    }

    /**
     * A call of the same-named pair method has no issues.
     */
    @Test
    public void testSameNameCallHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), markers.isEmpty());
    }
}
