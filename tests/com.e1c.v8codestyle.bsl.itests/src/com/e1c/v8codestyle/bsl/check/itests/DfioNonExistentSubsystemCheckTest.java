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
 *     malikov-pro - port of the upstream issue #634 (std 644, APK 475)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.DfioNonExistentSubsystemCheck;

/**
 * Tests for {@link DfioNonExistentSubsystemCheck} — a consumer marking
 * comment inside the «ДляВызоваИзДругихПодсистем» region must name
 * subsystems existing in the configuration (standard 644, clause 2.2,
 * upstream issue #634). The test project contains the subsystem tree
 * "StandardSubsystems" with the nested "BatchObjectModification".
 *
 * @author malikov-pro
 */
public class DfioNonExistentSubsystemCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String PROJECT_NAME = "DfioSubsystems"; //$NON-NLS-1$

    private static final String RESOURCE_NON_EXISTENT = FOLDER_RESOURCE + "up-634-dfio-nonexistent-subsystem.bsl"; //$NON-NLS-1$

    private static final String RESOURCE_CLEAN =
        FOLDER_RESOURCE + "up-634-dfio-nonexistent-subsystem-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public DfioNonExistentSubsystemCheckTest()
    {
        super(DfioNonExistentSubsystemCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * A marking comment naming a non-existing subsystem (unknown nested
     * subsystem or unknown top-level subsystem) is reported — one issue
     * per comment.
     */
    @Test
    public void testNonExistentSubsystemReported() throws Exception
    {
        updateModule(RESOURCE_NON_EXISTENT);

        List<Marker> markers = getModuleMarkers();
        assertEquals(2, markers.size());
    }

    /**
     * Existing top-level and nested subsystems resolve; an ordinary text
     * comment is not treated as a marking and is not reported.
     */
    @Test
    public void testExistingSubsystemsHaveNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
