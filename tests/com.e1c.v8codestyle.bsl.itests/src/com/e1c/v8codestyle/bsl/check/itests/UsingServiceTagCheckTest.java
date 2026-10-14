/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
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
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com.e1c.v8codestyle.bsl.check.UsingServiceTagCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class UsingServiceTagCheckTest extends AbstractSingleModuleTestBase
{
    public UsingServiceTagCheckTest()
    {
        super(UsingServiceTagCheck.class);
    }

    @Test
    public void testCorpusAndMask() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "using-service-tag.bsl"); //$NON-NLS-1$
        ICheckSettings settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        String original = settings.getParameters().get(UsingServiceTagCheck.PARAM_SERVICE_TAGS).getValue();
        try
        {
            assertLines(List.of(1, 3, 6, 7, 8, 9));
            setMask(settings, "CUSTOM"); //$NON-NLS-1$
            assertLines(List.of(10));
            setMask(settings, "todo"); //$NON-NLS-1$
            assertLines(List.of(1));
            setMask(settings, "["); //$NON-NLS-1$
            assertLines(List.of());
            setMask(settings, ""); //$NON-NLS-1$
            assertLines(List.of(1, 3, 4, 6, 7, 8, 9, 10));
        }
        finally
        {
            setMask(settings, original);
        }
    }

    @Test
    public void testIncrementalComment() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "service-tag-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(2));
        updateModule(FOLDER_RESOURCE + "service-tag-clean.bsl"); //$NON-NLS-1$
        assertLines(List.of());
        updateModule(FOLDER_RESOURCE + "service-tag-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(2));
    }

    private void setMask(ICheckSettings settings, String value)
    {
        settings.getParameters().get(UsingServiceTagCheck.PARAM_SERVICE_TAGS).setValue(value);
        checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
        waitForDD(getProject());
    }

    private void assertLines(List<Integer> expected)
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
            String text = node.getRootNode().getText().substring(offset, offset + length);
            assertEquals(true, text.startsWith("//")); //$NON-NLS-1$
            assertEquals(false, text.endsWith("\n")); //$NON-NLS-1$
        }
    }
}
