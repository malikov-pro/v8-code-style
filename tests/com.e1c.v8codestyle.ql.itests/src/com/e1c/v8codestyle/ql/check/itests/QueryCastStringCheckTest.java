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
 *     malikov-pro - port of the APK check АПК_00142
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import com.e1c.v8codestyle.ql.check.QueryCastStringCheck;
import com.e1c.v8codestyle.ql.check.itests.TestingQlResultAcceptor.QueryMarker;

/**
 * Test {@link QueryCastStringCheck} class that checks CAST(... AS STRING(N))
 * (ВЫРАЗИТЬ(... КАК СТРОКА(N))) usage in queries — port of the APK check
 * АПК_00142.
 *
 * @author malikov-pro
 */
public class QueryCastStringCheckTest
    extends AbstractQueryTestBase
{

    private static final String PROJECT_NAME = "QlFullDemo";

    private static final String FOLDER = FOLDER_RESOURCE + "apk-00142-";

    public QueryCastStringCheckTest()
    {
        super(QueryCastStringCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * Each CAST(... AS STRING(N)) / ВЫРАЗИТЬ(... КАК СТРОКА(N)) with a length
     * is reported once, in both keyword forms.
     */
    @Test
    public void testCastToStringWithLength() throws Exception
    {
        loadQueryAndValidate(FOLDER + "cast-string.ql");
        List<QueryMarker> markers = getQueryMarkers();
        assertEquals(2, markers.size());

        QueryMarker marker = markers.get(0);
        assertNotNull(marker.getTarget());
        assertEquals(2, marker.getLineNumber());

        marker = markers.get(1);
        assertNotNull(marker.getTarget());
        assertEquals(3, marker.getLineNumber());
    }

    /**
     * A query without string casts produces no issues.
     */
    @Test
    public void testNoCastToString() throws Exception
    {
        loadQueryAndValidate(FOLDER + "no-cast-string.ql");
        List<QueryMarker> markers = getQueryMarkers();
        assertEquals(0, markers.size());
    }

}
