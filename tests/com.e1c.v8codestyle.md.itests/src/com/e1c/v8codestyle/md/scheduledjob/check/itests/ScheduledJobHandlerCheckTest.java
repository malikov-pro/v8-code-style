/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.md.scheduledjob.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com._1c.g5.v8.dt.metadata.mdclass.ScheduledJob;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.v8codestyle.internal.md.CorePlugin;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.md.scheduledjob.check.ScheduledJobHandlerCheck;

public class ScheduledJobHandlerCheckTest extends SingleProjectReadOnlyCheckTestBase
{
    @Override
    protected String getTestConfigurationName()
    {
        return "ScheduledJobs"; //$NON-NLS-1$
    }

    @Test
    public void testBoundariesDefaultsAndPredefinedFlagCycle() throws Exception
    {
        var settings = checkRepository.getSettings(new CheckUid(ScheduledJobHandlerCheck.CHECK_ID,
            CorePlugin.PLUGIN_ID), getProject().getWorkspaceProject());
        assertTrue(settings.isEnabled());
        assertEquals(IssueSeverity.CRITICAL, settings.getSeverity());
        assertEquals(0, settings.getParameters().size());
        try
        {
            for (String reference : List.of("CommonModule.Jobs.WithParameter", //$NON-NLS-1$
                "ОбщийМодуль.Jobs.WithParameter", "cOmMoNmOdUlE.jObS.wItHpArAmEtEr", //$NON-NLS-1$ //$NON-NLS-2$
                "CommonModule.Jobs.WithFunction", "CommonModule.Jobs.PrivateHandler")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                assign(reference);
                assertCount(1);
                setPredefined(false);
                assertCount(0);
                setPredefined(true);
                assertCount(1);
            }
            for (String reference : List.of("", "CommonModule.Jobs", "CommonModule.Jobs.", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                "CommonModule.Missing.WithParameter", "CommonModule.Jobs.Missing", //$NON-NLS-1$ //$NON-NLS-2$
                "Catalog.Jobs.WithParameter", "CommonModule.Jobs.WithParameter.Extra")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                assign(reference);
                assertCount(0);
            }
            assign(null);
            assertCount(0);
            assign("CommonModule.Jobs.WithParameter"); //$NON-NLS-1$
            changeServer(false);
            assertCount(1); // Server issue only; LS stops before parameters and duplicates.
            changeServer(true);
            assertCount(1);
        }
        finally
        {
            changeServer(true);
            setPredefined(true);
            assign("CommonModule.Jobs.PredefinedJob"); //$NON-NLS-1$
        }
    }

    private void setPredefined(boolean value)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Change predefined flag") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                ((ScheduledJob)transaction.getTopObjectByFqn("ScheduledJob.PredefinedJob")).setPredefined(value); //$NON-NLS-1$
                return null;
            }
        });
        waitForDD(getProject());
    }

    @Test
    public void testDuplicateMethodCaseAndIndependentIssues() throws Exception
    {
        try
        {
            assign("CommonModule.Jobs.WithParameter"); //$NON-NLS-1$
            assignOther("CommonModule.Jobs.WithParameter"); //$NON-NLS-1$
            assertCount(1); // Parameter issue remains on predefined job, duplicate on sorted first.
            assertOtherCount(1);
            assignOther("CommonModule.Jobs.withparameter"); //$NON-NLS-1$
            assertOtherCount(0); // Pinned LS duplicate key preserves method-name case.
            assignOther("ОбщийМодуль.jObS.WithParameter"); //$NON-NLS-1$
            assertOtherCount(1);
            changeServer(false);
            assertCount(1);
            assertOtherCount(1); // Server issue on each job, no duplicate/parameter issue.
            changeServer(true);
            assertCount(1);
            assertOtherCount(1);
        }
        finally
        {
            changeServer(true);
            assignOther("CommonModule.Jobs.NotPredefinedJob"); //$NON-NLS-1$
            assign("CommonModule.Jobs.PredefinedJob"); //$NON-NLS-1$
        }
    }

    private void assertOtherCount(int expected)
    {
        var job = getTopObjectByFqn("ScheduledJob.NotPredefinedJob", getProject()); //$NON-NLS-1$
        assertEquals(expected, getMarkersByCheckIds(Set.of(ScheduledJobHandlerCheck.CHECK_ID), job, getProject()).size());
    }

    @Test
    public void testOtherJobFileAssignmentCycle() throws Exception
    {
        var file = getProject().getWorkspaceProject()
            .getFile("/src/ScheduledJobs/PredefinedJob/PredefinedJob.mdo"); //$NON-NLS-1$
        byte[] original;
        try (var input = file.getContents())
        {
            original = input.readAllBytes();
        }
        String text = new String(original, StandardCharsets.UTF_8);
        var consumer = getProject().getWorkspaceProject()
            .getFile("/src/ScheduledJobs/NotPredefinedJob/NotPredefinedJob.mdo"); //$NON-NLS-1$
        long stamp = consumer.getModificationStamp();
        try
        {
            file.setContents(new ByteArrayInputStream(text.replace("CommonModule.Jobs.PredefinedJob", //$NON-NLS-1$
                "CommonModule.Jobs.NotPredefinedJob").getBytes(StandardCharsets.UTF_8)), true, true, //$NON-NLS-1$
                new NullProgressMonitor());
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(getProject());
            assertEquals(1, getMarkersByCheckIds(Set.of(ScheduledJobHandlerCheck.CHECK_ID),
                getTopObjectByFqn("ScheduledJob.NotPredefinedJob", getProject()), getProject()).size()); //$NON-NLS-1$
            file.setContents(new ByteArrayInputStream(original), true, true, new NullProgressMonitor());
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(getProject());
            assertEquals(0, getMarkersByCheckIds(Set.of(ScheduledJobHandlerCheck.CHECK_ID),
                getTopObjectByFqn("ScheduledJob.NotPredefinedJob", getProject()), getProject()).size()); //$NON-NLS-1$
            assertEquals(stamp, consumer.getModificationStamp());
        }
        finally
        {
            file.setContents(new ByteArrayInputStream(original), true, true, new NullProgressMonitor());
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(getProject());
        }
    }

    @Test
    public void testUnchangedDuplicateOwnerCycle() throws Exception
    {
        var consumer = getProject().getWorkspaceProject()
            .getFile("/src/ScheduledJobs/NotPredefinedJob/NotPredefinedJob.mdo"); //$NON-NLS-1$
        long stamp = consumer.getModificationStamp();
        try
        {
            assign("CommonModule.Jobs.NotPredefinedJob"); //$NON-NLS-1$
            assertCount(0);
            assertEquals(1, getMarkersByCheckIds(Set.of(ScheduledJobHandlerCheck.CHECK_ID),
                getTopObjectByFqn("ScheduledJob.NotPredefinedJob", getProject()), getProject()).size()); //$NON-NLS-1$
            assign("CommonModule.Jobs.PredefinedJob"); //$NON-NLS-1$
            assertEquals(0, getMarkersByCheckIds(Set.of(ScheduledJobHandlerCheck.CHECK_ID),
                getTopObjectByFqn("ScheduledJob.NotPredefinedJob", getProject()), getProject()).size()); //$NON-NLS-1$
            assertEquals(stamp, consumer.getModificationStamp());
        }
        finally
        {
            assign("CommonModule.Jobs.PredefinedJob"); //$NON-NLS-1$
        }
    }

    @Test
    public void testProviderServerFileOnlyCycle() throws Exception
    {
        var file = getProject().getWorkspaceProject().getFile("/src/CommonModules/Jobs/Jobs.mdo"); //$NON-NLS-1$
        byte[] original;
        try (var input = file.getContents())
        {
            original = input.readAllBytes();
        }
        String text = new String(original, StandardCharsets.UTF_8);
        var consumer = getProject().getWorkspaceProject()
            .getFile("/src/ScheduledJobs/PredefinedJob/PredefinedJob.mdo"); //$NON-NLS-1$
        long stamp = consumer.getModificationStamp();
        try
        {
            file.setContents(new ByteArrayInputStream(text.replace("<server>true</server>", //$NON-NLS-1$
                "<server>false</server>").getBytes(StandardCharsets.UTF_8)), true, true, new NullProgressMonitor()); //$NON-NLS-1$
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(getProject());
            assertCount(1);
            file.setContents(new ByteArrayInputStream(original), true, true, new NullProgressMonitor());
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(getProject());
            assertCount(0);
            assertEquals(stamp, consumer.getModificationStamp());
        }
        finally
        {
            file.setContents(new ByteArrayInputStream(original), true, true, new NullProgressMonitor());
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(getProject());
        }
    }

    @Test
    public void testProviderServerOnlyCycle() throws Exception
    {
        try
        {
            changeServer(false);
            assertCount(1);
            changeServer(true);
            assertCount(0);
        }
        finally
        {
            changeServer(true);
        }
    }

    private void changeServer(boolean value)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Change job provider Server") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                ((CommonModule)transaction.getTopObjectByFqn("CommonModule.Jobs")).setServer(value); //$NON-NLS-1$
                return null;
            }
        });
        waitForDD(getProject());
    }

    @Test
    public void testOtherJobAssignmentCycle() throws Exception
    {
        try
        {
            assignOther("CommonModule.Jobs.PredefinedJob"); //$NON-NLS-1$
            assertCount(0); // NotPredefinedJob sorts first and owns the single duplicate issue.
            assertEquals(1, getMarkersByCheckIds(Set.of(ScheduledJobHandlerCheck.CHECK_ID),
                getTopObjectByFqn("ScheduledJob.NotPredefinedJob", getProject()), getProject()).size()); //$NON-NLS-1$
            assignOther("CommonModule.Jobs.NotPredefinedJob"); //$NON-NLS-1$
            assertCount(0);
            assertEquals(0, getMarkersByCheckIds(Set.of(ScheduledJobHandlerCheck.CHECK_ID),
                getTopObjectByFqn("ScheduledJob.NotPredefinedJob", getProject()), getProject()).size()); //$NON-NLS-1$
        }
        finally
        {
            assignOther("CommonModule.Jobs.NotPredefinedJob"); //$NON-NLS-1$
        }
    }

    private void assignOther(String handler)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Change other job assignment") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                ((ScheduledJob)transaction.getTopObjectByFqn("ScheduledJob.NotPredefinedJob")).setMethodName(handler); //$NON-NLS-1$
                return null;
            }
        });
        waitForDD(getProject());
    }

    @Test
    public void testProviderOptionalParameterOnlyCycle() throws Exception
    {
        var file = getProject().getWorkspaceProject().getFile("/src/CommonModules/Jobs/Module.bsl"); //$NON-NLS-1$
        byte[] original;
        try (var input = file.getContents())
        {
            original = input.readAllBytes();
        }
        var consumer = getProject().getWorkspaceProject()
            .getFile("/src/ScheduledJobs/PredefinedJob/PredefinedJob.mdo"); //$NON-NLS-1$
        long stamp = consumer.getModificationStamp();
        String text = new String(original, StandardCharsets.UTF_8);
        try
        {
            assign("CommonModule.Jobs.WithParameter"); //$NON-NLS-1$
            assertCount(1);
            file.setContents(new ByteArrayInputStream(text.replace("WithParameter(Value = Undefined)", //$NON-NLS-1$
                "WithParameter()").getBytes(StandardCharsets.UTF_8)), true, true, new NullProgressMonitor()); //$NON-NLS-1$
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(getProject());
            assertCount(0);
            file.setContents(new ByteArrayInputStream(original), true, true, new NullProgressMonitor());
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(getProject());
            assertCount(1);
            assertEquals(stamp, consumer.getModificationStamp());
        }
        finally
        {
            file.setContents(new ByteArrayInputStream(original), true, true, new NullProgressMonitor());
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(getProject());
            assign("CommonModule.Jobs.PredefinedJob"); //$NON-NLS-1$
        }
    }

    @Test
    public void testPredefinedParameters() throws Exception
    {
        try
        {
            assign("CommonModule.Jobs.WithParameter"); //$NON-NLS-1$
            assertCount(1);
            assign("CommonModule.Jobs.PredefinedJob"); //$NON-NLS-1$
            assertCount(0);
        }
        finally
        {
            assign("CommonModule.Jobs.PredefinedJob"); //$NON-NLS-1$
        }
    }

    private void assign(String handler)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Change job assignment") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                ((ScheduledJob)transaction.getTopObjectByFqn("ScheduledJob.PredefinedJob")).setMethodName(handler); //$NON-NLS-1$
                return null;
            }
        });
        waitForDD(getProject());
    }

    private void assertCount(int expected)
    {
        var job = getTopObjectByFqn("ScheduledJob.PredefinedJob", getProject()); //$NON-NLS-1$
        var markers = getMarkersByCheckIds(Set.of(ScheduledJobHandlerCheck.CHECK_ID), job, getProject());
        assertEquals(expected, markers.size());
        for (var marker : markers)
        {
            assertEquals(MdClassPackage.SCHEDULED_JOB__METHOD_NAME, marker.getFeatureId());
            assertTrue(marker.getMessage(), !marker.getMessage().isBlank());
        }
    }
}
