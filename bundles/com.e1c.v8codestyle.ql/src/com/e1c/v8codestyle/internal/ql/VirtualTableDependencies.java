/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.internal.ql;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.core.event.BmEvent;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.bm.integration.event.BmEventFilter;
import com._1c.g5.v8.bm.integration.event.IBmAsyncEventListener;
import com._1c.g5.v8.dt.core.lifecycle.ProjectContext;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.core.platform.IDtProjectManager;
import com._1c.g5.v8.dt.core.platform.IWorkspaceOrchestrator;
import com._1c.g5.v8.dt.core.platform.IWorkspaceOrchestratorListener;
import com._1c.g5.v8.dt.dcs.model.schema.DataCompositionSchemaDataSetQuery;
import com._1c.g5.v8.dt.dcs.model.schema.DcsPackage;
import com._1c.g5.v8.dt.dcs.util.DcsUtil;
import com._1c.g5.v8.dt.dcs.DcsDataSetInfoException;
import com._1c.g5.v8.dt.lifecycle.LifecycleParticipant;
import com._1c.g5.v8.dt.lifecycle.LifecyclePhase;
import com._1c.g5.v8.dt.lifecycle.LifecycleService;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com._1c.g5.v8.dt.ql.model.AbstractQuerySchemaTable;
import com._1c.g5.v8.dt.ql.model.MultiPartQuerySchemaTable;
import com._1c.g5.v8.dt.ql.model.QuerySchemaTable;
import com._1c.g5.v8.dt.ql.model.QuerySchema;
import com._1c.g5.v8.dt.ql.typesystem.IDynamicDbViewFieldComputer;
import com.e1c.g5.v8.dt.check.ICheckScheduler;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckRepository;
import com.e1c.v8codestyle.ql.check.JoinWithVirtualTableCheck;
import com.e1c.v8codestyle.ql.check.VirtualTableCallWithoutParametersCheck;
import com.google.inject.Inject;

/** Watches register views: bound DCS consumers and scoped project-wide BSL delegates. */
@LifecycleService(name = IVirtualTableDependencies.SERVICE_NAME)
public final class VirtualTableDependencies implements IVirtualTableDependencies
{
    private static final CheckUid HUB = new CheckUid("dcs-ql-hub", "com.e1c.g5.v8.dt.form.check"); //$NON-NLS-1$ //$NON-NLS-2$
    private static final CheckUid BSL_HUB = new CheckUid("bsl-ql-hub", "com.e1c.g5.v8.dt.bsl.check"); //$NON-NLS-1$ //$NON-NLS-2$
    private final IBmModelManager models;
    private final IDtProjectManager projects;
    private final IWorkspaceOrchestrator orchestrator;
    private final IWorkspaceOrchestratorListener projectListener;
    private boolean started;
    private final ICheckRepository repository;
    private final ICheckScheduler scheduler;
    private final IDynamicDbViewFieldComputer computer;
    private final Map<IProject, Registration> registrations = new ConcurrentHashMap<>();

    private record Registration(IBmModel model, IBmAsyncEventListener listener) { }

    @Inject
    public VirtualTableDependencies(IBmModelManager models, IDtProjectManager projects,
        IWorkspaceOrchestrator orchestrator, ICheckRepository repository, ICheckScheduler scheduler,
        IDynamicDbViewFieldComputer computer)
    {
        this.models = models;
        this.projects = projects;
        this.orchestrator = orchestrator;
        this.repository = repository;
        this.scheduler = scheduler;
        this.computer = computer;
        projectListener = event -> {
            var project = event.getProject();
            if (project == null)
            {
                return;
            }
            if (event.isProjectClosing())
            {
                detach(project);
            }
            else if (event.isStateChanged() && projects.isProjectActive(project))
            {
                attach(project);
            }
        };
    }

    @LifecycleParticipant(phase = LifecyclePhase.RESOURCE_LOADING, dependsOn = { IBmModelManager.SERVICE_NAME })
    public void init(ProjectContext context)
    {
        attach(context.getProject());
    }

    @Override
    public synchronized void start()
    {
        if (!started)
        {
            // Register first: projects can finish loading during the startup sweep.
            orchestrator.addListener(projectListener);
            started = true;
        }
        for (var project : projects.getDtProjects())
        {
            if (projects.isProjectActive(project))
            {
                attach(project);
            }
        }
    }

    @Override
    public synchronized void stop()
    {
        if (started)
        {
            orchestrator.removeListener(projectListener);
            started = false;
        }
        for (var registration : registrations.values())
        {
            registration.model().removeAsyncEventListener(registration.listener());
        }
        registrations.clear();
    }

    private synchronized void attach(IDtProject dtProject)
    {
        IProject project = dtProject.getWorkspaceProject();
        if (project == null || !project.isAccessible() || registrations.containsKey(project))
        {
            return;
        }
        var model = models.getModel(dtProject);
        if (model != null)
        {
            IBmAsyncEventListener listener = event -> onEvent(dtProject, model, event);
            model.addAsyncEventListener(listener,
                BmEventFilter.eClassChangeFilter(MdClassPackage.Literals.INFORMATION_REGISTER));
            registrations.put(project, new Registration(model, listener));
            CorePlugin.log(new org.eclipse.core.runtime.Status(org.eclipse.core.runtime.IStatus.INFO,
                CorePlugin.PLUGIN_ID, "Virtual-table dependency listener attached: " + project.getName())); //$NON-NLS-1$
        }
    }

    @LifecycleParticipant(phase = LifecyclePhase.RESOURCE_UNLOADING)
    public void dispose(ProjectContext context)
    {
        detach(context.getProject());
    }

    private synchronized void detach(IDtProject dtProject)
    {
        IProject project = dtProject.getWorkspaceProject();
        if (project != null)
        {
            var registration = registrations.remove(project);
            if (registration != null)
            {
                registration.model().removeAsyncEventListener(registration.listener());
            }
        }
    }

    private void onEvent(IDtProject dtProject, IBmModel model, BmEvent event)
    {
        IProject project = dtProject.getWorkspaceProject();
        if (event.getChangeEvents() == null || project == null || !project.isAccessible())
        {
            return;
        }
        var changed = new HashSet<String>();
        for (var change : event.getChangeEvents().values())
        {
            if (change.getObject() instanceof InformationRegister register
                && change.getFeatures().contains(MdClassPackage.Literals.INFORMATION_REGISTER__DB_VIEW_DEFS))
            {
                changed.add(((IBmObject)register).bmGetFqn());
            }
        }
        if (changed.isEmpty())
        {
            return;
        }
        model.execute(new AbstractBmTask<Void>("Revalidate virtual table consumers") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var checks = new HashSet<String>();
                for (String id : Set.of(JoinWithVirtualTableCheck.CHECK_ID,
                    VirtualTableCallWithoutParametersCheck.CHECK_ID))
                {
                    var uid = new CheckUid(id, CorePlugin.PLUGIN_ID);
                    if (repository.getSettings(uid, project).isEnabled())
                    {
                        checks.add(repository.getShortUid(uid, project));
                    }
                }
                if (checks.isEmpty())
                {
                    return null;
                }
                if (repository.getSettings(BSL_HUB, project).isEnabled() && !monitor.isCanceled())
                {
                    // Native BSL scheduling needs the hub and delegates in the same scope.
                    // No proven query-reference index is available here: this scans project modules.
                    var bslChecks = new HashSet<>(checks);
                    bslChecks.add(repository.getShortUid(BSL_HUB, project));
                    scheduler.scheduleValidation(project, bslChecks, java.util.List.of(), transaction, monitor);
                }
                if (!repository.getSettings(HUB, project).isEnabled())
                {
                    return null;
                }
                var consumers = new HashSet<Object>();
                var schemas = transaction.getTopObjectIterator(DcsPackage.Literals.DATA_COMPOSITION_SCHEMA);
                while (schemas.hasNext() && !monitor.isCanceled())
                {
                    var schema = schemas.next();
                    for (var dataset : EcoreUtil2.getAllContentsOfType(schema, DataCompositionSchemaDataSetQuery.class))
                    {
                        QuerySchema query;
                        try
                        {
                            query = DcsUtil.getQuerySchema(dataset.getQuery(), dtProject);
                        }
                        catch (DcsDataSetInfoException e)
                        {
                            CorePlugin.log(CorePlugin.createWarningStatus(
                                "Cannot resolve DCS query while updating virtual-table dependencies: " //$NON-NLS-1$
                                    + schema.bmGetFqn(), e));
                            continue;
                        }
                        if (query != null && EcoreUtil2.getAllContentsOfType(query, QuerySchemaTable.class).stream()
                            .anyMatch(table -> references(table.getTable(), changed)))
                        {
                            consumers.add(schema.bmGetId());
                            break;
                        }
                    }
                }
                if (!consumers.isEmpty() && !monitor.isCanceled())
                {
                    checks.add(repository.getShortUid(HUB, project));
                    scheduler.scheduleValidation(project, checks, consumers, transaction, monitor);
                }
                return null;
            }
        });
    }

    private boolean references(AbstractQuerySchemaTable table, Set<String> changed)
    {
        // Walk bound parent tables as well: the main register still resolves when SliceLast disappears.
        while (table != null)
        {
            var view = computer.computeDbView(table);
            if (view != null && view.getMdObject() instanceof IBmObject owner
                && changed.contains(owner.bmGetTopObject().bmGetFqn()))
            {
                return true;
            }
            table = table instanceof MultiPartQuerySchemaTable part ? part.getParentTable() : null;
        }
        return false;
    }
}
