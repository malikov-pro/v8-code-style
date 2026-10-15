/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.metadata.mdclass.CompatibilityMode;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com.e1c.v8codestyle.bsl.check.UnsafeSafeModeMethodCallCheck;

public class UnsafeSafeModeMethodCallCheckTest
    extends AbstractLocalCallTestBase
{
    public UnsafeSafeModeMethodCallCheckTest()
    {
        super(UnsafeSafeModeMethodCallCheck.class);
    }

    @Test
    public void testBooleanAndExplicitComparisonContexts() throws Exception
    {
        assertDefaults(com.e1c.g5.v8.dt.check.settings.IssueSeverity.CRITICAL);
        updateModule(FOLDER_RESOURCE + "unsafe-safe-mode-corpus.bsl"); //$NON-NLS-1$
        assertLines(List.of(2, 3, 5, 6, 7, 20));
    }

    @Test
    public void testIncrementalExplicitComparison() throws Exception
    {
        assertIncremental("unsafe-safe-mode", 2); //$NON-NLS-1$
    }

    @Test
    public void testParenthesesUnaryAndArgumentBoundaries() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "local-call-boundaries.bsl"); //$NON-NLS-1$
        assertLines(List.of(8, 13, 14));
    }

    @Test
    public void testCompatibilityBoundary() throws Exception
    {
        CompatibilityMode original = changeCompatibility(CompatibilityMode.VERSION8_216);
        try
        {
            updateModule(FOLDER_RESOURCE + "unsafe-safe-mode-bad.bsl"); //$NON-NLS-1$
            assertLines(List.of());
            changeCompatibility(CompatibilityMode.VERSION8_31);
            updateModule(FOLDER_RESOURCE + "unsafe-safe-mode-bad.bsl"); //$NON-NLS-1$
            assertLines(List.of(2));
            changeCompatibility(CompatibilityMode.VERSION8_216);
            updateModule(FOLDER_RESOURCE + "unsafe-safe-mode-bad.bsl"); //$NON-NLS-1$
            assertLines(List.of());
        }
        finally
        {
            changeCompatibility(original);
        }
    }

    private CompatibilityMode changeCompatibility(CompatibilityMode mode)
    {
        CompatibilityMode old = bmModelManager.getModel(getProject()).execute(
            new AbstractBmTask<CompatibilityMode>("Set test compatibility") //$NON-NLS-1$
            {
                @Override
                public CompatibilityMode execute(IBmTransaction transaction, IProgressMonitor monitor)
                {
                    Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration"); //$NON-NLS-1$
                    CompatibilityMode previous = configuration.getCompatibilityMode();
                    configuration.setCompatibilityMode(mode);
                    return previous;
                }
            });
        waitForDD(getProject());
        return old;
    }

    @Test
    public void testCompatibilityOnlyInvalidatesMarkers() throws Exception
    {
        CompatibilityMode original = changeCompatibility(CompatibilityMode.VERSION8_31);
        try
        {
            updateModule(FOLDER_RESOURCE + "unsafe-safe-mode-bad.bsl"); //$NON-NLS-1$
            assertLines(List.of(2));
            changeCompatibility(CompatibilityMode.VERSION8_216);
            assertLines(List.of());
            changeCompatibility(CompatibilityMode.VERSION8_31);
            assertLines(List.of(2));
        }
        finally
        {
            changeCompatibility(original);
        }
    }
}
