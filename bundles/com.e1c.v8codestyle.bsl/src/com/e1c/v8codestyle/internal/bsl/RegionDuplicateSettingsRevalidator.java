/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.internal.bsl;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;

import com._1c.g5.wiring.IManagedService;
import com.e1c.g5.v8.dt.check.ICheckScheduler;
import com.e1c.g5.v8.dt.check.settings.CheckSettingsChange;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckRepository;
import com.e1c.g5.v8.dt.check.settings.ICheckSettingsChangeListener;
import com.e1c.v8codestyle.bsl.check.DuplicateRegionCheck;
import com.google.inject.Inject;

/** Recompute legacy ownership on new-check changes without changing a profile or touching source files. */
public final class RegionDuplicateSettingsRevalidator implements IManagedService, ICheckSettingsChangeListener
{
    private static final CheckUid DUPLICATE = new CheckUid(DuplicateRegionCheck.CHECK_ID, BslPlugin.PLUGIN_ID);
    private static final CheckUid LEGACY = new CheckUid("module-structure-top-region", BslPlugin.PLUGIN_ID); //$NON-NLS-1$

    @Inject
    private ICheckRepository repository;

    @Inject
    private ICheckScheduler scheduler;

    @Override
    public void activate()
    {
        repository.addChangeListener(this);
    }

    @Override
    public void deactivate()
    {
        repository.removeChangeListener(this);
    }

    @Override
    public void onChange(IProject project, Collection<CheckSettingsChange> changes)
    {
        if (project != null && project.isAccessible() && changes != null && changes.stream().anyMatch(c ->
            DUPLICATE.equals(c.getCheckId()) && (c.isEnabled() || c.isDisabled() || c.isSettingsChanged()))
            && repository.getSettings(LEGACY, project).isEnabled())
        {
            scheduler.scheduleValidation(project, Set.of(LEGACY.getCheckId()), List.of(), new NullProgressMonitor());
        }
    }

    @Override
    public void onPreferenceChange(IProject project)
    {
        // A profile switch can change both owners even when source files are unchanged.
        if (project != null && project.isAccessible()
            && repository.getShortUid(DUPLICATE).keySet().stream()
                .anyMatch(p -> project.equals(p.getWorkspaceProject()))
            && repository.getSettings(LEGACY, project).isEnabled())
        {
            scheduler.scheduleValidation(project, Set.of(LEGACY.getCheckId()), List.of(), new NullProgressMonitor());
        }
    }
}
