/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.internal.md;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Set;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.ecore.EClass;

import com._1c.g5.wiring.IManagedService;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.core.platform.IDtProjectManager;
import com._1c.g5.v8.dt.core.platform.IWorkspaceOrchestrator;
import com._1c.g5.v8.dt.core.operations.ProjectPipelineJob;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com.e1c.g5.v8.dt.check.ICheckScheduler;
import com.e1c.g5.v8.dt.check.settings.CheckSettingsChange;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckRepository;
import com.e1c.g5.v8.dt.check.settings.ICheckSettingsChangeListener;
import com.e1c.v8codestyle.md.check.DenyIncompleteValuesCheck;
import com.google.inject.Inject;

/** Maintain this audit's marker scope on settings changes, without a dependency-wide rebuild. */
public final class DimensionAuditSettingsListener implements IManagedService, ICheckSettingsChangeListener
{
    private static final CheckUid UID = new CheckUid(DenyIncompleteValuesCheck.CHECK_ID, CorePlugin.PLUGIN_ID);

    @Inject
    private ICheckRepository repository;

    @Inject
    private ICheckScheduler scheduler;

    @Inject
    private IBmModelManager models;

    @Inject
    private IDtProjectManager projects;

    @Inject
    private IWorkspaceOrchestrator orchestrator;

    private volatile boolean active;

    private final Object jobFamily = new Object();

    @Override
    public void activate()
    {
        active = true;
        repository.addChangeListener(this);
    }

    @Override
    public void deactivate()
    {
        repository.removeChangeListener(this);
        active = false;
        Job.getJobManager().cancel(jobFamily);
    }

    @Override
    public void onChange(IProject project, Collection<CheckSettingsChange> changes)
    {
        if (active && project != null && project.isAccessible()
            && changes != null && changes.stream().anyMatch(c -> UID.equals(c.getCheckId())
                && (c.isEnabled() || c.isDisabled() || c.isSettingsChanged())))
        {
            updateAudit(project);
        }
    }

    @Override
    public void onPreferenceChange(IProject project)
    {
        if (active && project != null && project.isAccessible() && repository.getShortUid(UID).keySet().stream()
            .anyMatch(p -> project.equals(p.getWorkspaceProject())))
        {
            updateAudit(project);
        }
    }

    private void updateAudit(IProject project)
    {
        if (!repository.getSettings(UID, project).isEnabled())
        {
            String shortUid = repository.getShortUid(UID, project);
            if (shortUid != null)
            {
                // scheduleClearance expects marker short UIDs, not symbolic check IDs.
                scheduler.scheduleClearance(project, Set.of(shortUid), new NullProgressMonitor());
            }
            return;
        }
        // Live RED: immediate scheduling missed checks despite collecting all four owner IDs.
        // Wait for the framework settings rebuild, then enqueue this audit alone on explicit owners.
        Job job = new Job("Revalidate dimension audit after settings pipeline") //$NON-NLS-1$
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                var dtProject = projects.getDtProject(project);
                if (!active || dtProject == null || monitor.isCanceled() || !project.isAccessible())
                {
                    return Status.CANCEL_STATUS;
                }
                Object operation = orchestrator.beginExclusiveOperation("Wait for dimension audit settings", //$NON-NLS-1$
                    List.of(dtProject), ProjectPipelineJob.AFTER_BUILD_DD);
                orchestrator.endOperation(operation);
                if (active && !monitor.isCanceled() && project.isAccessible()
                    && repository.getSettings(UID, project).isEnabled())
                {
                    scheduleRegisters(project);
                }
                return Status.OK_STATUS;
            }

            @Override
            public boolean belongsTo(Object family)
            {
                return family == jobFamily;
            }
        };
        job.setSystem(true);
        job.schedule();
    }

    private void scheduleRegisters(IProject project)
    {
        var model = models.getModel(project);
        if (model == null)
        {
            return;
        }
        model.execute(new AbstractBmTask<Void>("Revalidate register dimension audit settings") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var ids = new ArrayList<Object>();
                for (EClass type : new EClass[] { MdClassPackage.Literals.INFORMATION_REGISTER,
                    MdClassPackage.Literals.ACCUMULATION_REGISTER, MdClassPackage.Literals.ACCOUNTING_REGISTER,
                    MdClassPackage.Literals.CALCULATION_REGISTER })
                {
                    var objects = transaction.getTopObjectIterator(type);
                    while (objects.hasNext() && !monitor.isCanceled())
                    {
                        var owner = objects.next();
                        ids.add(owner.bmGetId());
                    }
                }
                if (active && !ids.isEmpty() && !monitor.isCanceled()
                    && repository.getSettings(UID, project).isEnabled())
                {
                    // Explicit IDs avoid the full-settings dependency traversal. No BSL or unrelated metadata checks.
                    scheduler.scheduleValidation(project, Set.of(UID.getCheckId()), ids, false, transaction, monitor);
                }
                return null;
            }
        });
    }
}
