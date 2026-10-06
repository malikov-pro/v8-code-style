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
 *     malikov-pro - port of the APK check АПК_01216
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.ApkQueryFieldAliasCheck;

/**
 * Tests for {@link ApkQueryFieldAliasCheck} — port of the APK check АПК_01216
 * (standard 758: meaningful aliases of query sources and fields, no metadata
 * object class names as aliases).
 *
 * @author malikov-pro
 */
public class ApkQueryFieldAliasCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_ALIAS = FOLDER_RESOURCE + "apk-01216-query-field-alias.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-01216-query-field-alias-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public ApkQueryFieldAliasCheckTest()
    {
        super(ApkQueryFieldAliasCheck.class);
    }

    /**
     * Each alias after the КАК/AS keyword that matches a metadata object class
     * name (in Russian or English, in any case) is reported separately.
     */
    @Test
    public void testMetadataClassAliasReported() throws Exception
    {
        updateModule(RESOURCE_ALIAS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(3, markers.size());
        assertTrue(markers.stream().allMatch(m -> m.getMessage() != null && !m.getMessage().isEmpty()));
    }

    /**
     * Meaningful aliases, class names of query sources and «КАК …» in comments
     * produce no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
