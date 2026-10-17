/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

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
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegisterPeriodicity;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.testing.check.CheckTestBase;
import com.e1c.v8codestyle.ql.check.VirtualTableCallWithoutParametersCheck;
import com.e1c.v8codestyle.ql.check.JoinWithVirtualTableCheck;

/** Real consumers and provider-only metadata invalidation, without query touch or clean. */
public class VirtualTableCallWithoutParametersOwnerTest extends CheckTestBase
{
    private static final String TEMPLATE =
        "Report.QueryTestReport.Template.MainDataCompositionSchema.Template"; //$NON-NLS-1$
    private static final String BAD = "SELECT 1 AS Value FROM InformationRegister.Prices.SliceLast() V"; //$NON-NLS-1$

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
    public void testLateServiceStartOnAlreadyOpenProject() throws Exception
    {
        assertLateService(false);
    }

    @Test
    public void testReadinessEventAfterEmptyStartupSweep() throws Exception
    {
        assertLateService(true);
    }

    private void assertLateService(boolean initiallyNotReady) throws Exception
    {
        var project = project();
        var injector = com.e1c.v8codestyle.internal.ql.CorePlugin.getDefault().getInjector();
        var registered = injector.getInstance(com.e1c.v8codestyle.internal.ql.IVirtualTableDependencies.class);
        // A separate instance has missed this project's RESOURCE_LOADING phase, as on live lazy activation.
        var callbacks = new java.util.ArrayList<com._1c.g5.v8.dt.core.platform.IWorkspaceOrchestratorListener>();
        var orchestratorApi = com._1c.g5.v8.dt.core.platform.IWorkspaceOrchestrator.class;
        var realOrchestrator = injector.getInstance(orchestratorApi);
        var projectManagerApi = com._1c.g5.v8.dt.core.platform.IDtProjectManager.class;
        var delayedProjects = (com._1c.g5.v8.dt.core.platform.IDtProjectManager)java.lang.reflect.Proxy.newProxyInstance(
            projectManagerApi.getClassLoader(), new Class<?>[] { projectManagerApi }, (proxy, method, args) -> {
                if (initiallyNotReady && "getDtProjects".equals(method.getName())) //$NON-NLS-1$
                {
                    return List.of();
                }
                return method.invoke(dtProjectManager, args);
            });
        var observedOrchestrator = (com._1c.g5.v8.dt.core.platform.IWorkspaceOrchestrator)
            java.lang.reflect.Proxy.newProxyInstance(orchestratorApi.getClassLoader(),
                new Class<?>[] { orchestratorApi }, (proxy, method, args) -> {
                    if ("addListener".equals(method.getName())) //$NON-NLS-1$
                    {
                        callbacks.add((com._1c.g5.v8.dt.core.platform.IWorkspaceOrchestratorListener)args[0]);
                    }
                    else if ("removeListener".equals(method.getName())) //$NON-NLS-1$
                    {
                        callbacks.remove(args[0]);
                    }
                    return method.invoke(realOrchestrator, args);
                });
        var late = new com.e1c.v8codestyle.internal.ql.VirtualTableDependencies(bmModelManager, delayedProjects,
            observedOrchestrator, checkRepository, injector.getInstance(com.e1c.g5.v8.dt.check.ICheckScheduler.class),
            injector.getInstance(com._1c.g5.v8.dt.ql.typesystem.IDynamicDbViewFieldComputer.class));
        var hub = checkRepository.getSettings(new CheckUid("dcs-ql-hub", //$NON-NLS-1$
            "com.e1c.g5.v8.dt.form.check"), project.getWorkspaceProject()); //$NON-NLS-1$
        boolean enabled = hub.isEnabled();
        long id = getTopObjectIdByFqn(TEMPLATE, project);
        var schema = (DataCompositionSchema)getTopObjectByFqn(TEMPLATE, project);
        String original = ((DataCompositionSchemaDataSetQuery)schema.getDataSets().get(0)).getQuery();
        var periodicity = ((InformationRegister)getTopObjectByFqn("InformationRegister.Prices", project)) //$NON-NLS-1$
            .getInformationRegisterPeriodicity();
        var consumer = project.getWorkspaceProject()
            .getFile("src/Reports/QueryTestReport/Templates/MainDataCompositionSchema/Template.dcs"); //$NON-NLS-1$
        long stamp = consumer.getModificationStamp();
        registered.stop();
        try
        {
            hub.setEnabled(true);
            checkRepository.applyChanges(List.of(hub), project.getWorkspaceProject());
            waitForDD(project);
            setQuery(project, id, BAD + " LEFT JOIN Catalog.Products P ON TRUE"); //$NON-NLS-1$
            late.start();
            late.start(); // Startup sweep must be idempotent, not duplicate project listeners.
            assertEquals(1, callbacks.size());
            if (initiallyNotReady)
            {
                var eventApi = com._1c.g5.v8.dt.core.platform.IWorkspaceOrchestratorEvent.class;
                var ready = (com._1c.g5.v8.dt.core.platform.IWorkspaceOrchestratorEvent)
                    java.lang.reflect.Proxy.newProxyInstance(eventApi.getClassLoader(), new Class<?>[] { eventApi },
                        (proxy, method, args) -> "getProject".equals(method.getName()) ? project //$NON-NLS-1$
                            : "isStateChanged".equals(method.getName())); //$NON-NLS-1$
                callbacks.get(0).handleEvent(ready);
                callbacks.get(0).handleEvent(ready); // Repeated readiness does not duplicate BM subscriptions.
            }
            assertCount(project, id, 1);
            assertJoinCount(project, id, 1);
            setPeriodicity(project, InformationRegisterPeriodicity.NONPERIODICAL);
            assertCount(project, id, 0);
            assertJoinCount(project, id, 0);
            setPeriodicity(project, periodicity);
            assertCount(project, id, 1);
            assertJoinCount(project, id, 1);
            assertEquals(stamp, consumer.getModificationStamp());
        }
        finally
        {
            late.stop();
            assertEquals(0, callbacks.size());
            registered.start();
            setPeriodicity(project, periodicity);
            setQuery(project, id, original);
            hub.setEnabled(enabled);
            checkRepository.applyChanges(List.of(hub), project.getWorkspaceProject());
            waitForDD(project);
        }
    }

    @Test
    public void testDcsProviderFileOnlyBothChecks() throws Exception
    {
        var project = project();
        var hub = checkRepository.getSettings(new CheckUid("dcs-ql-hub", //$NON-NLS-1$
            "com.e1c.g5.v8.dt.form.check"), project.getWorkspaceProject()); //$NON-NLS-1$
        boolean enabled = hub.isEnabled();
        long id = getTopObjectIdByFqn(TEMPLATE, project);
        var schema = (DataCompositionSchema)getTopObjectByFqn(TEMPLATE, project);
        String originalQuery = ((DataCompositionSchemaDataSetQuery)schema.getDataSets().get(0)).getQuery();
        var provider = project.getWorkspaceProject().getFile("src/InformationRegisters/Prices/Prices.mdo"); //$NON-NLS-1$
        byte[] original;
        try (var input = provider.getContents())
        {
            original = input.readAllBytes();
        }
        var consumer = project.getWorkspaceProject()
            .getFile("src/Reports/QueryTestReport/Templates/MainDataCompositionSchema/Template.dcs"); //$NON-NLS-1$
        byte[] consumerBytes;
        try (var input = consumer.getContents())
        {
            consumerBytes = input.readAllBytes();
        }
        long stamp = consumer.getModificationStamp();
        String changed = new String(original, StandardCharsets.UTF_8)
            .replace("<informationRegisterPeriodicity>Second</informationRegisterPeriodicity>", //$NON-NLS-1$
                "<informationRegisterPeriodicity>Nonperiodical</informationRegisterPeriodicity>"); //$NON-NLS-1$
        org.junit.Assert.assertFalse(changed.equals(new String(original, StandardCharsets.UTF_8)));
        try
        {
            hub.setEnabled(true);
            checkRepository.applyChanges(List.of(hub), project.getWorkspaceProject());
            waitForDD(project);
            setQuery(project, id, BAD + " LEFT JOIN Catalog.Products P ON TRUE"); //$NON-NLS-1$
            assertCount(project, id, 1);
            assertJoinCount(project, id, 1);
            writeFile(project, provider, changed.getBytes(StandardCharsets.UTF_8));
            assertEquals(InformationRegisterPeriodicity.NONPERIODICAL,
                ((InformationRegister)getTopObjectByFqn("InformationRegister.Prices", project)) //$NON-NLS-1$
                    .getInformationRegisterPeriodicity());
            assertCount(project, id, 0);
            assertJoinCount(project, id, 0);
            writeFile(project, provider, original);
            assertCount(project, id, 1);
            assertJoinCount(project, id, 1);
            assertEquals(stamp, consumer.getModificationStamp());
            try (var input = consumer.getContents())
            {
                assertArrayEquals(consumerBytes, input.readAllBytes());
            }
        }
        finally
        {
            writeFile(project, provider, original);
            setQuery(project, id, originalQuery);
            hub.setEnabled(enabled);
            checkRepository.applyChanges(List.of(hub), project.getWorkspaceProject());
            waitForDD(project);
        }
    }

    @Test
    public void testBslProviderOnlyCycle() throws Exception
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
        var provider = (InformationRegister)getTopObjectByFqn("InformationRegister.Prices", project); //$NON-NLS-1$
        var periodicity = provider.getInformationRegisterPeriodicity();
        byte[] source = ("Function QueryText() Export\n    Query = New Query;\n    Query.Text = \"" //$NON-NLS-1$
            + BAD + " LEFT JOIN Catalog.Products P ON TRUE\";\n    Return Query.Text;\nEndFunction\n") //$NON-NLS-1$
            .getBytes(StandardCharsets.UTF_8);
        var providerFile = project.getWorkspaceProject().getFile("src/InformationRegisters/Prices/Prices.mdo"); //$NON-NLS-1$
        byte[] providerBytes;
        try (var input = providerFile.getContents())
        {
            providerBytes = input.readAllBytes();
        }
        try
        {
            hub.setEnabled(true);
            checkRepository.applyChanges(List.of(hub), project.getWorkspaceProject());
            writeFile(project, file, source);
            long stamp = file.getModificationStamp();
            for (var value : List.of(periodicity, InformationRegisterPeriodicity.NONPERIODICAL, periodicity))
            {
                setPeriodicity(project, value);
                int expected = value == InformationRegisterPeriodicity.NONPERIODICAL ? 0 : 1;
                for (String check : Set.of(VirtualTableCallWithoutParametersCheck.CHECK_ID,
                    JoinWithVirtualTableCheck.CHECK_ID))
                {
                    assertEquals(check, expected, getMarkersByCheckIds(Set.of(check), null, project,
                        markerManager.getMarkers(project.getWorkspaceProject(), file.getFullPath().toString())).size());
                }
                assertEquals(stamp, file.getModificationStamp());
                try (var input = file.getContents())
                {
                    assertArrayEquals(source, input.readAllBytes());
                }
            }
            String changed = new String(providerBytes, StandardCharsets.UTF_8)
                .replace("<informationRegisterPeriodicity>Second</informationRegisterPeriodicity>", //$NON-NLS-1$
                    "<informationRegisterPeriodicity>Nonperiodical</informationRegisterPeriodicity>"); //$NON-NLS-1$
            for (byte[] state : List.of(changed.getBytes(StandardCharsets.UTF_8), providerBytes))
            {
                writeFile(project, providerFile, state);
                var current = (InformationRegister)getTopObjectByFqn("InformationRegister.Prices", project); //$NON-NLS-1$
                int expected = current.getInformationRegisterPeriodicity() == InformationRegisterPeriodicity.NONPERIODICAL
                    ? 0 : 1;
                for (String check : Set.of(VirtualTableCallWithoutParametersCheck.CHECK_ID,
                    JoinWithVirtualTableCheck.CHECK_ID))
                {
                    assertEquals(check, expected, getMarkersByCheckIds(Set.of(check), null, project,
                        markerManager.getMarkers(project.getWorkspaceProject(), file.getFullPath().toString())).size());
                }
                assertEquals(stamp, file.getModificationStamp());
                try (var input = file.getContents())
                {
                    assertArrayEquals(source, input.readAllBytes());
                }
            }
        }
        finally
        {
            writeFile(project, providerFile, providerBytes);
            setPeriodicity(project, periodicity);
            writeFile(project, file, original);
            hub.setEnabled(enabled);
            checkRepository.applyChanges(List.of(hub), project.getWorkspaceProject());
            waitForDD(project);
        }
    }

    private void writeFile(IDtProject project, org.eclipse.core.resources.IFile file, byte[] content) throws Exception
    {
        file.setContents(new ByteArrayInputStream(content), true, true, new NullProgressMonitor());
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
        waitForDD(project);
    }

    private void assertJoinCount(IDtProject project, long id, int expected)
    {
        assertEquals(expected, getMarkersByCheckIds(Set.of(JoinWithVirtualTableCheck.CHECK_ID), null,
            project, markerManager.getNestedMarkers(project.getWorkspaceProject(), id)).size());
    }

    @Test
    public void testDisabledDcsHubAndDelegateAreRespected() throws Exception
    {
        var project = project();
        var hub = checkRepository.getSettings(new CheckUid("dcs-ql-hub", //$NON-NLS-1$
            "com.e1c.g5.v8.dt.form.check"), project.getWorkspaceProject()); //$NON-NLS-1$
        var empty = checkRepository.getSettings(new CheckUid(VirtualTableCallWithoutParametersCheck.CHECK_ID,
            com.e1c.v8codestyle.internal.ql.CorePlugin.PLUGIN_ID), project.getWorkspaceProject());
        var join = checkRepository.getSettings(new CheckUid(JoinWithVirtualTableCheck.CHECK_ID,
            com.e1c.v8codestyle.internal.ql.CorePlugin.PLUGIN_ID), project.getWorkspaceProject());
        boolean hubEnabled = hub.isEnabled();
        boolean emptyEnabled = empty.isEnabled();
        boolean joinEnabled = join.isEnabled();
        var provider = (InformationRegister)getTopObjectByFqn("InformationRegister.Prices", project); //$NON-NLS-1$
        var periodicity = provider.getInformationRegisterPeriodicity();
        long id = getTopObjectIdByFqn(TEMPLATE, project);
        var schema = (DataCompositionSchema)getTopObjectByFqn(TEMPLATE, project);
        String original = ((DataCompositionSchemaDataSetQuery)schema.getDataSets().get(0)).getQuery();
        try
        {
            hub.setEnabled(true);
            empty.setEnabled(false);
            join.setEnabled(true);
            checkRepository.applyChanges(List.of(hub, empty, join), project.getWorkspaceProject());
            waitForDD(project);
            setQuery(project, id, BAD + " LEFT JOIN Catalog.Products P ON TRUE"); //$NON-NLS-1$
            for (var value : List.of(InformationRegisterPeriodicity.NONPERIODICAL, periodicity))
            {
                setPeriodicity(project, value);
                assertCount(project, id, 0);
                assertJoinCount(project, id, value == InformationRegisterPeriodicity.NONPERIODICAL ? 0 : 1);
                org.junit.Assert.assertFalse(checkRepository.getSettings(empty.getId(),
                    project.getWorkspaceProject()).isEnabled());
            }
            hub.setEnabled(false);
            empty.setEnabled(true);
            var oldMarkers = getMarkersByCheckIds(Set.of(JoinWithVirtualTableCheck.CHECK_ID), null, project,
                markerManager.getNestedMarkers(project.getWorkspaceProject(), id));
            var originalMarkers = oldMarkers.stream()
                .map(marker -> marker.getMarkerId() + ":" + marker.getCreatedAt()).toList(); //$NON-NLS-1$
            checkRepository.applyChanges(List.of(hub, empty), project.getWorkspaceProject());
            waitForDD(project);
            assertEquals(originalMarkers, getMarkersByCheckIds(
                Set.of(JoinWithVirtualTableCheck.CHECK_ID), null, project,
                markerManager.getNestedMarkers(project.getWorkspaceProject(), id)).stream()
                .map(marker -> marker.getMarkerId() + ":" + marker.getCreatedAt()).toList()); //$NON-NLS-1$
            for (var value : List.of(InformationRegisterPeriodicity.NONPERIODICAL, periodicity))
            {
                setPeriodicity(project, value);
                assertCount(project, id, 0);
                // Native hub disable retains delegated markers; prove none was recreated.
                assertEquals(originalMarkers, getMarkersByCheckIds(
                    Set.of(JoinWithVirtualTableCheck.CHECK_ID), null, project,
                    markerManager.getNestedMarkers(project.getWorkspaceProject(), id)).stream()
                    .map(marker -> marker.getMarkerId() + ":" + marker.getCreatedAt()).toList()); //$NON-NLS-1$
                org.junit.Assert.assertFalse(checkRepository.getSettings(hub.getId(),
                    project.getWorkspaceProject()).isEnabled());
            }
            // Independently test no NEW diagnostics while the hub is disabled, starting clean.
            hub.setEnabled(true);
            checkRepository.applyChanges(List.of(hub), project.getWorkspaceProject());
            waitForDD(project);
            setQuery(project, id, BAD.replace(".SliceLast()", "") + " LEFT JOIN Catalog.Products P ON TRUE"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertCount(project, id, 0);
            assertJoinCount(project, id, 0);
            hub.setEnabled(false);
            checkRepository.applyChanges(List.of(hub), project.getWorkspaceProject());
            waitForDD(project);
            setQuery(project, id, BAD + " LEFT JOIN Catalog.Products P ON TRUE"); //$NON-NLS-1$
            for (var value : List.of(InformationRegisterPeriodicity.NONPERIODICAL, periodicity))
            {
                setPeriodicity(project, value);
                assertCount(project, id, 0);
                assertJoinCount(project, id, 0);
                org.junit.Assert.assertFalse(checkRepository.getSettings(hub.getId(),
                    project.getWorkspaceProject()).isEnabled());
            }
        }
        finally
        {
            setPeriodicity(project, periodicity);
            setQuery(project, id, original);
            hub.setEnabled(hubEnabled);
            empty.setEnabled(emptyEnabled);
            join.setEnabled(joinEnabled);
            checkRepository.applyChanges(List.of(hub, empty, join), project.getWorkspaceProject());
            waitForDD(project);
        }
    }

    @Test
    public void testBslParameterPositionCycles() throws Exception
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
            for (String args : List.of("()", "(&Period)", "(&Period, )", "(, Price = &Price)", "()")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            {
                String source = "Function QueryText() Export\n    Query = New Query;\n    Query.Text = \"" //$NON-NLS-1$
                    + BAD.replace("()", args) + "\";\n    Return Query.Text;\nEndFunction\n"; //$NON-NLS-1$ //$NON-NLS-2$
                file.setContents(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), true, true,
                    new NullProgressMonitor());
                ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
                waitForDD(project);
                assertEquals(args.equals("()") || args.equals("(&Period, )") ? 1 : 0, //$NON-NLS-1$ //$NON-NLS-2$
                    getMarkersByCheckIds(Set.of(VirtualTableCallWithoutParametersCheck.CHECK_ID), null, project,
                        markerManager.getMarkers(project.getWorkspaceProject(), file.getFullPath().toString())).size());
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
    public void testDcsCriterionAndOrdinaryCycles() throws Exception
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
            for (String args : List.of("()", "(&Period)", "(&Period, )", "(, Price = &Price)", "()")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            {
                setQuery(project, id, BAD.replace("()", args)); //$NON-NLS-1$
                assertCount(project, id, args.equals("()") || args.equals("(&Period, )") ? 1 : 0); //$NON-NLS-1$ //$NON-NLS-2$
            }
            for (String args : List.of("()", "(&Filter)", "()")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            {
                setQuery(project, id, "SELECT 1 AS Value FROM FilterCriterion.QueryFilter" + args + " F"); //$NON-NLS-1$ //$NON-NLS-2$
                assertCount(project, id, args.equals("()") ? 1 : 0); //$NON-NLS-1$
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

    @Test
    public void testDcsProviderPeriodicityOnlyCycle() throws Exception
    {
        var project = project();
        var hub = checkRepository.getSettings(new CheckUid("dcs-ql-hub", //$NON-NLS-1$
            "com.e1c.g5.v8.dt.form.check"), project.getWorkspaceProject()); //$NON-NLS-1$
        boolean enabled = hub.isEnabled();
        long id = getTopObjectIdByFqn(TEMPLATE, project);
        var schema = (DataCompositionSchema)getTopObjectByFqn(TEMPLATE, project);
        String original = ((DataCompositionSchemaDataSetQuery)schema.getDataSets().get(0)).getQuery();
        var provider = (InformationRegister)getTopObjectByFqn("InformationRegister.Prices", project); //$NON-NLS-1$
        var periodicity = provider.getInformationRegisterPeriodicity();
        var consumer = project.getWorkspaceProject()
            .getFile("src/Reports/QueryTestReport/Templates/MainDataCompositionSchema/Template.dcs"); //$NON-NLS-1$
        byte[] before;
        try (var input = consumer.getContents())
        {
            before = input.readAllBytes();
        }
        long stamp = consumer.getModificationStamp();
        try
        {
            hub.setEnabled(true);
            checkRepository.applyChanges(List.of(hub), project.getWorkspaceProject());
            waitForDD(project);
            setQuery(project, id, BAD);
            assertCount(project, id, 1);
            setPeriodicity(project, InformationRegisterPeriodicity.NONPERIODICAL);
            assertCount(project, id, 0); // No resolved SliceLast for a nonperiodic register.
            setPeriodicity(project, periodicity);
            assertCount(project, id, 1);
            assertEquals(stamp, consumer.getModificationStamp());
            try (var input = consumer.getContents())
            {
                assertArrayEquals(before, input.readAllBytes());
            }
        }
        finally
        {
            setPeriodicity(project, periodicity);
            setQuery(project, id, original);
            hub.setEnabled(enabled);
            checkRepository.applyChanges(List.of(hub), project.getWorkspaceProject());
            waitForDD(project);
        }
    }

    private void setPeriodicity(IDtProject project, InformationRegisterPeriodicity value)
    {
        bmModelManager.getModel(project).execute(new AbstractBmTask<Void>("Change virtual table provider") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                ((InformationRegister)transaction.getTopObjectByFqn("InformationRegister.Prices")) //$NON-NLS-1$
                    .setInformationRegisterPeriodicity(value);
                return null;
            }
        });
        waitForDD(project);
    }

    private void assertCount(IDtProject project, long id, int expected)
    {
        assertEquals(expected, getMarkersByCheckIds(Set.of(VirtualTableCallWithoutParametersCheck.CHECK_ID), null,
            project, markerManager.getNestedMarkers(project.getWorkspaceProject(), id)).size());
    }

    private void setQuery(IDtProject project, long id, String query)
    {
        bmModelManager.getModel(project).execute(new AbstractBmTask<Void>("Edit virtual table arguments") //$NON-NLS-1$
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
