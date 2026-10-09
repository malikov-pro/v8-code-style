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
 *     malikov-pro - port of the upstream issue #632 (std 644, APK 473)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.DfioNotInPublicRegionCheck;

/**
 * Tests for {@link DfioNotInPublicRegionCheck} — the
 * «ДляВызоваИзДругихПодсистем» region must be nested inside the
 * «ПрограммныйИнтерфейс» (Public) region (standard 644, clause 2.2,
 * upstream issue #632).
 *
 * @author malikov-pro
 */
public class DfioNotInPublicRegionCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_NOT_IN_PUBLIC = FOLDER_RESOURCE + "up-632-dfio-not-in-public.bsl"; //$NON-NLS-1$

    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "up-632-dfio-not-in-public-clean.bsl"; //$NON-NLS-1$

    private static final String RESOURCE_CLEAN_EN = FOLDER_RESOURCE + "up-632-dfio-not-in-public-clean-en.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public DfioNotInPublicRegionCheckTest()
    {
        super(DfioNotInPublicRegionCheck.class);
    }

    /**
     * A top-level region and a region nested into a non-public region
     * are reported — one issue per region.
     */
    @Test
    public void testRegionOutsidePublicReported() throws Exception
    {
        updateModule(RESOURCE_NOT_IN_PUBLIC);

        List<Marker> markers = getModuleMarkers();
        assertEquals(2, markers.size());
    }

    /**
     * The Russian region names «ПрограммныйИнтерфейс» →
     * «ДляВызоваИзДругихПодсистем» produce no issues.
     */
    @Test
    public void testRussianNamesHaveNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }

    /**
     * The English region names Public → InterfaceImplementation produce
     * no issues.
     */
    @Test
    public void testEnglishNamesHaveNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN_EN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
