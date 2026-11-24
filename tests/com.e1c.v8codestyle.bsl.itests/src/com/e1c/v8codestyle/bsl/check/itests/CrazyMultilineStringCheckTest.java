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
import com.e1c.v8codestyle.bsl.check.CrazyMultilineStringCheck;

/** Pinned LS multiline and adjacent-literal corpus. */
public class CrazyMultilineStringCheckTest
    extends AbstractSingleModuleTestBase
{
    public CrazyMultilineStringCheckTest()
    {
        super(CrazyMultilineStringCheck.class);
    }

    @Test
    public void testCorpus() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "crazy-multiline-string.bsl"); //$NON-NLS-1$
        INode node = NodeModelUtils.findActualNodeFor(getModule());
        for (INode leaf : node.getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(List.of(2, 3, 7, 13), getModuleMarkers().stream()
            .map(marker -> (Integer)marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE))
            .sorted().toList());
    }

    @Test
    public void testIncrementalUpdates() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "crazy-multiline-clean.bsl"); //$NON-NLS-1$
        assertEquals(0, getModuleMarkers().size());
        updateModule(FOLDER_RESOURCE + "crazy-multiline-bad.bsl"); //$NON-NLS-1$
        assertEquals(1, getModuleMarkers().size());
        updateModule(FOLDER_RESOURCE + "crazy-multiline-clean.bsl"); //$NON-NLS-1$
        assertEquals(0, getModuleMarkers().size());
    }
}
