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
import com.e1c.v8codestyle.bsl.check.FunctionReturnsSamePrimitiveCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class FunctionReturnsSamePrimitiveCheckTest
    extends AbstractSingleModuleTestBase
{
    public FunctionReturnsSamePrimitiveCheckTest()
    {
        super(FunctionReturnsSamePrimitiveCheck.class);
    }

    @Test
    public void testCorpusAndParameters() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "same-primitive.bsl"); //$NON-NLS-1$
        ICheckSettings settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        String originalCase = settings.getParameters().get(FunctionReturnsSamePrimitiveCheck.PARAM_CASE).getValue();
        String originalAttachable = settings.getParameters().get(FunctionReturnsSamePrimitiveCheck.PARAM_ATTACHABLE).getValue();
        assertEquals("false", originalCase); //$NON-NLS-1$
        assertEquals("true", originalAttachable); //$NON-NLS-1$
        try
        {
            assertLines(List.of(1, 8));
            settings.getParameters().get(FunctionReturnsSamePrimitiveCheck.PARAM_CASE).setValue("true"); //$NON-NLS-1$
            apply(settings);
            assertLines(List.of(1));
            settings.getParameters().get(FunctionReturnsSamePrimitiveCheck.PARAM_ATTACHABLE).setValue("false"); //$NON-NLS-1$
            apply(settings);
            assertLines(List.of(1, 15));
        }
        finally
        {
            settings.getParameters().get(FunctionReturnsSamePrimitiveCheck.PARAM_CASE).setValue(originalCase);
            settings.getParameters().get(FunctionReturnsSamePrimitiveCheck.PARAM_ATTACHABLE).setValue(originalAttachable);
            apply(settings);
        }
    }

    @Test
    public void testIncrementalReturnChange() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "same-primitive-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(1));
        updateModule(FOLDER_RESOURCE + "same-primitive-clean.bsl"); //$NON-NLS-1$
        assertLines(List.of());
        updateModule(FOLDER_RESOURCE + "same-primitive-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(1));
    }

    @Test
    public void testPrimitiveExpressionBoundaries() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "same-primitive-boundaries.bsl"); //$NON-NLS-1$
        assertLines(List.of(1, 8));
    }

    private void apply(ICheckSettings settings)
    {
        checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
        waitForDD(getProject());
    }

    private void assertLines(List<Integer> expected)
    {
        INode node = NodeModelUtils.findActualNodeFor(getModule());
        for (INode leaf : node.getLeafNodes())
        {
            assertNull("Syntax at line " + leaf.getStartLine(), leaf.getSyntaxErrorMessage()); //$NON-NLS-1$
        }
        assertEquals(expected, getModuleMarkers().stream().map(marker -> {
            Integer line = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
            return line;
        }).sorted().toList());
        for (var marker : getModuleMarkers())
        {
            Integer offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            Integer length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            assertEquals(true, node.getRootNode().getText().substring(offset, offset + length)
                .matches("[А-Яа-яA-Za-z_]+")); //$NON-NLS-1$
        }
    }
}
