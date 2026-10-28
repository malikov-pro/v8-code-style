/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check.itests;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.dcs.model.schema.DataCompositionSchema;
import com._1c.g5.v8.dt.dcs.model.schema.DataCompositionSchemaDataSetQuery;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.testing.check.CheckTestBase;
import com.e1c.v8codestyle.ql.check.JoinWithVirtualTableCheck;

/** Actual BSL and DCS consumers: replacing table kind and removing an existing join. */
public class JoinWithVirtualTableOwnerTest extends CheckTestBase
{
    private static final String TEMPLATE =
        "Report.QueryTestReport.Template.MainDataCompositionSchema.Template"; //$NON-NLS-1$
    private static final String BAD = "SELECT 1 AS Value FROM Catalog.Products AS P\n" //$NON-NLS-1$
        + "LEFT JOIN InformationRegister.Prices.SliceLast() AS V ON TRUE"; //$NON-NLS-1$
    private static final String PHYSICAL = BAD.replace(".SliceLast()", ""); //$NON-NLS-1$ //$NON-NLS-2$
    private static final String STANDALONE = "SELECT 1 AS Value FROM InformationRegister.Prices.SliceLast() AS V"; //$NON-NLS-1$

    @Override
    protected boolean enableCleanUp()
    {
        return false;
    }

    private IDtProject project() throws Exception
    {
        var resource = testingWorkspace.getProject("QlFullDemo"); //$NON-NLS-1$
        return resource.exists() && resource.isAccessible() ? dtProjectManager.getDtProject(resource)
            : openProjectAndWaitForValidationFinish("QlFullDemo"); //$NON-NLS-1$
    }

    @Test
    public void testBslSourceCycles() throws Exception
    {
        var project = project();
        var hub = checkRepository.getSettings(new CheckUid("bsl-ql-hub", //$NON-NLS-1$
            "com.e1c.g5.v8.dt.bsl.check"), project.getWorkspaceProject()); //$NON-NLS-1$
        boolean enabled = hub.isEnabled();
        var file = project.getWorkspaceProject().getFile("src/CommonModules/QueryWave/Module.bsl"); //$NON-NLS-1$
        byte[] original;
        try (var input = file.getContents())
        {
            original = input.readAllBytes();
        }
        try
        {
            hub.setEnabled(true);
            checkRepository.applyChanges(List.of(hub), project.getWorkspaceProject());
            waitForDD(project);
            for (String query : List.of(BAD, PHYSICAL, BAD, STANDALONE, BAD))
            {
                String source = "Function QueryText() Export\n    Query = New Query;\n    Query.Text = \"" //$NON-NLS-1$
                    + query.replace("\n", "\n    |") + "\";\n    Return Query.Text;\nEndFunction\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                file.setContents(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), true, true,
                    new NullProgressMonitor());
                ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
                waitForDD(project);
                var markers = getMarkersByCheckIds(Set.of(JoinWithVirtualTableCheck.CHECK_ID), null, project,
                    markerManager.getMarkers(project.getWorkspaceProject(), file.getFullPath().toString()));
                int expected = BAD.equals(query) ? 1 : 0;
                assertEquals(expected, markers.size());
                if (expected == 1)
                {
                    assertEquals(Integer.valueOf(4), markers.get(0).getExtraInfo().get(StandardExtraInfo.TEXT_LINE));
                }
            }
        }
        finally
        {
            file.setContents(new ByteArrayInputStream(original), true, true, new NullProgressMonitor());
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(project);
            hub.setEnabled(enabled);
            checkRepository.applyChanges(List.of(hub), project.getWorkspaceProject());
            waitForDD(project);
        }
    }

    @Test
    public void testDcsSourceCycles() throws Exception
    {
        var project = project();
        var hub = checkRepository.getSettings(new CheckUid("dcs-ql-hub", //$NON-NLS-1$
            "com.e1c.g5.v8.dt.form.check"), project.getWorkspaceProject()); //$NON-NLS-1$
        boolean enabled = hub.isEnabled();
        long id = getTopObjectIdByFqn(TEMPLATE, project);
        var schema = (DataCompositionSchema)getTopObjectByFqn(TEMPLATE, project);
        String original = ((DataCompositionSchemaDataSetQuery)schema.getDataSets().get(0)).getQuery();
        try
        {
            hub.setEnabled(true);
            checkRepository.applyChanges(List.of(hub), project.getWorkspaceProject());
            waitForDD(project);
            for (String query : List.of(BAD, PHYSICAL, BAD, STANDALONE, BAD))
            {
                setQuery(project, id, query);
                assertEquals(BAD.equals(query) ? 1 : 0,
                    getMarkersByCheckIds(Set.of(JoinWithVirtualTableCheck.CHECK_ID), null, project,
                        markerManager.getNestedMarkers(project.getWorkspaceProject(), id)).size());
            }
        }
        finally
        {
            setQuery(project, id, original);
            hub.setEnabled(enabled);
            checkRepository.applyChanges(List.of(hub), project.getWorkspaceProject());
            waitForDD(project);
        }
    }

    private void setQuery(IDtProject project, long id, String query)
    {
        bmModelManager.getModel(project).execute(new AbstractBmTask<Void>("Edit virtual table join") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var schema = (DataCompositionSchema)transaction.getObjectById(id);
                ((DataCompositionSchemaDataSetQuery)schema.getDataSets().get(0)).setQuery(query);
                return null;
            }
        });
        waitForDD(project);
    }
}
