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
 *     malikov-pro - port of the APK check АПК_01192
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.ApkSumInQueryCheck;

/**
 * Tests for {@link ApkSumInQueryCheck} — port of the APK check АПК_01192
 * (standard 787: use the QUANTITY function instead of SUM to count records).
 *
 * @author malikov-pro
 */
public class ApkSumInQueryCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_SUM = FOLDER_RESOURCE + "apk-01192-summa-in-query.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-01192-summa-in-query-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public ApkSumInQueryCheckTest()
    {
        super(ApkSumInQueryCheck.class);
    }

    /**
     * Each call of SUM()/СУММА() with a constant numeric operand other than 0
     * in a query text is reported separately.
     */
    @Test
    public void testSumWithNumericOperandReported() throws Exception
    {
        updateModule(RESOURCE_SUM);

        List<Marker> markers = getModuleMarkers();
        assertEquals(3, markers.size());
        assertTrue(markers.stream().allMatch(m -> m.getMessage() != null && !m.getMessage().isEmpty()));
    }

    /**
     * QUANTITY usage, СУММА(0) and non-numeric operands produce no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
