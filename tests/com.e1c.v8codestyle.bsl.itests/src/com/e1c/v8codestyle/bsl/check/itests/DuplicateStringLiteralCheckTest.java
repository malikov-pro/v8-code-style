/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com.e1c.v8codestyle.bsl.check.DuplicateStringLiteralCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class DuplicateStringLiteralCheckTest extends AbstractSingleModuleTestBase
{
    public DuplicateStringLiteralCheckTest()
    {
        super(DuplicateStringLiteralCheck.class);
    }

    @Test
    public void testScopeAndParameters() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "duplicate-string-literal.bsl"); //$NON-NLS-1$
        ICheckSettings settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        Map<String, String> original = new HashMap<>();
        settings.getParameters().forEach((key, value) -> original.put(key, value.getValue()));
        try
        {
            assertLines(List.of(2, 21));
            set(settings, DuplicateStringLiteralCheck.PARAM_CASE, "true"); //$NON-NLS-1$
            assertLines(List.of(21));
            set(settings, DuplicateStringLiteralCheck.PARAM_CASE, "false"); //$NON-NLS-1$
            set(settings, DuplicateStringLiteralCheck.PARAM_FILE, "true"); //$NON-NLS-1$
            assertLines(List.of(2, 10, 21));
            set(settings, DuplicateStringLiteralCheck.PARAM_COPIES, "3"); //$NON-NLS-1$
            assertLines(List.of());
            set(settings, DuplicateStringLiteralCheck.PARAM_COPIES, "0"); //$NON-NLS-1$
            assertLines(List.of(2, 10, 21));
            set(settings, DuplicateStringLiteralCheck.PARAM_LENGTH, "12"); //$NON-NLS-1$
            assertLines(List.of(10, 21));
            set(settings, DuplicateStringLiteralCheck.PARAM_LENGTH, "1"); //$NON-NLS-1$
            assertLines(List.of(2, 10, 21));
        }
        finally
        {
            original.forEach((key, value) -> settings.getParameters().get(key).setValue(value));
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
        }
    }

    @Test
    public void testIncrementalLiteralReplacement() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "duplicate-literal-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(2));
        updateModule(FOLDER_RESOURCE + "duplicate-literal-clean.bsl"); //$NON-NLS-1$
        assertLines(List.of());
        updateModule(FOLDER_RESOURCE + "duplicate-literal-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(2));
    }

    @Test
    public void testQuotedLengthAndMethodDefaults() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "duplicate-literal-boundaries.bsl"); //$NON-NLS-1$
        assertLines(List.of(5, 10, 16));
    }

    private void set(ICheckSettings settings, String key, String value)
    {
        settings.getParameters().get(key).setValue(value);
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
            assertEquals(true, text.startsWith("\"") && text.endsWith("\"")); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}
