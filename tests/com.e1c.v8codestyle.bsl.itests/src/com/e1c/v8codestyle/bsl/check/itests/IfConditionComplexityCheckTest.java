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
 *     malikov-pro - port of the BSL Language Server diagnostic IfConditionComplexityCheck
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.IfConditionComplexityCheck;

/**
 * Tests for {@link IfConditionComplexityCheck} — port of the BSL Language Server diagnostic IfConditionComplexityCheck.
 *
 * @author malikov-pro
 */
public class IfConditionComplexityCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "if-condition-complexity.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "if-condition-complexity-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public IfConditionComplexityCheckTest()
    {
        super(IfConditionComplexityCheck.class);
    }

    /**
     * A condition with 4 And operations (max 3) is reported.
     */
    @Test
    public void testViolationsReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 1, markers.size());
    }

    /**
     * A compliant module has no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 0, markers.size());
    }

    /** NOT preserves the AND/OR count, including ElsIf and the clean threshold. */
    @Test
    public void testNegatedConditions() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "if-condition-complexity-negation.bsl"); //$NON-NLS-1$
        for (INode leaf : NodeModelUtils.findActualNodeFor(getModule()).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        List<Integer> lines = getModuleMarkers().stream().map(marker -> {
            Integer line = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
            return line;
        }).sorted().toList();
        assertEquals(List.of(2, 4, 7), lines);
    }
}
