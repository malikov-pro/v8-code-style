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
import com.e1c.v8codestyle.bsl.check.CanonicalSpellingKeywordsCheck;

/** Pinned LS canonical keyword corpus with exact keyword ranges. */
public class CanonicalSpellingKeywordsCheckTest
    extends AbstractSingleModuleTestBase
{
    public CanonicalSpellingKeywordsCheckTest()
    {
        super(CanonicalSpellingKeywordsCheck.class);
    }

    @Test
    public void testCorpus() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "canonical-spelling-keywords.bsl"); //$NON-NLS-1$
        INode node = NodeModelUtils.findActualNodeFor(getModule());
        for (INode leaf : node.getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(127, getModuleMarkers().size());
        assertMarker(9, 151, 5);
        assertMarker(16, 280, 12);
        assertMarker(36, 684, 4);
        assertMarker(36, 700, 5);
        assertMarker(17, 302, 5);
    }

    @Test
    public void testIncrementalUpdates() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "canonical-spelling-clean.bsl"); //$NON-NLS-1$
        assertEquals(0, getModuleMarkers().size());
        updateModule(FOLDER_RESOURCE + "canonical-spelling-bad.bsl"); //$NON-NLS-1$
        assertEquals(2, getModuleMarkers().size());
        updateModule(FOLDER_RESOURCE + "canonical-spelling-clean.bsl"); //$NON-NLS-1$
        assertEquals(0, getModuleMarkers().size());
    }

    private void assertMarker(int line, int offset, int length)
    {
        boolean found = getModuleMarkers().stream().anyMatch(marker ->
        {
            Integer markerLine = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
            Integer markerOffset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            Integer markerLength = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            return Integer.valueOf(line).equals(markerLine) && Integer.valueOf(offset).equals(markerOffset)
                && Integer.valueOf(length).equals(markerLength);
        });
        org.junit.Assert.assertTrue("missing marker at line " + line, found); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
