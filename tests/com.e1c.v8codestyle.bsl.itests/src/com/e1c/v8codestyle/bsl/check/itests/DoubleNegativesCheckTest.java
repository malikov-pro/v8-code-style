/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - initial API and implementation
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
import com.e1c.v8codestyle.bsl.check.DoubleNegativesCheck;

/** Regression corpus for AST-based double negatives. */
public class DoubleNegativesCheckTest
    extends AbstractSingleModuleTestBase
{
    public DoubleNegativesCheckTest()
    {
        super(DoubleNegativesCheck.class);
    }

    /** @throws Exception if module validation fails */
    @Test
    public void testCorpusAndPositions() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "double-negatives-clean.bsl"); //$NON-NLS-1$
        assertEquals(0, getModuleMarkers().size());
        updateModule(FOLDER_RESOURCE + "double-negatives.bsl"); //$NON-NLS-1$
        for (INode leaf : NodeModelUtils.findActualNodeFor(getModule()).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        List<Marker> markers = getModuleMarkers();
        assertEquals(15, markers.size());
        List<Integer> lines = markers.stream().map(marker -> {
            Integer line = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
            return line;
        }).sorted().toList();
        assertEquals(List.of(2, 5, 6, 7, 8, 9, 10, 14, 18, 23, 24, 26, 28, 29, 30), lines);
        updateModule(FOLDER_RESOURCE + "double-negatives-syntax-error.bsl"); //$NON-NLS-1$
        assertEquals(0, getModuleMarkers().size());
    }
}
