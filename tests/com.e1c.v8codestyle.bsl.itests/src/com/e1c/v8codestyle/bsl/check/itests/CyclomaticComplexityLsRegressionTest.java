/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import com.e1c.v8codestyle.bsl.check.CyclomaticComplexityCheck;

public class CyclomaticComplexityLsRegressionTest extends AbstractLsMetricTestBase
{
    public CyclomaticComplexityLsRegressionTest()
    {
        super(CyclomaticComplexityCheck.class);
    }

    @Test
    public void testExactMethodValuesAndModuleToggle() throws Exception
    {
        var settings = settings();
        String original = settings.getParameters().get("complexityThreshold").getValue(); //$NON-NLS-1$
        assertEquals("20", original); //$NON-NLS-1$
        try
        {
            setParameter(settings, "complexityThreshold", "0"); //$NON-NLS-1$ //$NON-NLS-2$
            updateModule(FOLDER_RESOURCE + "metrics-ls-regressions.bsl"); //$NON-NLS-1$
            assertMetricValues(Map.of(1, 4, 7, 3, 14, 2, 22, 2, 26, 2, 30, 5, 41, 2));
            setParameter(settings, "checkModuleBody", "false"); //$NON-NLS-1$ //$NON-NLS-2$
            assertMetricValues(Map.of(1, 4, 7, 3, 14, 2, 22, 2, 26, 2, 30, 5));
            setParameter(settings, "checkModuleBody", "true"); //$NON-NLS-1$ //$NON-NLS-2$
            assertMetricValues(Map.of(1, 4, 7, 3, 14, 2, 22, 2, 26, 2, 30, 5, 41, 2));
        }
        finally
        {
            setParameter(settings, "complexityThreshold", original); //$NON-NLS-1$
            if (settings.getParameters().containsKey("checkModuleBody")) //$NON-NLS-1$
            {
                setParameter(settings, "checkModuleBody", "true"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    @Test
    public void testPinnedLsCorpus() throws Exception
    {
        var settings = settings();
        String original = settings.getParameters().get("complexityThreshold").getValue(); //$NON-NLS-1$
        try
        {
            setParameter(settings, "complexityThreshold", "0"); //$NON-NLS-1$ //$NON-NLS-2$
            updateModule(FOLDER_RESOURCE + "metrics-cyclomatic-corpus.bsl"); //$NON-NLS-1$
            assertMetricValues(Map.of(1, 12, 30, 11, 52, 1, 56, 15));
        }
        finally
        {
            setParameter(settings, "complexityThreshold", original); //$NON-NLS-1$
        }
    }

    @Test
    public void testExpressionBoundaries() throws Exception
    {
        var settings = settings();
        String original = settings.getParameters().get("complexityThreshold").getValue(); //$NON-NLS-1$
        try
        {
            setParameter(settings, "complexityThreshold", "0"); //$NON-NLS-1$ //$NON-NLS-2$
            updateModule(FOLDER_RESOURCE + "metrics-expression-edges.bsl"); //$NON-NLS-1$
            // Parenthesized logical subexpressions are counted once in EDT AST.
            // Pinned LS's listener can recount them; this remains an explicit difference.
            assertMetricValues(Map.of(1, 7, 7, 4, 11, 2, 16, 6));
        }
        finally
        {
            setParameter(settings, "complexityThreshold", original); //$NON-NLS-1$
        }
    }
}
