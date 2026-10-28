/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.ql.model.QuerySchema;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.v8codestyle.internal.ql.CorePlugin;

import com.e1c.v8codestyle.ql.check.UnionAllCheck;

public class UnionAllCheckTest extends AbstractQueryTestBase
{
    public UnionAllCheckTest()
    {
        super(UnionAllCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "QlFullDemo"; //$NON-NLS-1$
    }

    @Test
    public void testUnionKeyword() throws Exception
    {
        var schema = loadQueryAndValidate(FOLDER_RESOURCE + "union-all-boundaries.ql"); //$NON-NLS-1$
        assertSyntaxClean(schema);
        assertEquals(1, getQueryMarkers().size());
        assertEquals(4, getQueryMarkers().get(0).getLineNumber());
        assertEquals(5, getQueryMarkers().get(0).getLength());
        assertEquals(48, getQueryMarkers().get(0).getOffset());
    }

    @Test
    public void testCleanAndNestedPackageCycles() throws Exception
    {
        var settings = checkRepository.getSettings(new CheckUid(UnionAllCheck.CHECK_ID, CorePlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        assertTrue(settings.isEnabled());
        assertEquals(IssueSeverity.MINOR, settings.getSeverity());
        assertTrue(settings.getParameters().isEmpty());
        for (int cycle = 0; cycle < 2; cycle++)
        {
            getQueryMarkers().clear();
            assertSyntaxClean(loadQueryAndValidate(FOLDER_RESOURCE + "union-all-clean.ql")); //$NON-NLS-1$
            assertEquals(0, getQueryMarkers().size());
            var schema = loadQueryAndValidate(FOLDER_RESOURCE + "union-all-nested.ql"); //$NON-NLS-1$
            assertSyntaxClean(schema);
            assertEquals(2, getQueryMarkers().size());
            String text;
            try (var input = getClass().getResourceAsStream(FOLDER_RESOURCE + "union-all-nested.ql")) //$NON-NLS-1$
            {
                text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            assertEquals(3, getQueryMarkers().get(0).getLineNumber());
            assertEquals(9, getQueryMarkers().get(1).getLineNumber());
            assertEquals("UNION", text.substring(getQueryMarkers().get(0).getOffset(), //$NON-NLS-1$
                getQueryMarkers().get(0).getOffset() + getQueryMarkers().get(0).getLength()));
            assertEquals("ОбЪеДиНиТь", text.substring(getQueryMarkers().get(1).getOffset(), //$NON-NLS-1$
                getQueryMarkers().get(1).getOffset() + getQueryMarkers().get(1).getLength()));
        }
    }

    private void assertSyntaxClean(QuerySchema schema)
    {
        var node = NodeModelUtils.getNode(schema);
        assertTrue(node != null);
        for (var leaf : node.getLeafNodes())
        {
            assertTrue("Query fixture has syntax errors: " + leaf.getText(), leaf.getSyntaxErrorMessage() == null); //$NON-NLS-1$
        }
    }
}
