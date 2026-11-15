/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.xtext.EcoreUtil2;
import org.junit.Test;

import com._1c.g5.v8.dt.mcore.NamedElement;
import com._1c.g5.v8.dt.ql.model.FunctionInvocationExpression;
import com._1c.g5.v8.dt.ql.model.IsNullOperatorExpression;
import com._1c.g5.v8.dt.ql.model.MultiPartCommonExpression;
import com._1c.g5.v8.dt.ql.model.QuerySchemaExpression;
import com.e1c.v8codestyle.ql.check.FieldsFromJoinsWithoutIsNullCheck;

/**
 * One-question research: how the EDT query model represents the ЕСТЬNULL
 * function guard, a dotted field path and the {@code ЕСТЬ НЕ NULL} comparison.
 */
public class FieldsFromJoinsModelResearchTest
    extends AbstractQueryTestBase
{
    public FieldsFromJoinsModelResearchTest()
    {
        super(FieldsFromJoinsWithoutIsNullCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "QlFullDemo"; //$NON-NLS-1$
    }

    /** The function form of the guard must be a resolved function invocation. */
    @Test
    public void testIsNullFunctionRepresentation() throws Exception
    {
        var schema = loadQueryAndValidate(FOLDER_RESOURCE + "ql-outer-join-with-isnull.ql"); //$NON-NLS-1$
        var invocations = EcoreUtil2.getAllContentsOfType(schema, FunctionInvocationExpression.class);
        assertEquals(1, invocations.size());
        NamedElement function = invocations.get(0).getFunctionType();
        assertNotNull(function);
        String name = function.getName().toUpperCase(java.util.Locale.ROOT);
        System.out.println("QL ISNULL function: " + name); //$NON-NLS-1$
        assertTrue(name, "ISNULL".equals(name) || "ЕСТЬNULL".equals(name)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /** A dotted field path is a chain of multi parts, the outermost holds the whole text. */
    @Test
    public void testDottedPathRepresentation() throws Exception
    {
        var schema = loadQueryAndValidate(FOLDER_RESOURCE + "ql-outer-join-without-isnull.ql"); //$NON-NLS-1$
        var parts = EcoreUtil2.getAllContentsOfType(schema, MultiPartCommonExpression.class);
        assertTrue(!parts.isEmpty());
        for (var part : parts)
        {
            System.out.println("QL multipart: content=" + part.getContent() + ", full=" + part.getFullContent() //$NON-NLS-1$ //$NON-NLS-2$
                + ", container=" + part.eContainer().eClass().getName()); //$NON-NLS-1$
        }
        var outermost = parts.stream().filter(part -> "T.Description".equals(part.getFullContent())).toList(); //$NON-NLS-1$
        assertEquals(1, outermost.size());
        // A plain selected field is the expression root itself, not a nested node:
        // getAllContentsOfType on the expression does not return it.
        assertTrue(outermost.get(0).eContainer() instanceof QuerySchemaExpression);
    }

    /** {@code ЕСТЬ НЕ NULL} must be a negated IS NULL operator inside the WHERE filter. */
    @Test
    public void testNotNullComparisonRepresentation() throws Exception
    {
        var schema = loadQueryAndValidate(FOLDER_RESOURCE + "ql-outer-join-not-null-filter.ql"); //$NON-NLS-1$
        var comparisons = EcoreUtil2.getAllContentsOfType(schema, IsNullOperatorExpression.class);
        assertEquals(1, comparisons.size());
        assertTrue(comparisons.get(0).isIsNot());
        var filters = EcoreUtil2.getContainerOfType(comparisons.get(0), QuerySchemaExpression.class);
        assertNotNull(filters);
    }
}
