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
import com.e1c.v8codestyle.bsl.check.OverridableModuleRedundantCodeCheck;

/**
 * Tests for {@link OverridableModuleRedundantCodeCheck} — port of the APK
 * check АПК_00460, sub-item 1 (standard 554: a procedure of an overridable
 * common module has no code except the call of the overridden method).
 *
 * @author malikov-pro
 */
public class OverridableModuleRedundantCodeCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "apk-00460-redundant-code.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-00460-redundant-code-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public OverridableModuleRedundantCodeCheckTest()
    {
        super(OverridableModuleRedundantCodeCheck.class);
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
     * An assignment in the overriding procedure is reported as redundant code.
     */
    @Test
    public void testRedundantCodeReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 1, markers.size());
    }

    /**
     * A procedure reduced to the call of the overridden method has no issues;
     * code between the demo example comments is not checked.
     */
    @Test
    public void testForwardingOnlyHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), markers.isEmpty());
    }
}
