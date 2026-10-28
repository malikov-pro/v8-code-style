/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import java.util.Map;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Ignore;

import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.ICheck;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Shared metric regression assertions, not an independent product check. */
@Ignore
public abstract class AbstractLsMetricTestBase extends AbstractSingleModuleTestBase
{
    protected AbstractLsMetricTestBase(Class<? extends ICheck> clazz)
    {
        super(clazz);
    }

    protected void assertMetricValues(Map<Integer, Integer> expected)
    {
        INode node = NodeModelUtils.findActualNodeFor(getModule());
        for (INode leaf : node.getLeafNodes())
        {
            assertNull("Syntax at line " + leaf.getStartLine(), leaf.getSyntaxErrorMessage()); //$NON-NLS-1$
        }
        Map<Integer, Integer> actual = new TreeMap<>();
        for (var marker : getModuleMarkers())
        {
            var matcher = Pattern.compile("(\\d+) \\(").matcher(marker.getMessage()); //$NON-NLS-1$
            assertEquals(marker.getMessage(), true, matcher.find());
            Integer line = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
            assertNull("Duplicate metric marker at line " + line, actual.put(line, Integer.valueOf(matcher.group(1)))); //$NON-NLS-1$
            Integer offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            Integer length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            assertEquals(true, node.getRootNode().getText().substring(offset, offset + length)
                .matches("[А-Яа-яA-Za-z_][А-Яа-яA-Za-z_0-9]*")); //$NON-NLS-1$
        }
        assertEquals(expected, actual);
    }

    protected ICheckSettings settings()
    {
        return checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
    }

    protected void setParameter(ICheckSettings settings, String name, String value)
    {
        assertNotNull("Missing metric parameter " + name, settings.getParameters().get(name)); //$NON-NLS-1$
        settings.getParameters().get(name).setValue(value);
        checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
        waitForDD(getProject());
    }
}
