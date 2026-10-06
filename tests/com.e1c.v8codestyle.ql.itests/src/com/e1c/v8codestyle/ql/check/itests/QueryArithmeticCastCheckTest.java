/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: 1C-Soft LLC
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the APK check АПК_00212
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import com.e1c.v8codestyle.ql.check.QueryArithmeticCastCheck;
import com.e1c.v8codestyle.ql.check.itests.TestingQlResultAcceptor.QueryMarker;

/**
 * Test {@link QueryArithmeticCastCheck} class that checks division and AVERAGE
 * (СРЕДНЕЕ) results wrapped in a CAST(... AS Number) expression — port of the
 * APK check АПК_00212.
 *
 * @author malikov-pro
 */
public class QueryArithmeticCastCheckTest
    extends AbstractQueryTestBase
{

    private static final String PROJECT_NAME = "QlFullDemo";

    private static final String FOLDER = FOLDER_RESOURCE + "apk-00212-";

    public QueryArithmeticCastCheckTest()
    {
        super(QueryArithmeticCastCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * A division without a number cast is reported once.
     */
    @Test
    public void testDivisionWithoutCast() throws Exception
    {
        loadQueryAndValidate(FOLDER + "division-no-cast.ql");
        List<QueryMarker> markers = getQueryMarkers();
        assertEquals(1, markers.size());

        QueryMarker marker = markers.get(0);
        assertNotNull(marker.getTarget());
        assertEquals(3, marker.getLineNumber());
    }

    /**
     * The AVERAGE function without a number cast is reported once.
     */
    @Test
    public void testAverageWithoutCast() throws Exception
    {
        loadQueryAndValidate(FOLDER + "average-no-cast.ql");
        List<QueryMarker> markers = getQueryMarkers();
        assertEquals(1, markers.size());

        QueryMarker marker = markers.get(0);
        assertNotNull(marker.getTarget());
        assertEquals(2, marker.getLineNumber());
    }

    /**
     * A division and an AVERAGE wrapped in a number cast produce no issues;
     * multiplication alone is not checked.
     */
    @Test
    public void testCastCompliant() throws Exception
    {
        loadQueryAndValidate(FOLDER + "cast-compliant.ql");
        List<QueryMarker> markers = getQueryMarkers();
        assertEquals(0, markers.size());
    }

}
