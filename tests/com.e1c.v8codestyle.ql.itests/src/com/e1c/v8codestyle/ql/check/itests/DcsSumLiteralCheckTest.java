/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the APK check АПК_01193
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.dcs.model.schema.DcsFactory;
import com._1c.g5.v8.dt.dcs.model.schema.DcsPackage;
import com._1c.g5.v8.dt.ql.model.QuerySchema;
import com.e1c.g5.v8.dt.ql.check.QlBasicDelegateCheck.QueryOwner;
import com.e1c.v8codestyle.ql.check.DcsSumLiteralCheck;

/** DCS-only literal matrix and exact query ranges. */
public class DcsSumLiteralCheckTest
    extends AbstractQueryTestBase
{
    private boolean dcsOwner = true;

    public DcsSumLiteralCheckTest()
    {
        super(DcsSumLiteralCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "QlFullDemo"; //$NON-NLS-1$
    }

    @Override
    protected QueryOwner getQueryOwner(QuerySchema query)
    {
        return dcsOwner ? new QueryOwner(DcsFactory.eINSTANCE.createDataCompositionSchemaDataSetQuery(),
            DcsPackage.Literals.DATA_COMPOSITION_SCHEMA_DATA_SET_QUERY__QUERY) : super.getQueryOwner(query);
    }

    /** @throws Exception if query validation fails */
    @Test
    public void testLiteralSpellingsAndPositions() throws Exception
    {
        var query = loadQueryAndValidate(FOLDER_RESOURCE + "apk-01193-dcs-sum-literal.ql"); //$NON-NLS-1$
        for (var leaf : NodeModelUtils.findActualNodeFor(query).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        var markers = getQueryMarkers();
        assertEquals(List.of(2, 3, 4, 5, 6, 7), markers.stream().map(marker -> marker.getLineNumber()).sorted().toList());
        String text = NodeModelUtils.findActualNodeFor(query).getRootNode().getText();
        for (var marker : markers)
        {
            String range = text.substring(marker.getOffset(), marker.getOffset() + marker.getLength());
            assertTrue(range, range.startsWith("SUM(") || range.startsWith("СУММА(")); //$NON-NLS-1$ //$NON-NLS-2$
            assertTrue(range, range.endsWith(")")); //$NON-NLS-1$
        }
    }

    /** @throws Exception if query validation fails */
    @Test
    public void testCleanQuery() throws Exception
    {
        loadQueryAndValidate(FOLDER_RESOURCE + "apk-01193-dcs-sum-literal-clean.ql"); //$NON-NLS-1$
        assertEquals(0, getQueryMarkers().size());
    }

    /** @throws Exception if query validation fails */
    @Test
    public void testNonDcsOwnerIsSkipped() throws Exception
    {
        dcsOwner = false;
        loadQueryAndValidate(FOLDER_RESOURCE + "apk-01193-dcs-sum-literal.ql"); //$NON-NLS-1$
        assertEquals(0, getQueryMarkers().size());
    }
}
