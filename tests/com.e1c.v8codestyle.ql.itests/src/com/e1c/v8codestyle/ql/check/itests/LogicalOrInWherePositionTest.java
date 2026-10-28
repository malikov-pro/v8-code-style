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
 *     malikov-pro - initial API and implementation
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check.itests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.e1c.v8codestyle.ql.check.LogicalOrInWhereCheck;

/** Regression test for the location of an OR issue in a WHERE condition. */
public class LogicalOrInWherePositionTest
    extends AbstractQueryTestBase
{
    public LogicalOrInWherePositionTest()
    {
        super(LogicalOrInWhereCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "QlFullDemo"; //$NON-NLS-1$
    }

    /** @throws Exception if query loading fails */
    @Test
    public void testOrKeywordLocation() throws Exception
    {
        loadQueryAndValidate(FOLDER_RESOURCE + "ql-or-position.ql"); //$NON-NLS-1$
        assertEquals(1, getQueryMarkers().size());
        assertEquals(8, getQueryMarkers().get(0).getLineNumber());
        assertEquals(3, getQueryMarkers().get(0).getLength());
    }
}
