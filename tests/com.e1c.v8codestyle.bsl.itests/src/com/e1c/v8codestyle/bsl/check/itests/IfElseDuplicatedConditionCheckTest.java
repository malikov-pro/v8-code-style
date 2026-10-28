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
import static org.junit.Assert.assertNull;

import java.util.List;

import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
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
     * One issue is reported at the first condition of each duplicate group.
     */
    @Test
    public void testDuplicatedConditionsReported() throws Exception
    {
        updateModule(RESOURCE_DUPLICATES);

        List<Marker> markers = getModuleMarkers();
        assertEquals(1, markers.size());
        assertMarkerLines(List.of(3));
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

    @Test
    public void testTokenAndStringEquality() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "duplicated-condition-tokens.bsl"); //$NON-NLS-1$
        // LS anchors the first condition in each duplicate group.
        assertMarkerLines(List.of(2, 17));
    }

    @Test
    public void testIncrementalStringCase() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "duplicated-condition-token-bad.bsl"); //$NON-NLS-1$
        assertMarkerLines(List.of(2));
        updateModule(FOLDER_RESOURCE + "duplicated-condition-token-clean.bsl"); //$NON-NLS-1$
        assertMarkerLines(List.of());
        updateModule(FOLDER_RESOURCE + "duplicated-condition-token-bad.bsl"); //$NON-NLS-1$
        assertMarkerLines(List.of(2));
    }

    private void assertMarkerLines(List<Integer> expected)
    {
        for (INode leaf : NodeModelUtils.findActualNodeFor(getModule()).getLeafNodes())
        {
            assertNull("Syntax at line " + leaf.getStartLine(), leaf.getSyntaxErrorMessage()); //$NON-NLS-1$
        }
        assertEquals(expected, getModuleMarkers().stream().map(marker -> {
            Integer line = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
            return line;
        }).sorted().toList());
        for (var marker : getModuleMarkers())
        {
            Integer offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            Integer length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            String text = NodeModelUtils.findActualNodeFor(getModule()).getRootNode().getText()
                .substring(offset, offset + length);
            assertEquals(true, text.contains("=")); //$NON-NLS-1$
            assertEquals(false, text.contains("Тогда")); //$NON-NLS-1$
        }
    }

    @Test
    public void testOneIssuePerGroupAndModuleBody() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "condition-token-groups.bsl"); //$NON-NLS-1$
        assertMarkerLines(List.of(2, 4, 15));
    }
}
