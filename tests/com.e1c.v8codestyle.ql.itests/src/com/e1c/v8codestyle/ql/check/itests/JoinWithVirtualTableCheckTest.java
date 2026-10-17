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

import com.e1c.v8codestyle.ql.check.JoinWithVirtualTableCheck;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.v8codestyle.internal.ql.CorePlugin;

public class JoinWithVirtualTableCheckTest extends AbstractQueryTestBase
{
    public JoinWithVirtualTableCheckTest()
    {
        super(JoinWithVirtualTableCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "QlFullDemo"; //$NON-NLS-1$
    }

    @Test
    public void testSubcontoAndCriterionAreJoinVirtualTables() throws Exception
    {
        var schema = loadQueryAndValidate(FOLDER_RESOURCE + "join-virtual-subconto.ql"); //$NON-NLS-1$
        for (var leaf : NodeModelUtils.getNode(schema).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(List.of(2, 3), getQueryMarkers().stream().map(marker -> marker.getLineNumber()).toList());
    }

    @Test
    public void testImplicitAliasJoinChainAndDefaults() throws Exception
    {
        var settings = checkRepository.getSettings(new CheckUid(JoinWithVirtualTableCheck.CHECK_ID,
            CorePlugin.PLUGIN_ID), getProject().getWorkspaceProject());
        assertTrue(settings.isEnabled());
        assertEquals(IssueSeverity.MAJOR, settings.getSeverity());
        assertTrue(settings.getParameters().isEmpty());
        var schema = loadQueryAndValidate(FOLDER_RESOURCE + "join-virtual-extra.ql"); //$NON-NLS-1$
        for (var leaf : NodeModelUtils.getNode(schema).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(List.of(1, 5, 7), getQueryMarkers().stream().map(marker -> marker.getLineNumber()).toList());
        String text = NodeModelUtils.getNode(schema).getRootNode().getText();
        assertEquals(List.of("InformationRegister.Prices.SliceLast()", //$NON-NLS-1$
            "InformationRegister.Prices.SliceLast(, Price = &Price)", //$NON-NLS-1$
            "InformationRegister.Prices.SliceLast()"), //$NON-NLS-1$
            getQueryMarkers().stream().map(marker -> text.substring(marker.getOffset(),
                marker.getOffset() + marker.getLength())).toList());
    }

    @Test
    public void testBothJoinSidesAndNestedBoundaries() throws Exception
    {
        var schema = loadQueryAndValidate(FOLDER_RESOURCE + "join-virtual-boundaries.ql"); //$NON-NLS-1$
        for (var leaf : NodeModelUtils.getNode(schema).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(List.of(2, 3, 5, 6, 14),
            getQueryMarkers().stream().map(marker -> marker.getLineNumber()).toList());
        String text;
        try (var input = getClass().getResourceAsStream(FOLDER_RESOURCE + "join-virtual-boundaries.ql")) //$NON-NLS-1$
        {
            text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertEquals(List.of("InformationRegister.Prices.SliceLast()", //$NON-NLS-1$
            "InformationRegister.Prices.SliceLast()", "InformationRegister.Prices.SliceLast()", //$NON-NLS-1$ //$NON-NLS-2$
            "InformationRegister.Prices.SliceFirst()", "РегистрСведений.Prices.СрезПоследних(&Период)"), //$NON-NLS-1$ //$NON-NLS-2$
            getQueryMarkers().stream().map(marker -> text.substring(marker.getOffset(),
                marker.getOffset() + marker.getLength())).toList());
        assertTrue(getQueryMarkers().stream().map(marker -> marker.getOffset()).distinct().count() == 5);
    }
}
