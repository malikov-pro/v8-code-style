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

import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com.e1c.v8codestyle.ql.check.VirtualTableCallWithoutParametersCheck;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.v8codestyle.internal.ql.CorePlugin;

public class VirtualTableCallWithoutParametersCheckTest extends AbstractQueryTestBase
{
    public VirtualTableCallWithoutParametersCheckTest()
    {
        super(VirtualTableCallWithoutParametersCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "QlFullDemo"; //$NON-NLS-1$
    }

    @Test
    public void testEmptyPositionsAndExemptions() throws Exception
    {
        var schema = loadQueryAndValidate(FOLDER_RESOURCE + "virtual-table-empty-boundaries.ql"); //$NON-NLS-1$
        for (var leaf : NodeModelUtils.getNode(schema).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(List.of(1, 2, 3, 4, 7, 10, 12),
            getQueryMarkers().stream().map(marker -> marker.getLineNumber()).toList());
        String text = NodeModelUtils.getNode(schema).getRootNode().getText();
        assertEquals(List.of("InformationRegister.Prices.SliceLast", //$NON-NLS-1$
            "InformationRegister.Prices.SliceLast()", "InformationRegister.Prices.SliceLast(, )", //$NON-NLS-1$ //$NON-NLS-2$
            "InformationRegister.Prices.SliceLast(&Period, )", "FilterCriterion.QueryFilter()", //$NON-NLS-1$ //$NON-NLS-2$
            "AccountingRegister.QueryAccounting.Balance()", //$NON-NLS-1$
            "РегистрСведений.Prices.СрезПоследних( // comment only\n)"), //$NON-NLS-1$
            getQueryMarkers().stream().map(marker -> text.substring(marker.getOffset(),
                marker.getOffset() + marker.getLength())).toList());
        var settings = checkRepository.getSettings(new CheckUid(VirtualTableCallWithoutParametersCheck.CHECK_ID,
            CorePlugin.PLUGIN_ID), getProject().getWorkspaceProject());
        assertTrue(settings.isEnabled());
        assertEquals(IssueSeverity.MAJOR, settings.getSeverity());
        assertTrue(settings.getParameters().isEmpty());
    }
}
