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
 *     malikov-pro - port of the BSL Language Server diagnostic IfElseDuplicatedCondition
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.IfElseDuplicatedConditionCheck;

/**
 * Tests for {@link IfElseDuplicatedConditionCheck} — port of the BSL Language
 * Server diagnostic IfElseDuplicatedCondition (duplicated conditions of the
 * If/ElsIf statement).
 *
 * @author malikov-pro
 */
public class IfElseDuplicatedConditionCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_DUPLICATES = FOLDER_RESOURCE + "if-else-duplicated-condition.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "if-else-duplicated-condition-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public IfElseDuplicatedConditionCheckTest()
    {
        super(IfElseDuplicatedConditionCheck.class);
    }

    /**
     * Each duplicated ElsIf condition is reported (2 duplicates in the module).
     */
    @Test
    public void testDuplicatedConditionsReported() throws Exception
    {
        updateModule(RESOURCE_DUPLICATES);

        List<Marker> markers = getModuleMarkers();
        assertEquals(2, markers.size());
    }

    /**
     * Unique conditions produce no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(0, markers.size());
    }
}
