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
 *     malikov-pro - port of the APK check АПК_00334
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.ApkPostingsWriteOrderCheck;

/**
 * Tests for {@link ApkPostingsWriteOrderCheck} — port of the APK check АПК_00334
 * (standard 450: do not write register record sets explicitly in the posting handler).
 *
 * @author malikov-pro
 */
public class ApkPostingsWriteOrderCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "apk-00334-postings-write-order.bsl"; //$NON-NLS-1$

    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-00334-postings-write-order-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public ApkPostingsWriteOrderCheckTest()
    {
        super(ApkPostingsWriteOrderCheck.class);
    }

    /**
     * Explicit writes of record sets in the posting handler are reported: direct write, write via a local alias
     * and via an alias chain. Writes outside the handler and commented-out writes are not reported.
     */
    @Test
    public void testExplicitPostingsWriteReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 3, markers.size()); //$NON-NLS-1$

        Set<Integer> lines = markers.stream()
            .map(marker -> marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE))
            .filter(Integer.class::isInstance)
            .map(Integer.class::cast)
            .collect(Collectors.toSet());
        assertEquals(Set.of(7, 11, 15), lines);
    }

    /**
     * A module where postings are written by the system and record sets are written outside
     * the posting handler has no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 0, markers.size()); //$NON-NLS-1$
    }
}
