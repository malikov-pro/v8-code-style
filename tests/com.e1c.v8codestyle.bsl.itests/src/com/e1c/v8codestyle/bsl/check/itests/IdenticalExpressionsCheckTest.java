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
 *     malikov-pro - port of the BSL Language Server diagnostic IdenticalExpressions
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
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com.e1c.v8codestyle.bsl.check.IdenticalExpressionsCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Tests for {@link IdenticalExpressionsCheck} — port of the BSL Language
 * Server diagnostic IdenticalExpressions.
 *
 * @author malikov-pro
 */
public class IdenticalExpressionsCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "identical-expressions.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "identical-expressions-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public IdenticalExpressionsCheckTest()
    {
        super(IdenticalExpressionsCheck.class);
    }

    /**
     * "Value = Value" in the If condition and the duplicated right side of And
     * are reported; division by the popular divisor 60 is not (2 markers).
     */
    @Test
    public void testIdenticalExpressionsReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(2, markers.size());
    }

    /**
     * Distinct operands produce no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(0, markers.size());
    }

    /** Nested negation must not hide a binary expression from validation. */
    @Test
    public void testNegatedExpressions() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "identical-expressions-negation.bsl"); //$NON-NLS-1$
        for (INode leaf : NodeModelUtils.findActualNodeFor(getModule()).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        List<Integer> lines = getModuleMarkers().stream().map(marker -> {
            Integer line = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
            return line;
        }).sorted().toList();
        assertEquals(List.of(2, 3, 4, 5, 6), lines);
    }

    @Test
    public void testTokenAndStringEquality() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "identical-expression-tokens.bsl"); //$NON-NLS-1$
        assertMarkerLines(List.of(4, 5, 6, 8));
    }

    @Test
    public void testIncrementalStringCase() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "identical-token-bad.bsl"); //$NON-NLS-1$
        assertMarkerLines(List.of(2));
        updateModule(FOLDER_RESOURCE + "identical-token-clean.bsl"); //$NON-NLS-1$
        assertMarkerLines(List.of());
        updateModule(FOLDER_RESOURCE + "identical-token-bad.bsl"); //$NON-NLS-1$
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
    }

    @Test
    public void testPinnedTreeEqualityRules() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "identical-tree-parity.bsl"); //$NON-NLS-1$
        // LS excludes dereference/index equality even when the text is identical.
        assertMarkerLines(List.of(2, 3, 4, 5, 10, 11, 12));
    }

    @Test
    public void testIncrementalOperandOperation() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "identical-tree-bad.bsl"); //$NON-NLS-1$
        assertMarkerLines(List.of(2));
        updateModule(FOLDER_RESOURCE + "identical-tree-clean.bsl"); //$NON-NLS-1$
        assertMarkerLines(List.of());
        updateModule(FOLDER_RESOURCE + "identical-tree-bad.bsl"); //$NON-NLS-1$
        assertMarkerLines(List.of(2));
    }

    @Test
    public void testTreeComparisonBoundaries() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "identical-tree-boundaries.bsl"); //$NON-NLS-1$
        assertMarkerLines(List.of(2, 4, 7, 8, 9, 12, 13));
    }

    @Test
    public void testPinnedLsLinearCorpus() throws Exception
    {
        // Adapted from LS's fixture, retaining all cases before the preprocessor sections.
        updateModule(FOLDER_RESOURCE + "identical-ls-linear-corpus.bsl"); //$NON-NLS-1$
        assertMarkerLines(List.of(3, 4, 7, 8, 9, 11, 12, 14, 15, 17, 24, 25, 27, 29, 31, 33, 36, 37));
    }

    @Test
    public void testDistinctRepeatedGroupsAndMixedOperators() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "identical-chain-groups.bsl"); //$NON-NLS-1$
        assertMarkerLines(List.of(2, 2));
        assertEquals(2, getModuleMarkers().stream().map(Marker::getMessage).distinct().count());
    }

    @Test
    public void testLiteralAndDivisorBoundaries() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "expression-token-boundaries.bsl"); //$NON-NLS-1$
        ICheckSettings settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        String key = "popularDivisors"; //$NON-NLS-1$
        String original = settings.getParameters().get(key).getValue();
        assertEquals("60, 1024", original); //$NON-NLS-1$
        try
        {
            // Structural comparison ignores redundant parentheses, preserving string contents.
            assertMarkerLines(List.of(2, 7, 10, 14));
            settings.getParameters().get(key).setValue(""); //$NON-NLS-1$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
            assertMarkerLines(List.of(2, 7, 10, 11, 14));
        }
        finally
        {
            settings.getParameters().get(key).setValue(original);
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
        }
    }
}
