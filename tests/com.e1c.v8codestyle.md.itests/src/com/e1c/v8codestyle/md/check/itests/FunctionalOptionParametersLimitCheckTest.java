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
 *     malikov-pro - port of the APK check АПК_00073
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassFactory;
import com.e1c.g5.v8.dt.testing.check.CheckTestBase;
import com.e1c.v8codestyle.md.check.FunctionalOptionParametersLimitCheck;

/** Actual configuration validation, including removal at the boundary. */
public class FunctionalOptionParametersLimitCheckTest
    extends CheckTestBase
{
    /** @throws Exception if project validation fails */
    @Test
    public void testBoundaryAndRemoval() throws Exception
    {
        IDtProject project = openProjectAndWaitForValidationFinish("DataLock"); //$NON-NLS-1$
        assertCount(project, 0);
        setCount(project, 10);
        assertCount(project, 0);
        setCount(project, 11);
        assertCount(project, 1);
        setCount(project, 12);
        assertCount(project, 1);
        setCount(project, 10);
        assertCount(project, 0);
    }

    /** Local extension counts do not represent the complete base configuration. */
    @Test
    public void testExtensionIsExcluded() throws Exception
    {
        openProjectAndWaitForValidationFinish("ExtensionObjectNamePrefixCheck"); //$NON-NLS-1$
        IDtProject project = openProjectAndWaitForValidationFinish("ExtensionObjectNamePrefixCheck_Extension"); //$NON-NLS-1$
        setCount(project, 11);
        assertCount(project, 0);
    }

    private void assertCount(IDtProject project, int expected)
    {
        long id = getTopObjectIdByFqn("Configuration", project); //$NON-NLS-1$
        assertEquals(expected, getMarkersByCheckIds(Set.of(FunctionalOptionParametersLimitCheck.CHECK_ID), id,
            project).size());
    }

    private void setCount(IDtProject project, int count) throws Exception
    {
        bmModelManager.getModel(project).execute(new AbstractBmTask<Void>("Change parameter count") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration"); //$NON-NLS-1$
                var parameters = configuration.getFunctionalOptionsParameters();
                while (parameters.size() > count)
                {
                    var removed = parameters.remove(parameters.size() - 1);
                    transaction.detachTopObject((IBmObject)removed);
                }
                while (parameters.size() < count)
                {
                    var parameter = MdClassFactory.eINSTANCE.createFunctionalOptionsParameter();
                    parameter.setName("Parameter" + (parameters.size() + 1)); //$NON-NLS-1$
                    transaction.attachTopObject((IBmObject)parameter, "FunctionalOptionsParameter." + parameter.getName()); //$NON-NLS-1$
                    parameters.add(parameter);
                }
                return null;
            }
        });
        waitForDD(project);
    }
}
