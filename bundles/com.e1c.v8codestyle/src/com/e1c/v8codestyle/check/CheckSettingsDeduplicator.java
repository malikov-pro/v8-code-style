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
 *     malikov-pro - initial API and implementation
 *******************************************************************************/
package com.e1c.v8codestyle.check;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.e1c.g5.v8.dt.check.settings.CheckSettingsChange;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckRepository;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com.e1c.g5.v8.dt.check.settings.ICheckSettingsChangeListener;
import com.e1c.v8codestyle.internal.CorePlugin;
import com.google.inject.Inject;

/**
 * Automatically disables a superseded (coarser) check when the finer,
 * substituting check is enabled for a project, so that both do not produce
 * duplicate markers for the same violation. Pairs come from
 * {@link CheckSubstitutionRegistry}.
 *
 * Design principles:
 * <ul>
 * <li>Reacts to check settings change events only, never scans on start-up:
 * on start projects and their settings profiles are not loaded yet.</li>
 * <li>Acts only when the change event touches the <em>fine</em> check of a
 * pair: a user consciously enabling the coarse check alone keeps it enabled.
 * The actual enabled state of both checks is re-read from the check
 * repository — the event payload is not trusted.</li>
 * <li>Unidirectional: only disables the coarse check, never enables anything
 * back — the user may have disabled a check consciously.</li>
 * <li>Optional and transparent: gated by the project preference
 * {@link CheckUtils#PREF_KEY_DEDUP_SUBSTITUTED_CHECKS}, default is off —
 * settings are never touched implicitly; every auto-disable is logged as INFO.</li>
 * <li>Anti ping-pong: an in-flight guard skips events caused by the
 * deduplication itself; the action is also idempotent by condition.</li>
 * </ul>
 * {@link #onPreferenceChange(IProject)} is intentionally ignored: switching
 * the whole settings profile is a conscious user action.
 *
 * @author malikov-pro
 */
public class CheckSettingsDeduplicator
    implements ICheckSettingsChangeListener
{

    private static final String AUTO_DISABLED_PATTERN =
        "Check {0} is automatically disabled for project {2}: superseded by check {1}"; //$NON-NLS-1$

    private final ICheckRepository checkRepository;

    private final AtomicBoolean deduplicationInProgress = new AtomicBoolean();

    /**
     * Instantiates a new check settings deduplicator.
     *
     * @param checkRepository the check repository service, cannot be {@code null}
     */
    @Inject
    public CheckSettingsDeduplicator(ICheckRepository checkRepository)
    {
        this.checkRepository = checkRepository;
    }

    /**
     * Starts listening to check settings changes.
     */
    public void start()
    {
        checkRepository.addChangeListener(this);
    }

    /**
     * Stops listening to check settings changes.
     */
    public void stop()
    {
        checkRepository.removeChangeListener(this);
    }

    @Override
    public void onChange(IProject project, Collection<CheckSettingsChange> changes)
    {
        if (project == null || changes == null || changes.isEmpty()
            || !CheckUtils.isDedupSubstitutedChecksEnable(project))
        {
            return;
        }
        if (!deduplicationInProgress.compareAndSet(false, true))
        {
            return;
        }
        try
        {
            deduplicate(project, changes);
        }
        catch (Exception e)
        {
            CorePlugin.logError(e);
        }
        finally
        {
            deduplicationInProgress.set(false);
        }
    }

    @Override
    public void onPreferenceChange(IProject project)
    {
        // Intentionally ignored: see class javadoc.
    }

    private void deduplicate(IProject project, Collection<CheckSettingsChange> changes)
    {
        Set<String> changedCheckIds = new HashSet<>();
        for (CheckSettingsChange change : changes)
        {
            CheckUid checkId = change.getCheckId();
            if (checkId != null)
            {
                changedCheckIds.add(checkId.getCheckId());
            }
        }
        for (CheckSubstitutionRegistry.Substitution substitution : CheckSubstitutionRegistry.substitutions())
        {
            if (changedCheckIds.contains(substitution.getFineCheckId()))
            {
                disableSubstitutedCheck(project, substitution);
            }
        }
    }

    private void disableSubstitutedCheck(IProject project, CheckSubstitutionRegistry.Substitution substitution)
    {
        ICheckSettings fineSettings = settings(substitution.getFineCheckId(), project);
        if (fineSettings == null || !fineSettings.isEnabled())
        {
            return;
        }
        List<ICheckSettings> substituted = substitutedSettings(substitution.getCoarseCheckId(), project);
        if (substituted.isEmpty())
        {
            return;
        }
        List<ICheckSettings> toDisable = new ArrayList<>();
        for (ICheckSettings settings : substituted)
        {
            if (settings.isEnabled())
            {
                settings.setEnabled(false);
                toDisable.add(settings);
            }
        }
        if (toDisable.isEmpty())
        {
            return;
        }
        checkRepository.applyChanges(toDisable, project);
        String fineCheckId = substitution.getFineCheckId();
        for (ICheckSettings settings : toDisable)
        {
            String message = MessageFormat.format(AUTO_DISABLED_PATTERN, settings.getId()
                .getCheckId(), fineCheckId, project.getName());
            CorePlugin.log(new Status(IStatus.INFO, CorePlugin.PLUGIN_ID, message));
        }
    }

    private ICheckSettings settings(String checkId, IProject project)
    {
        List<ICheckSettings> all = substitutedSettings(checkId, project);
        return all.isEmpty() ? null : all.get(0);
    }

    private List<ICheckSettings> substitutedSettings(String checkId, IProject project)
    {
        Set<CheckUid> uids = checkRepository.getCheckUidForCheckId(checkId, project);
        if (uids == null || uids.isEmpty())
        {
            // Check is not known for the project yet: nothing can be proven,
            // keep the previous behaviour instead of acting on unknown state.
            return List.of();
        }
        return uids.stream()
            .map(uid -> checkRepository.getSettings(uid, project))
            .filter(Objects::nonNull)
            .toList();
    }

}
