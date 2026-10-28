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
import com.e1c.v8codestyle.ql.check.UnionAllCheck;

/** Real BSL and DCS bridge cycles, separate from direct delegate invocation. */
public class UnionAllOwnerTest extends CheckTestBase
{
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
    public void testBslBridgeLiteralCycle() throws Exception
    {
        var project = project();
        var settings = checkRepository.getSettings(new CheckUid("bsl-ql-hub", //$NON-NLS-1$
            "com.e1c.g5.v8.dt.bsl.check"), project.getWorkspaceProject()); //$NON-NLS-1$
        boolean enabled = settings.isEnabled();
        var file = project.getWorkspaceProject().getFile("src/CommonModules/QueryWave/Module.bsl"); //$NON-NLS-1$
        byte[] original;
        try (var input = file.getContents())
        {
            original = input.readAllBytes();
        }
        String bad = "Function QueryText() Export\n    Query = New Query;\n" //$NON-NLS-1$
            + "    Query.Text = \"SELECT 1 AS Value\n    |UNION\n    |SELECT 2 AS Value\";\n" //$NON-NLS-1$
            + "    Return Query.Text;\nEndFunction\n"; //$NON-NLS-1$
        try
        {
            settings.setEnabled(true);
            checkRepository.applyChanges(List.of(settings), project.getWorkspaceProject());
            waitForDD(project);
            for (String source : List.of(bad, bad.replace("|UNION", "|UNION ALL"), bad)) //$NON-NLS-1$ //$NON-NLS-2$
            {
                file.setContents(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), true, true,
                    new NullProgressMonitor());
                ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
                waitForDD(project);
                var markers = getMarkersByCheckIds(Set.of(UnionAllCheck.CHECK_ID), null, project,
                    markerManager.getMarkers(project.getWorkspaceProject(), file.getFullPath().toString()));
                int expected = source.contains("UNION ALL") ? 0 : 1; //$NON-NLS-1$
                assertEquals(expected, markers.size());
                if (expected == 1)
                {
                    assertEquals(Integer.valueOf(4), markers.get(0).getExtraInfo().get(StandardExtraInfo.TEXT_LINE));
                    // Native BSL bridge anchors the literal line, including its continuation prefix.
                    assertEquals(Integer.valueOf("|UNION".length()), //$NON-NLS-1$
                        markers.get(0).getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH));
                }
            }
        }
        finally
        {
            file.setContents(new ByteArrayInputStream(original), true, true, new NullProgressMonitor());
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(project);
            settings.setEnabled(enabled);
            checkRepository.applyChanges(List.of(settings), project.getWorkspaceProject());
            waitForDD(project);
        }
    }

    @Test
    public void testDcsQueryCycle() throws Exception
    {
        var project = project();
        var settings = checkRepository.getSettings(new CheckUid("dcs-ql-hub", //$NON-NLS-1$
            "com.e1c.g5.v8.dt.form.check"), project.getWorkspaceProject()); //$NON-NLS-1$
        boolean enabled = settings.isEnabled();
        long id = getTopObjectIdByFqn("Report.QueryTestReport.Template.MainDataCompositionSchema.Template", project); //$NON-NLS-1$
        var schema = (DataCompositionSchema)getTopObjectByFqn(
            "Report.QueryTestReport.Template.MainDataCompositionSchema.Template", project); //$NON-NLS-1$
        String original = ((DataCompositionSchemaDataSetQuery)schema.getDataSets().get(0)).getQuery();
        try
        {
            settings.setEnabled(true);
            checkRepository.applyChanges(List.of(settings), project.getWorkspaceProject());
            waitForDD(project);
            for (String query : List.of("SELECT 1 AS Value UNION SELECT 2 AS Value", //$NON-NLS-1$
                "SELECT 1 AS Value UNION ALL SELECT 2 AS Value", "SELECT 1 AS Value UNION SELECT 2 AS Value")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                setQuery(project, id, query);
                assertEquals(query.contains("UNION ALL") ? 0 : 1, //$NON-NLS-1$
                    getMarkersByCheckIds(Set.of(UnionAllCheck.CHECK_ID), null, project,
                        markerManager.getNestedMarkers(project.getWorkspaceProject(), id)).size());
            }
        }
        finally
        {
            setQuery(project, id, original);
            settings.setEnabled(enabled);
            checkRepository.applyChanges(List.of(settings), project.getWorkspaceProject());
            waitForDD(project);
        }
    }

    private void setQuery(IDtProject project, long id, String query)
    {
        bmModelManager.getModel(project).execute(new AbstractBmTask<Void>("Edit UNION dataset") //$NON-NLS-1$
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
