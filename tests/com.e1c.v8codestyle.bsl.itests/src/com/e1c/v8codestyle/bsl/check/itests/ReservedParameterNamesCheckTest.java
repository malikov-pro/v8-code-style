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
import com.e1c.v8codestyle.bsl.check.ReservedParameterNamesCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class ReservedParameterNamesCheckTest extends AbstractSingleModuleTestBase
{
    public ReservedParameterNamesCheckTest()
    {
        super(ReservedParameterNamesCheck.class);
    }

    @Test
    public void testMaskUpdates() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "reserved-parameter-names.bsl"); //$NON-NLS-1$
        ICheckSettings settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        String original = settings.getParameters().get(ReservedParameterNamesCheck.PARAM_RESERVED_WORDS).getValue();
        assertEquals("", original); //$NON-NLS-1$
        try
        {
            assertLines(List.of());
            setMask(settings, "(ВидГруппыФормы|Запрет)"); //$NON-NLS-1$
            assertLines(List.of(1, 1, 5));
            setMask(settings, "ВидГруппы"); //$NON-NLS-1$
            assertLines(List.of());
            setMask(settings, "Запрет"); //$NON-NLS-1$
            assertLines(List.of(1, 5));
            setMask(settings, "["); //$NON-NLS-1$
            assertLines(List.of());
        }
        finally
        {
            setMask(settings, original);
        }
    }

    private void setMask(ICheckSettings settings, String value)
    {
        settings.getParameters().get(ReservedParameterNamesCheck.PARAM_RESERVED_WORDS).setValue(value);
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
            assertEquals(true, node.getRootNode().getText().substring(offset, offset + length).matches("[А-Яа-я]+")); //$NON-NLS-1$
        }
    }
}
