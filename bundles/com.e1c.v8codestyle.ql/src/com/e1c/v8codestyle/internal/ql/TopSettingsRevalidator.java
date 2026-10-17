/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.internal.ql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.dcs.model.schema.DcsPackage;
import com.e1c.g5.v8.dt.check.ICheckScheduler;
import com.e1c.g5.v8.dt.check.settings.CheckSettingsChange;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckRepository;
import com.e1c.g5.v8.dt.check.settings.ICheckSettingsChangeListener;
import com.e1c.v8codestyle.ql.check.SelectTopWithoutOrderByCheck;
import com.google.inject.Inject;

/** Restarts DCS consumers when TOP parameters change; never modifies settings or query text. */
public final class TopSettingsRevalidator implements ICheckSettingsChangeListener
{
    private static final CheckUid TOP = new CheckUid(SelectTopWithoutOrderByCheck.CHECK_ID, CorePlugin.PLUGIN_ID);
    private static final CheckUid DCS = new CheckUid("dcs-ql-hub", "com.e1c.g5.v8.dt.form.check"); //$NON-NLS-1$ //$NON-NLS-2$
    private final ICheckRepository repository;
    private final ICheckScheduler scheduler;
    private final IBmModelManager models;

    @Inject
    public TopSettingsRevalidator(ICheckRepository repository, ICheckScheduler scheduler, IBmModelManager models)
    {
        this.repository = repository;
        this.scheduler = scheduler;
        this.models = models;
    }

    public void start()
    {
        repository.addChangeListener(this);
    }

    public void stop()
    {
        repository.removeChangeListener(this);
    }

    @Override
    public void onChange(IProject project, Collection<CheckSettingsChange> changes)
    {
        if (project == null || !project.isAccessible() || changes == null
            || changes.stream().noneMatch(change -> TOP.equals(change.getCheckId()) && change.isSettingsChanged())
            || !repository.getSettings(TOP, project).isEnabled()
            || !repository.getSettings(DCS, project).isEnabled())
        {
            return;
        }
        var model = models.getModel(project);
        if (model == null)
        {
            return;
        }
        model.execute(new AbstractBmTask<Void>("Revalidate TOP settings in DCS queries") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var ids = new ArrayList<Object>();
                var schemas = transaction.getTopObjectIterator(DcsPackage.Literals.DATA_COMPOSITION_SCHEMA);
                while (schemas.hasNext() && !monitor.isCanceled())
                {
                    ids.add(schemas.next().bmGetId());
                }
                if (!ids.isEmpty() && !monitor.isCanceled())
                {
                    // Both IDs are necessary: the scope also filters delegated checks.
                    scheduler.scheduleValidation(project,
                        Set.of(repository.getShortUid(DCS, project), repository.getShortUid(TOP, project)),
                        ids, transaction, new NullProgressMonitor());
                }
                return null;
            }
        });
    }
}
