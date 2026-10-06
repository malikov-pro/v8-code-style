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
 *     malikov-pro - port of the APK check АПК_01171
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.ApkStringConcatInLoopCheck;

/**
 * Tests for {@link ApkStringConcatInLoopCheck} — port of the APK check
 * АПК_01171 (standard 782: bulk string concatenation).
 *
 * @author malikov-pro
 */
public class ApkStringConcatInLoopCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "apk-01171-string-concat-in-loop.bsl"; //$NON-NLS-1$

    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-01171-string-concat-in-loop-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public ApkStringConcatInLoopCheckTest()
    {
        super(ApkStringConcatInLoopCheck.class);
    }

    /**
     * Each loop with a self-accumulating assignment (Var = Var + ... on the
     * left and in the right addition) is reported once: one per loop, not per
     * statement. An assignment in the inner loop or in a nested If is
     * attributed to the innermost loop only.
     */
    @Test
    public void testConcatInLoopReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(4, markers.size());
        assertTrue(markers.stream().allMatch(m -> m.getMessage() != null && !m.getMessage().isEmpty()));
    }

    /**
     * Numeric counters (Counter = Counter + 1) and assignments without the
     * left-hand variable in the right part produce no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
