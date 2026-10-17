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
import com.e1c.v8codestyle.internal.ql.CorePlugin;
import com.e1c.v8codestyle.ql.check.SelectTopWithoutOrderByCheck;

/** Real consumers: source changes and parameter-only revalidation without a clean. */
public class SelectTopWithoutOrderByOwnerTest extends CheckTestBase
{
    private static final String TEMPLATE =
        "Report.QueryTestReport.Template.MainDataCompositionSchema.Template"; //$NON-NLS-1$

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
    public void testBslParameterOnlyCycle() throws Exception
    {
        var project = project();
        var hub = checkRepository.getSettings(new CheckUid("bsl-ql-hub", //$NON-NLS-1$
            "com.e1c.g5.v8.dt.bsl.check"), project.getWorkspaceProject()); //$NON-NLS-1$
        boolean enabled = hub.isEnabled();
        var settings = checkRepository.getSettings(new CheckUid(SelectTopWithoutOrderByCheck.CHECK_ID,
            CorePlugin.PLUGIN_ID), project.getWorkspaceProject());
        var parameter = settings.getParameters().get(SelectTopWithoutOrderByCheck.SKIP_SELECT_TOP_ONE);
        String originalParameter = parameter.getValue();
        var file = project.getWorkspaceProject().getFile("src/CommonModules/QueryWave/Module.bsl"); //$NON-NLS-1$
        byte[] original;
        try (var input = file.getContents())
        {
            original = input.readAllBytes();
        }
        byte[] source = ("Function QueryText() Export\n    Query = New Query;\n" //$NON-NLS-1$
            + "    Query.Text = \"SELECT TOP 1 1 AS Value\";\n" //$NON-NLS-1$
            + "    Return Query.Text;\nEndFunction\n").getBytes(StandardCharsets.UTF_8); //$NON-NLS-1$
        try
        {
            hub.setEnabled(true);
            checkRepository.applyChanges(List.of(hub), project.getWorkspaceProject());
            file.setContents(new ByteArrayInputStream(source), true, true, new NullProgressMonitor());
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(project);
            long stamp = file.getModificationStamp();
            for (String value : List.of("true", "false", "true")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            {
                parameter.setValue(value);
                checkRepository.applyChanges(List.of(settings), project.getWorkspaceProject());
                waitForDD(project);
                assertEquals(Boolean.parseBoolean(value) ? 0 : 1,
                    getMarkersByCheckIds(Set.of(SelectTopWithoutOrderByCheck.CHECK_ID), null, project,
                        markerManager.getMarkers(project.getWorkspaceProject(), file.getFullPath().toString())).size());
                assertEquals(stamp, file.getModificationStamp());
                try (var input = file.getContents())
                {
                    org.junit.Assert.assertArrayEquals(source, input.readAllBytes());
                }
            }
        }
        finally
        {
            file.setContents(new ByteArrayInputStream(original), true, true, new NullProgressMonitor());
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(project);
            hub.setEnabled(enabled);
            parameter.setValue(originalParameter);
            checkRepository.applyChanges(List.of(hub, settings), project.getWorkspaceProject());
            waitForDD(project);
        }
    }

    @Test
    public void testDisabledDcsHubStaysDisabled() throws Exception
    {
        var project = project();
        var hub = checkRepository.getSettings(new CheckUid("dcs-ql-hub", //$NON-NLS-1$
            "com.e1c.g5.v8.dt.form.check"), project.getWorkspaceProject()); //$NON-NLS-1$
        boolean enabled = hub.isEnabled();
        var settings = checkRepository.getSettings(new CheckUid(SelectTopWithoutOrderByCheck.CHECK_ID,
            CorePlugin.PLUGIN_ID), project.getWorkspaceProject());
        var parameter = settings.getParameters().get(SelectTopWithoutOrderByCheck.SKIP_SELECT_TOP_ONE);
        String originalParameter = parameter.getValue();
        long id = getTopObjectIdByFqn(TEMPLATE, project);
        var schema = (DataCompositionSchema)getTopObjectByFqn(TEMPLATE, project);
        String original = ((DataCompositionSchemaDataSetQuery)schema.getDataSets().get(0)).getQuery();
        try
        {
            hub.setEnabled(false);
            checkRepository.applyChanges(List.of(hub), project.getWorkspaceProject());
            waitForDD(project);
            setQuery(project, id, "SELECT TOP 1 1 AS Value"); //$NON-NLS-1$
            for (String value : List.of("false", "true")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                parameter.setValue(value);
                checkRepository.applyChanges(List.of(settings), project.getWorkspaceProject());
                waitForDD(project);
                assertCount(project, id, 0);
                org.junit.Assert.assertFalse(checkRepository.getSettings(hub.getId(),
                    project.getWorkspaceProject()).isEnabled());
            }
        }
        finally
        {
            setQuery(project, id, original);
            parameter.setValue(originalParameter);
            hub.setEnabled(enabled);
            checkRepository.applyChanges(List.of(hub, settings), project.getWorkspaceProject());
            waitForDD(project);
        }
    }

    @Test
    public void testBslOrderingCycle() throws Exception
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
            + "    Query.Text = \"SELECT TOP 2 1 AS Value\n    |\";\n" //$NON-NLS-1$
            + "    Return Query.Text;\nEndFunction\n"; //$NON-NLS-1$
        try
        {
            settings.setEnabled(true);
            checkRepository.applyChanges(List.of(settings), project.getWorkspaceProject());
            waitForDD(project);
            for (String source : List.of(bad, bad.replace("|\"", "|ORDER BY Value\""), bad)) //$NON-NLS-1$ //$NON-NLS-2$
            {
                file.setContents(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), true, true,
                    new NullProgressMonitor());
                ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
                waitForDD(project);
                var markers = getMarkersByCheckIds(Set.of(SelectTopWithoutOrderByCheck.CHECK_ID), null, project,
                    markerManager.getMarkers(project.getWorkspaceProject(), file.getFullPath().toString()));
                int expected = source.contains("ORDER BY") ? 0 : 1; //$NON-NLS-1$
                assertEquals(expected, markers.size());
                if (expected == 1)
                {
                    assertEquals(Integer.valueOf(3), markers.get(0).getExtraInfo().get(StandardExtraInfo.TEXT_LINE));
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
    public void testDcsOrderingAndParameterOnlyCycles() throws Exception
    {
        var project = project();
        var hub = checkRepository.getSettings(new CheckUid("dcs-ql-hub", //$NON-NLS-1$
            "com.e1c.g5.v8.dt.form.check"), project.getWorkspaceProject()); //$NON-NLS-1$
        boolean enabled = hub.isEnabled();
        var settings = checkRepository.getSettings(new CheckUid(SelectTopWithoutOrderByCheck.CHECK_ID,
            CorePlugin.PLUGIN_ID), project.getWorkspaceProject());
        var parameter = settings.getParameters().get(SelectTopWithoutOrderByCheck.SKIP_SELECT_TOP_ONE);
        String originalParameter = parameter.getValue();
        long id = getTopObjectIdByFqn(TEMPLATE, project);
        var schema = (DataCompositionSchema)getTopObjectByFqn(TEMPLATE, project);
        String original = ((DataCompositionSchemaDataSetQuery)schema.getDataSets().get(0)).getQuery();
        try
        {
            hub.setEnabled(true);
            checkRepository.applyChanges(List.of(hub), project.getWorkspaceProject());
            waitForDD(project);
            for (String query : List.of("SELECT TOP 2 1 AS Value", //$NON-NLS-1$
                "SELECT TOP 2 1 AS Value ORDER BY Value", "SELECT TOP 2 1 AS Value")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                setQuery(project, id, query);
                assertCount(project, id, query.contains("ORDER BY") ? 0 : 1); //$NON-NLS-1$
            }
            setQuery(project, id, "SELECT TOP 1 1 AS Value"); //$NON-NLS-1$
            for (String value : List.of("true", "false", "true")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            {
                parameter.setValue(value);
                checkRepository.applyChanges(List.of(settings), project.getWorkspaceProject());
                waitForDD(project);
                assertCount(project, id, Boolean.parseBoolean(value) ? 0 : 1);
                // Parameter-only change must not rewrite the query consumer.
                var current = (DataCompositionSchema)getTopObjectByFqn(TEMPLATE, project);
                assertEquals("SELECT TOP 1 1 AS Value", //$NON-NLS-1$
                    ((DataCompositionSchemaDataSetQuery)current.getDataSets().get(0)).getQuery());
            }
        }
        finally
        {
            setQuery(project, id, original);
            parameter.setValue(originalParameter);
            hub.setEnabled(enabled);
            checkRepository.applyChanges(List.of(settings, hub), project.getWorkspaceProject());
            waitForDD(project);
        }
    }

    private void assertCount(IDtProject project, long id, int expected)
    {
        assertEquals(expected, getMarkersByCheckIds(Set.of(SelectTopWithoutOrderByCheck.CHECK_ID), null, project,
            markerManager.getNestedMarkers(project.getWorkspaceProject(), id)).size());
    }

    private void setQuery(IDtProject project, long id, String query)
    {
        bmModelManager.getModel(project).execute(new AbstractBmTask<Void>("Edit TOP dataset") //$NON-NLS-1$
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
