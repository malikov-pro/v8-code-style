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

import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.UnaryPlusInConcatenationCheck;

/** Valid-AST LS cases, numeric exception and precise incremental marker ranges. */
public class UnaryPlusInConcatenationCheckTest
    extends AbstractSingleModuleTestBase
{
    public UnaryPlusInConcatenationCheckTest()
    {
        super(UnaryPlusInConcatenationCheck.class);
    }

    @Test
    public void testCorpusAndModuleBody() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "unary-plus-in-concatenation.bsl"); //$NON-NLS-1$
        assertMarkers(List.of(2, 3, 4, 6, 17));
    }

    @Test
    public void testIncrementalUpdates() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "unary-plus-incremental-clean.bsl"); //$NON-NLS-1$
        assertMarkers(List.of());
        updateModule(FOLDER_RESOURCE + "unary-plus-incremental-bad.bsl"); //$NON-NLS-1$
        assertMarkers(List.of(2));
        updateModule(FOLDER_RESOURCE + "unary-plus-incremental-clean.bsl"); //$NON-NLS-1$
        assertMarkers(List.of());
        updateModule(FOLDER_RESOURCE + "unary-plus-incremental-bad.bsl"); //$NON-NLS-1$
        assertMarkers(List.of(2));
    }

    private void assertMarkers(List<Integer> expected)
    {
        INode node = NodeModelUtils.findActualNodeFor(getModule());
        for (INode leaf : node.getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(expected, getModuleMarkers().stream().map(marker -> {
            Integer line = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
            return line;
        }).sorted().toList());
        for (var marker : getModuleMarkers())
        {
            Integer offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            Integer length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            assertEquals(Integer.valueOf(1), length);
            assertEquals("+", node.getRootNode().getText().substring(offset, offset + length)); //$NON-NLS-1$
        }
    }
}
