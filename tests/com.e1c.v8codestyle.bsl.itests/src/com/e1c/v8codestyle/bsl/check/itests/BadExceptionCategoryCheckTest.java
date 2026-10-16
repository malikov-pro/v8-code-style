/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.v8codestyle.bsl.check.BadExceptionCategoryCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class BadExceptionCategoryCheckTest extends AbstractSingleModuleTestBase
{
    public BadExceptionCategoryCheckTest()
    {
        super(BadExceptionCategoryCheck.class);
    }

    @Test
    public void testCaseMultilineModuleBodyAndArgumentBoundaries() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "bad-category-boundaries.bsl"); //$NON-NLS-1$
        assertLines(List.of(2, 6, 9, 24));
    }

    @Test
    public void testBoundCategoriesAndExclusions() throws Exception
    {
        var settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        assertTrue(settings.isEnabled());
        assertEquals(IssueSeverity.TRIVIAL, settings.getSeverity());
        assertEquals(0, settings.getParameters().size());
        updateModule(FOLDER_RESOURCE + "bad-category-corpus.bsl"); //$NON-NLS-1$
        assertLines(List.of(2, 5, 8, 11, 14, 17, 20, 23, 26));
    }

    @Test
    public void testSourceIncremental() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "bad-category-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(2));
        updateModule(FOLDER_RESOURCE + "bad-category-clean.bsl"); //$NON-NLS-1$
        assertLines(List.of());
        updateModule(FOLDER_RESOURCE + "bad-category-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(2));
    }

    private void assertLines(List<Integer> expected)
    {
        var node = NodeModelUtils.findActualNodeFor(getModule());
        for (var leaf : node.getLeafNodes())
        {
            assertNull("Syntax at " + leaf.getStartLine(), leaf.getSyntaxErrorMessage()); //$NON-NLS-1$
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
            assertTrue(text, text.regionMatches(true, 0, "КатегорияОшибки.", 0, "КатегорияОшибки.".length()) //$NON-NLS-1$ //$NON-NLS-2$
                || text.regionMatches(true, 0, "ErrorCategory.", 0, "ErrorCategory.".length())); //$NON-NLS-1$ //$NON-NLS-2$
            assertTrue(text, !text.contains("(") && !text.contains(")") && !text.contains(";")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            Integer line = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
            var access = org.eclipse.xtext.EcoreUtil2.getAllContentsOfType(getModule(),
                com._1c.g5.v8.dt.bsl.model.RaiseStatement.class).stream()
                .filter(statement -> statement.getExpressions().size() > 1)
                .filter(statement -> NodeModelUtils.findActualNodeFor(statement.getExpressions().get(1))
                    .getStartLine() == line).findFirst().orElseThrow().getExpressions().get(1);
            var actual = NodeModelUtils.findActualNodeFor(access);
            var tokens = java.util.stream.StreamSupport.stream(actual.getLeafNodes().spliterator(), false)
                .filter(leaf -> !leaf.isHidden()).toList();
            assertEquals(Integer.valueOf(tokens.get(0).getOffset()), offset);
            assertEquals(Integer.valueOf(tokens.get(tokens.size() - 1).getEndOffset() - offset), length);
        }
    }
}
