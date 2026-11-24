/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.MissingVariablesDescriptionCheck;

/** Pinned LS corpus, comment attachment through annotations and incremental updates. */
public class MissingVariablesDescriptionCheckTest
    extends AbstractSingleModuleTestBase
{
    public MissingVariablesDescriptionCheckTest()
    {
        super(MissingVariablesDescriptionCheck.class);
    }

    @Test
    public void testCorpus() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "missing-variables-description.bsl"); //$NON-NLS-1$
        assertLines(List.of(2, 4, 18, 22, 38));
    }

    @Test
    public void testBomDoesNotBreakTheDescription() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "missing-variables-description-bom.bsl"); //$NON-NLS-1$
        assertLines(List.of());
    }

    @Test
    public void testIncrementalUpdates() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "missing-variables-description-clean.bsl"); //$NON-NLS-1$
        assertLines(List.of());
        updateModule(FOLDER_RESOURCE + "missing-variables-description-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(1));
        updateModule(FOLDER_RESOURCE + "missing-variables-description-clean.bsl"); //$NON-NLS-1$
        assertLines(List.of());
        setCheckEnable(false);
        updateModule(FOLDER_RESOURCE + "missing-variables-description-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of());
        setCheckEnable(true);
        assertLines(List.of(1));
    }

    private void assertLines(List<Integer> expected)
    {
        INode node = NodeModelUtils.findActualNodeFor(getModule());
        for (INode leaf : node.getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(expected, getModuleMarkers().stream()
            .map(marker -> (Integer)marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE))
            .sorted().toList());
    }
}
