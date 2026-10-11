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
 *     malikov-pro - port of the APK check АПК_01193
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.dcs.model.schema.DataCompositionSchema;
import com._1c.g5.v8.dt.dcs.model.schema.DataCompositionSchemaDataSetQuery;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.testing.check.CheckTestBase;
import com.e1c.v8codestyle.ql.check.DcsSumLiteralCheck;

/** Real DCS bridge dispatch and incremental query updates. */
public class DcsSumLiteralOwnerTest
    extends CheckTestBase
{
    @Override
    protected boolean enableCleanUp()
    {
        return false;
    }

    /** @throws Exception if validation fails */
    @Test
    public void testDcsBridgeAndQueryUpdate() throws Exception
    {
        var workspaceProject = testingWorkspace.getProject("QlFullDemo"); //$NON-NLS-1$
        IDtProject project = workspaceProject.exists() && workspaceProject.isAccessible()
            ? dtProjectManager.getDtProject(workspaceProject)
            : openProjectAndWaitForValidationFinish("QlFullDemo"); //$NON-NLS-1$
        assertNotNull(project);
        var settings = checkRepository.getSettings(new CheckUid("dcs-ql-hub", //$NON-NLS-1$
            "com.e1c.g5.v8.dt.form.check"), project.getWorkspaceProject()); //$NON-NLS-1$
        boolean originalEnabled = settings.isEnabled();
        long id = getTopObjectIdByFqn("Report.QueryTestReport.Template.MainDataCompositionSchema.Template", project); //$NON-NLS-1$
        try
        {
            settings.setEnabled(true);
            checkRepository.applyChanges(List.of(settings), project.getWorkspaceProject());
            waitForDD(project);
            setQuery(project, id, "SELECT SUM(1) AS Total FROM Catalog.Products AS P"); //$NON-NLS-1$
            assertCount(project, id, 1);
            setQuery(project, id, "SELECT COUNT(*) AS Total FROM Catalog.Products AS P"); //$NON-NLS-1$
            assertCount(project, id, 0);
            setQuery(project, id, "SELECT СУММА(-0) AS Total FROM Catalog.Products AS P"); //$NON-NLS-1$
            assertCount(project, id, 1);
            setQuery(project, id, "SELECT SUM(0) AS Total FROM Catalog.Products AS P"); //$NON-NLS-1$
            assertCount(project, id, 0);
        }
        finally
        {
            setQuery(project, id, ""); //$NON-NLS-1$
            settings.setEnabled(originalEnabled);
            checkRepository.applyChanges(List.of(settings), project.getWorkspaceProject());
            waitForDD(project);
        }
    }

    private void assertCount(IDtProject project, long id, int count)
    {
        assertEquals(count, getMarkersByCheckIds(Set.of(DcsSumLiteralCheck.CHECK_ID), null, project,
            markerManager.getNestedMarkers(project.getWorkspaceProject(), id)).size());
    }

    private void setQuery(IDtProject project, long id, String query) throws Exception
    {
        bmModelManager.getModel(project).execute(new AbstractBmTask<Void>("Edit dataset query") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                DataCompositionSchema schema = (DataCompositionSchema)transaction.getObjectById(id);
                ((DataCompositionSchemaDataSetQuery)schema.getDataSets().get(0)).setQuery(query);
                return null;
            }
        });
        waitForDD(project);
    }
}
