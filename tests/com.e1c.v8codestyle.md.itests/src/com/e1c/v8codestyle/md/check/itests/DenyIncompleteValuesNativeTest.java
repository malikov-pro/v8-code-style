/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.metadata.mdclass.RegisterDimension;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;

/** Native-only flag probe: no new product check is registered during this baseline. */
public class DenyIncompleteValuesNativeTest extends SingleProjectReadOnlyCheckTestBase
{
    @Override
    public void setUp() throws org.eclipse.core.runtime.CoreException
    {
        testingWorkspace.cleanUpWorkspace();
        super.setUp();
    }

    private static final List<String> OWNERS = List.of("InformationRegister.Information", //$NON-NLS-1$
        "AccumulationRegister.Accumulation", "AccountingRegister.Accounting", "CalculationRegister.Calculation"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    @Override
    protected String getTestConfigurationName()
    {
        return "DenyIncompleteValues"; //$NON-NLS-1$
    }

    @Test
    public void testNativeFlagCycles() throws Exception
    {
        var evidence = new ArrayList<String>();
        for (String fqn : OWNERS)
        {
            var owner = getTopObjectByFqn(fqn, getProject());
            assertNotNull(owner);
            var dimension = dimensions(owner).get(0);
            assertEquals(false, dimension.isDenyIncompleteValues());
            var initial = markers(fqn);
            evidence.add(fqn + " false=" + initial); //$NON-NLS-1$
            try
            {
                changeFlag(fqn, true);
                assertEquals(true, dimensions(getTopObjectByFqn(fqn, getProject())).get(0).isDenyIncompleteValues());
                var enabled = markers(fqn);
                evidence.add(fqn + " true=" + enabled); //$NON-NLS-1$
                assertEquals("Native flag audit appeared", initial, enabled); //$NON-NLS-1$
                changeFlag(fqn, false);
                assertEquals(initial, markers(fqn));
            }
            finally
            {
                changeFlag(fqn, false);
            }
        }
        var out = Path.of("/home/aleksandr/edt-plugins/_notes/evidence/dimensions-regions-20261105"); //$NON-NLS-1$
        Files.createDirectories(out);
        Files.write(out.resolve("native-dimension-flags.txt"), evidence); //$NON-NLS-1$
    }

    private List<String> markers(String fqn)
    {
        var owner = getTopObjectByFqn(fqn, getProject());
        return Arrays.stream(markerManager.getNestedMarkers(getProject().getWorkspaceProject(), owner.bmGetId()))
            .filter(m -> !"deny-incomplete-values".equals(getCheckIdFromMarker(m, getProject()))) //$NON-NLS-1$
            .map(m -> getCheckIdFromMarker(m, getProject()) + ":" + m.getMessage()) //$NON-NLS-1$
            .sorted().toList();
    }

    private static List<RegisterDimension> dimensions(EObject owner)
    {
        return owner.eContents().stream().filter(RegisterDimension.class::isInstance)
            .map(RegisterDimension.class::cast).toList();
    }

    private void changeFlag(String fqn, boolean value)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Toggle native dimension flag") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                dimensions(transaction.getTopObjectByFqn(fqn)).get(0).setDenyIncompleteValues(value);
                return null;
            }
        });
        waitForDD(getProject());
    }
}
