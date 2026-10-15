/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import com.e1c.v8codestyle.bsl.check.CognitiveComplexityCheck;

public class CognitiveComplexityLsRegressionTest extends AbstractLsMetricTestBase
{
    public CognitiveComplexityLsRegressionTest()
    {
        super(CognitiveComplexityCheck.class);
    }

    @Test
    public void testExactMethodValues() throws Exception
    {
        var settings = settings();
        String original = settings.getParameters().get("complexityThreshold").getValue(); //$NON-NLS-1$
        assertEquals("15", original); //$NON-NLS-1$
        try
        {
            setParameter(settings, "complexityThreshold", "0"); //$NON-NLS-1$ //$NON-NLS-2$
            updateModule(FOLDER_RESOURCE + "metrics-ls-regressions.bsl"); //$NON-NLS-1$
            assertMetricValues(Map.of(1, 2, 7, 2, 14, 1, 22, 1, 26, 1, 30, 5, 41, 2));
        }
        finally
        {
            setParameter(settings, "complexityThreshold", original); //$NON-NLS-1$
        }
    }

    @Test
    public void testBooleanSeriesIncremental() throws Exception
    {
        var settings = settings();
        String original = settings.getParameters().get("complexityThreshold").getValue(); //$NON-NLS-1$
        try
        {
            setParameter(settings, "complexityThreshold", "2"); //$NON-NLS-1$ //$NON-NLS-2$
            updateModule(FOLDER_RESOURCE + "metrics-logic-bad.bsl"); //$NON-NLS-1$
            assertMetricValues(Map.of(1, 3));
            updateModule(FOLDER_RESOURCE + "metrics-logic-clean.bsl"); //$NON-NLS-1$
            assertMetricValues(Map.of());
            updateModule(FOLDER_RESOURCE + "metrics-logic-bad.bsl"); //$NON-NLS-1$
            assertMetricValues(Map.of(1, 3));
        }
        finally
        {
            setParameter(settings, "complexityThreshold", original); //$NON-NLS-1$
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
            updateModule(FOLDER_RESOURCE + "metrics-cognitive-corpus.bsl"); //$NON-NLS-1$
            // Adapted from pinned LS computer corpus, same structure/expected metrics.
            assertMetricValues(Map.of(1, 19, 27, 33, 66, 20, 107, 15));
        }
        finally
        {
            setParameter(settings, "complexityThreshold", original); //$NON-NLS-1$
        }
    }

    @Test
    public void testModuleBodyIncremental() throws Exception
    {
        var settings = settings();
        String original = settings.getParameters().get("complexityThreshold").getValue(); //$NON-NLS-1$
        assertEquals("true", settings.getParameters().get("checkModuleBody").getValue()); //$NON-NLS-1$ //$NON-NLS-2$
        try
        {
            setParameter(settings, "complexityThreshold", "1"); //$NON-NLS-1$ //$NON-NLS-2$
            updateModule(FOLDER_RESOURCE + "metrics-body-bad.bsl"); //$NON-NLS-1$
            assertMetricValues(Map.of(4, 2));
            setParameter(settings, "checkModuleBody", "false"); //$NON-NLS-1$ //$NON-NLS-2$
            assertMetricValues(Map.of());
            setParameter(settings, "checkModuleBody", "true"); //$NON-NLS-1$ //$NON-NLS-2$
            assertMetricValues(Map.of(4, 2));
            updateModule(FOLDER_RESOURCE + "metrics-body-clean.bsl"); //$NON-NLS-1$
            assertMetricValues(Map.of());
            updateModule(FOLDER_RESOURCE + "metrics-body-bad.bsl"); //$NON-NLS-1$
            assertMetricValues(Map.of(4, 2));
        }
        finally
        {
            setParameter(settings, "complexityThreshold", original); //$NON-NLS-1$
            setParameter(settings, "checkModuleBody", "true"); //$NON-NLS-1$ //$NON-NLS-2$
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
            assertMetricValues(Map.of(1, 5, 7, 2, 11, 1, 16, 4));
        }
        finally
        {
            setParameter(settings, "complexityThreshold", original); //$NON-NLS-1$
        }
    }

    @Test
    public void testPreprocessorAndLabeledStructure() throws Exception
    {
        var settings = settings();
        String original = settings.getParameters().get("complexityThreshold").getValue(); //$NON-NLS-1$
        try
        {
            setParameter(settings, "complexityThreshold", "0"); //$NON-NLS-1$ //$NON-NLS-2$
            updateModule(FOLDER_RESOURCE + "metrics-preprocessor-corpus.bsl"); //$NON-NLS-1$
            assertMetricValues(Map.of(1, 4, 15, 3, 24, 1));
        }
        finally
        {
            setParameter(settings, "complexityThreshold", original); //$NON-NLS-1$
        }
    }

    @Test
    public void testNestedCallArgumentsAndIndexExpression() throws Exception
    {
        var settings = settings();
        String original = settings.getParameters().get("complexityThreshold").getValue(); //$NON-NLS-1$
        try
        {
            setParameter(settings, "complexityThreshold", "0"); //$NON-NLS-1$ //$NON-NLS-2$
            updateModule(FOLDER_RESOURCE + "metrics-nested-call-corpus.bsl"); //$NON-NLS-1$
            assertMetricValues(Map.of(1, 2, 6, 2, 10, 2, 14, 2));
        }
        finally
        {
            setParameter(settings, "complexityThreshold", original); //$NON-NLS-1$
        }
    }
}
