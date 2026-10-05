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
 *     malikov-pro - port of the BSL Language Server diagnostic IfElseDuplicatedCodeBlock
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.IfElseDuplicatedCodeBlockCheck;

/**
 * Tests for {@link IfElseDuplicatedCodeBlockCheck} — port of the BSL Language
 * Server diagnostic IfElseDuplicatedCodeBlock.
 *
 * @author malikov-pro
 */
public class IfElseDuplicatedCodeBlockCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "if-else-duplicated-code-block.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "if-else-duplicated-code-block-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public IfElseDuplicatedCodeBlockCheckTest()
    {
        super(IfElseDuplicatedCodeBlockCheck.class);
    }

    /**
     * Each duplicated branch is reported: the ElsIf block repeats the If block
     * and the Else block repeats the second ElsIf block (2 markers).
     */
    @Test
    public void testDuplicatedBlocksReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(2, markers.size());
    }

    /**
     * Distinct branches produce no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(0, markers.size());
    }
}
