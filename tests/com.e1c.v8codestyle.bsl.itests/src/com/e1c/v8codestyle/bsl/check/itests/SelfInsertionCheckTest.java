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
 *     malikov-pro - port of the BSL Language Server diagnostic SelfInsertion
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.SelfInsertionCheck;

/**
 * Tests for {@link SelfInsertionCheck} — port of the BSL Language Server
 * diagnostic SelfInsertion (inserting a collection into itself).
 *
 * @author malikov-pro
 */
public class SelfInsertionCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_INSERT = FOLDER_RESOURCE + "self-insertion.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "self-insertion-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public SelfInsertionCheckTest()
    {
        super(SelfInsertionCheck.class);
    }

    /**
     * Inserting a collection into itself via Add/Insert methods is reported
     * for each such call.
     */
    @Test
    public void testSelfInsertionReported() throws Exception
    {
        updateModule(RESOURCE_INSERT);

        List<Marker> markers = getModuleMarkers();
        assertEquals(2, markers.size());
    }

    /**
     * Adding other values to a collection produces no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(0, markers.size());
    }
}
