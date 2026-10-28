/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.nio.charset.StandardCharsets;

import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com.e1c.v8codestyle.ql.check.SelectTopWithoutOrderByCheck;
import com.e1c.v8codestyle.internal.ql.CorePlugin;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;

public class SelectTopWithoutOrderByCheckTest extends AbstractQueryTestBase
{
    public SelectTopWithoutOrderByCheckTest()
    {
        super(SelectTopWithoutOrderByCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "QlFullDemo"; //$NON-NLS-1$
    }

    @Test
    public void testDefaultTopBoundaries() throws Exception
    {
        var schema = loadQueryAndValidate(FOLDER_RESOURCE + "select-top-boundaries.ql"); //$NON-NLS-1$
        for (var leaf : NodeModelUtils.getNode(schema).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(List.of(1, 6, 8), getQueryMarkers().stream().map(marker -> marker.getLineNumber()).toList());
        assertRanges("select-top-boundaries.ql"); //$NON-NLS-1$
    }

    @Test
    public void testParameterFalseAndRestore() throws Exception
    {
        var settings = checkRepository.getSettings(new CheckUid(SelectTopWithoutOrderByCheck.CHECK_ID,
            CorePlugin.PLUGIN_ID), getProject().getWorkspaceProject());
        assertTrue(settings.isEnabled());
        assertEquals(IssueSeverity.MAJOR, settings.getSeverity());
        assertEquals(1, settings.getParameters().size());
        var parameter = settings.getParameters().get(SelectTopWithoutOrderByCheck.SKIP_SELECT_TOP_ONE);
        String original = parameter.getValue();
        assertEquals("true", original); //$NON-NLS-1$
        try
        {
            parameter.setValue("false"); //$NON-NLS-1$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
            loadQueryAndValidate(FOLDER_RESOURCE + "select-top-boundaries.ql"); //$NON-NLS-1$
            assertEquals(List.of(1, 2, 3, 6, 8),
                getQueryMarkers().stream().map(marker -> marker.getLineNumber()).toList());
        }
        finally
        {
            parameter.setValue(original);
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
        }
        getQueryMarkers().clear();
        loadQueryAndValidate(FOLDER_RESOURCE + "select-top-boundaries.ql"); //$NON-NLS-1$
        assertEquals(List.of(1, 6, 8), getQueryMarkers().stream().map(marker -> marker.getLineNumber()).toList());
    }

    @Test
    public void testNestedOwnersPackagesWhereAndLiteralCount() throws Exception
    {
        var schema = loadQueryAndValidate(FOLDER_RESOURCE + "select-top-nested.ql"); //$NON-NLS-1$
        for (var leaf : NodeModelUtils.getNode(schema).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(List.of(2, 4, 6, 7, 11),
            getQueryMarkers().stream().map(marker -> marker.getLineNumber()).toList());
        assertRanges("select-top-nested.ql"); //$NON-NLS-1$
    }

    private void assertRanges(String resource) throws Exception
    {
        String text;
        try (var input = getClass().getResourceAsStream(FOLDER_RESOURCE + resource))
        {
            text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (var marker : getQueryMarkers())
        {
            String range = text.substring(marker.getOffset(), marker.getOffset() + marker.getLength());
            assertTrue(range, range.matches("(?iu)(TOP|ПЕРВЫЕ) [0-9]+")); //$NON-NLS-1$
        }
    }
}
