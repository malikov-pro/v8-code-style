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
import com.e1c.v8codestyle.bsl.check.IdenticalExpressionsCheck;

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
}
