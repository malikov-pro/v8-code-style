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
 *     malikov-pro - port of the APK check АПК_00429
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.junit.Before;
import org.junit.Test;

import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.dcs.util.DcsUtil;
import com._1c.g5.v8.dt.ql.model.QuerySchema;
import com._1c.g5.v8.dt.ql.model.QuerySchemaOperator;
import com._1c.g5.v8.dt.ql.model.QuerySchemaSelectQuery;
import com.e1c.g5.v8.dt.ql.check.QlBasicDelegateCheck;
import com.e1c.g5.v8.dt.ql.check.QlBasicDelegateCheck.QueryOwner;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.ql.check.UsingAllowedKeywordCheck;

/**
 * Test {@link UsingAllowedKeywordCheck} class that checks usage of the ALLOWED
 * (РАЗРЕШЕННЫЕ) keyword in queries — port of the APK check АПК_00429.
 *
 * @author malikov-pro
 */
public class UsingAllowedKeywordCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String PROJECT_NAME = "QlEmptyProject"; //$NON-NLS-1$

    private static final String FOLDER = "/resources/"; //$NON-NLS-1$

    private TestingQlResultAcceptor qlResultAcceptor;

    private UsingAllowedKeywordCheck check;

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * Setups the check instance and the QL result acceptor.
     */
    @Before
    public void setupCheck() throws Exception
    {
        qlResultAcceptor = new TestingQlResultAcceptor();
        QlBasicDelegateCheck.setResultAcceptor((o, f) -> qlResultAcceptor);
        check = new UsingAllowedKeywordCheck();
    }

    /**
     * A query with the ALLOWED keyword is reported once per operator.
     */
    @Test
    public void testQueryWithAllowedKeyword() throws Exception
    {
        QuerySchemaOperator operator = loadOperator("apk-00429-query-allowed.ql"); //$NON-NLS-1$
        assertTrue(operator.isSelectAllowed());

        check.check(operator, new TestingCheckResultAcceptor(), new TestingCheckParameters(Map.of()), null,
            new NullProgressMonitor());

        assertEquals(1, qlResultAcceptor.getMarkers().size());
    }

    /**
     * A query without the ALLOWED keyword produces no issues.
     */
    @Test
    public void testQueryWithoutAllowedKeyword() throws Exception
    {
        QuerySchemaOperator operator = loadOperator("apk-00429-query-not-allowed.ql"); //$NON-NLS-1$
        assertTrue(!operator.isSelectAllowed());

        check.check(operator, new TestingCheckResultAcceptor(), new TestingCheckParameters(Map.of()), null,
            new NullProgressMonitor());

        assertTrue(qlResultAcceptor.getMarkers().isEmpty());
    }

    private QuerySchemaOperator loadOperator(String resource) throws Exception
    {
        IDtProject project = dtProjectManager.getDtProject(PROJECT_NAME);

        String queryText = new String(getClass().getResourceAsStream(FOLDER + resource).readAllBytes(),
            StandardCharsets.UTF_8);

        QuerySchema querySchema = DcsUtil.getQuerySchema(queryText, project);
        assertNotNull(querySchema);
        assertEquals(1, querySchema.getQueries().size());

        QlBasicDelegateCheck.setOwner(new QueryOwner(querySchema, null));
        EObject query = querySchema.getQueries().get(0);
        assertTrue(query instanceof QuerySchemaSelectQuery);
        QuerySchemaSelectQuery selectQuery = (QuerySchemaSelectQuery)query;
        assertEquals(1, selectQuery.getOperators().size());
        return selectQuery.getOperators().get(0);
    }
}
