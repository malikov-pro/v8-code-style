/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the upstream issue #781 (standard 467)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.NotifyChangedHandlerExistCheck;

/**
 * Tests for the {@link NotifyChangedHandlerExistCheck} check against the
 * {@code OverridableCommonModules} test configuration with five server
 * common modules.
 *
 * @author malikov-pro
 */
public class NotifyChangedHandlerExistCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATING = FOLDER_RESOURCE + "up-781-notify-handler-nonexistent.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "up-781-notify-handler-nonexistent-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public NotifyChangedHandlerExistCheckTest()
    {
        super(NotifyChangedHandlerExistCheck.class);
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
     * A handler name that resolves to no export method of any server common
     * module (a simple name and a name of a non-existent module) is
     * reported, one issue per call.
     */
    @Test
    public void testViolatingHandlerNames() throws Exception
    {
        updateModule(RESOURCE_VIOLATING);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 2, markers.size());
        assertTrue(markers.stream().allMatch(m -> m.getMessage() != null && !m.getMessage().isEmpty()));
    }

    /**
     * A simple name of a method of another server common module, a
     * qualified name, the name of a method of the same module, the object
     * form of the call and a dynamic name produce no issues.
     */
    @Test
    public void testCleanHandlerNames() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), markers.isEmpty());
    }
}
