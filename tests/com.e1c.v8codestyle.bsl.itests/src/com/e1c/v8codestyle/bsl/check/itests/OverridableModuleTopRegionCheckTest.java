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
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.OverridableModuleTopRegionCheck;

/**
 * Tests for {@link OverridableModuleTopRegionCheck} — port of the APK check
 * АПК_00460, sub-item 7 (standard 554: an overridable common module has
 * no top-level regions except "ПрограммныйИнтерфейс"/"Public").
 *
 * @author malikov-pro
 */
public class OverridableModuleTopRegionCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_TOP_REGION = FOLDER_RESOURCE + "apk-00460-top-region.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-00460-top-region-clean.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN_RU = FOLDER_RESOURCE + "apk-00460-top-region-clean-ru.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public OverridableModuleTopRegionCheckTest()
    {
        super(OverridableModuleTopRegionCheck.class);
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
     * A top-level region other than "Public" is reported; a nested region
     * (inside another region) is allowed.
     */
    @Test
    public void testTopLevelRegionReported() throws Exception
    {
        updateModule(RESOURCE_TOP_REGION);

        List<Marker> markers = getModuleMarkers();
        assertEquals(1, markers.size());
        assertEquals(Integer.valueOf(11), markers.get(0).getExtraInfo().get(StandardExtraInfo.TEXT_LINE));
    }

    /**
     * A module with the "Public" top-level region only produces no issues.
     */
    @Test
    public void testPublicRegionOnlyHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }

    /**
     * The Russian public region name «ПрограммныйИнтерфейс» is allowed
     * as well.
     */
    @Test
    public void testRussianPublicRegionNameHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN_RU);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
