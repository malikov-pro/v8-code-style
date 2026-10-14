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
import com.e1c.v8codestyle.bsl.check.TooManyReturnsCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class TooManyReturnsCheckTest extends AbstractSingleModuleTestBase
{
    public TooManyReturnsCheckTest()
    {
        super(TooManyReturnsCheck.class);
    }

    @Test
    public void testBoundaryAndParameter() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "returns-bad.bsl"); //$NON-NLS-1$
        ICheckSettings settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        String original = settings.getParameters().get(TooManyReturnsCheck.PARAM_MAX_RETURNS).getValue();
        assertEquals("3", original); //$NON-NLS-1$
        try
        {
            assertCount(1);
            setMax(settings, "4"); //$NON-NLS-1$
            assertCount(0);
            setMax(settings, "3"); //$NON-NLS-1$
            assertCount(1);
            updateModule(FOLDER_RESOURCE + "returns-clean.bsl"); //$NON-NLS-1$
            assertCount(0);
            updateModule(FOLDER_RESOURCE + "returns-bad.bsl"); //$NON-NLS-1$
            assertCount(1);
        }
        finally
        {
            setMax(settings, original);
        }
    }

    private void setMax(ICheckSettings settings, String value)
    {
        settings.getParameters().get(TooManyReturnsCheck.PARAM_MAX_RETURNS).setValue(value);
        checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
        waitForDD(getProject());
    }

    @Test
    public void testProcedureAndUnreachableReturns() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "returns-procedure.bsl"); //$NON-NLS-1$
        assertCount(1);
        try
        {
            setCheckEnable(false);
            assertCount(0);
            setCheckEnable(true);
            assertCount(1);
        }
        finally
        {
            setCheckEnable(true);
        }
    }

    private void assertCount(int count)
    {
        INode node = NodeModelUtils.findActualNodeFor(getModule());
        for (INode leaf : node.getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(count, getModuleMarkers().size());
        for (var marker : getModuleMarkers())
        {
            Integer offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            Integer length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            assertEquals("Проверка", node.getRootNode().getText().substring(offset, offset + length)); //$NON-NLS-1$
        }
    }
}
