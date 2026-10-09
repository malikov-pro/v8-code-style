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
 *     malikov-pro - port of the upstream issue #633 (std 644, APK 474)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.DfioNoConsumerCheck;

/**
 * Tests for {@link DfioNoConsumerCheck} — an export method inside the
 * «ДляВызоваИзДругихПодсистем» region must have a consumer marking
 * comment above its declaration (standard 644, clause 2.2, upstream
 * issue #633).
 *
 * @author malikov-pro
 */
public class DfioNoConsumerCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_NO_CONSUMER = FOLDER_RESOURCE + "up-633-dfio-no-consumer.bsl"; //$NON-NLS-1$

    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "up-633-dfio-no-consumer-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public DfioNoConsumerCheckTest()
    {
        super(DfioNoConsumerCheck.class);
    }

    /**
     * An export method with no comment at all and an export method with
     * an ordinary text comment are reported — one issue per method.
     */
    @Test
    public void testMissingConsumerCommentReported() throws Exception
    {
        updateModule(RESOURCE_NO_CONSUMER);

        List<Marker> markers = getModuleMarkers();
        assertEquals(2, markers.size());
    }

    /**
     * A marking comment (also followed by description lines) satisfies
     * the requirement; a non-export method is not checked.
     */
    @Test
    public void testMarkingCommentsHaveNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
