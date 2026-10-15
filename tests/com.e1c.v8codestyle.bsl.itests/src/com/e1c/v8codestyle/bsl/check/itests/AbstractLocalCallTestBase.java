/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Ignore;

import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.ICheck;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Assertions for syntax-clean local-call regressions. */
@Ignore
public abstract class AbstractLocalCallTestBase extends AbstractSingleModuleTestBase
{
    protected AbstractLocalCallTestBase(Class<? extends ICheck> check)
    {
        super(check);
    }

    protected void assertLines(List<Integer> expected)
    {
        var node = NodeModelUtils.findActualNodeFor(getModule());
        for (var leaf : node.getLeafNodes())
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
            String text = node.getRootNode().getText().substring(offset, offset + length);
            assertEquals(text, true, text.contains("(") && text.endsWith(")")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(text, false, text.startsWith(" ") || text.contains("Тогда")); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    protected void assertIncremental(String prefix, int line) throws Exception
    {
        updateModule(FOLDER_RESOURCE + prefix + "-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(line));
        updateModule(FOLDER_RESOURCE + prefix + "-clean.bsl"); //$NON-NLS-1$
        assertLines(List.of());
        updateModule(FOLDER_RESOURCE + prefix + "-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(line));
    }

    protected void assertDefaults(IssueSeverity severity)
    {
        var settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        assertEquals(true, settings.isEnabled());
        assertEquals(severity, settings.getSeverity());
        assertEquals(0, settings.getParameters().size());
    }
}
