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
 *     malikov-pro - port of the BSL Language Server diagnostic UselessTernaryOperator
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.UselessTernaryOperatorCheck;

/**
 * Tests for {@link UselessTernaryOperatorCheck} — port of the BSL Language
 * Server diagnostic UselessTernaryOperator.
 *
 * @author malikov-pro
 */
public class UselessTernaryOperatorCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "useless-ternary-operator.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "useless-ternary-operator-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public UselessTernaryOperatorCheckTest()
    {
        super(UselessTernaryOperatorCheck.class);
    }

    /**
     * Each useless ternary is reported: constant condition, (X, True, False),
     * (X, False, True) and identical branches (4 in the module).
     */
    @Test
    public void testUselessTernariesReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(4, markers.size());
    }

    /**
     * Meaningful ternaries produce no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(0, markers.size());
    }
}
