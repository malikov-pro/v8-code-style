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
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.framework.BundleContext;

import com.e1c.g5.v8.dt.check.settings.CheckSettingsChange;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckRepository;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com.e1c.g5.v8.dt.check.settings.ICheckSettingsChangeListener;
import com.e1c.v8codestyle.internal.CorePlugin;
import com.google.inject.Inject;

/**
 * Enables the EDT check {@code bsl-ql-hub} for projects where the user has
 * not customized its setting. The hub check is the bridge that parses query
 * string literals of BSL modules into the QL model and dispatches them to
 * the QL delegate checks (see {@code _notes/ql-literal-bridge.md}); EDT
 * registers it disabled by default, so query checks never see module
 * literals unless it is turned on.
 *
 * Triggers (all idempotent, act only when settings are readable):
 * <ul>
 * <li>a delayed one-time sweep after start — for projects already open;</li>
 * <li>workspace {@code POST_CHANGE} (project open) with a delayed retry —
 * settings are usually not loaded at the open event itself;</li>
 * <li>check settings change events.</li>
 * </ul>
 * A project is processed once: after this enabler enables the hub, a
 * preference marker makes all further runs skip the project (the check
 * default is disabled, so a later user disable is value-equal to the
 * default and must not be reverted). A project whose settings are not
 * ready yet is retried on the next event. An explicitly configured setting
 * ({@code isDefault() == false}) is never touched: if the user disabled
 * the hub consciously before the first auto-enable, it stays disabled.
 */
public class QlHubAutoEnabler
    implements ICheckSettingsChangeListener, IResourceChangeListener
{

    /** The id of the EDT hub check that drives QL delegate checks. */
    public static final String HUB_CHECK_ID = "bsl-ql-hub"; //$NON-NLS-1$

    /** The contributor plugin of the EDT hub check. */
    public static final String HUB_PLUGIN_ID = "com.e1c.g5.v8.dt.bsl.check"; //$NON-NLS-1$

    private static final String ENABLED_MESSAGE =
        "Check {0} is enabled for project {1}: QL delegate checks require it for query string literals of modules"; //$NON-NLS-1$

    /**
     * Preference key (instance scope, plugin node): the hub was enabled by
     * this enabler for the named project. The check default is disabled, so
     * an explicit user disable is value-equal to the default and cannot be
     * distinguished afterwards — this marker makes the auto-enable strictly
     * one-time per project (survives restarts).
     */
    private static final String AUTO_ENABLED_KEY_PREFIX = "qlHubAutoEnabled."; //$NON-NLS-1$

    private static final long INITIAL_SWEEP_DELAY_MS = 30_000;

    private static final long OPEN_RETRY_DELAY_MS = 15_000;

    private final ICheckRepository checkRepository;

    /**
     * Instantiates a new hub auto enabler.
     *
     * @param checkRepository the check repository service, cannot be {@code null}
     */
    @Inject
    public QlHubAutoEnabler(ICheckRepository checkRepository)
    {
        this.checkRepository = checkRepository;
    }

    /**
     * Starts listening to workspace and check settings events and schedules
     * the initial sweep. Does nothing when the system property
     * {@code v8codestyle.qlHubAutoEnableDisabled} is set (integration test
     * runtimes drive {@link #ensureEnabled(IProject)} manually: background
     * enablement restarts project validation and races tests reading
     * markers).
     *
     * @param bundleContext the bundle context, cannot be {@code null}
     */
    public void start(BundleContext bundleContext)
    {
        if (System.getProperty("v8codestyle.qlHubAutoEnableDisabled") != null) //$NON-NLS-1$
        {
            return;
        }
        checkRepository.addChangeListener(this);
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
        Job sweep = new Job("Enabling the bsl-ql-hub check for workspace projects") //$NON-NLS-1$
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects())
                {
                    ensureEnabled(project);
                }
                return Status.OK_STATUS;
            }
        };
        sweep.setSystem(true);
        sweep.schedule(INITIAL_SWEEP_DELAY_MS);
    }

    /**
     * Stops listening to workspace and check settings events.
     */
    public void stop()
    {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        checkRepository.removeChangeListener(this);
    }

    @Override
    public void onChange(IProject project, Collection<CheckSettingsChange> changes)
    {
        ensureEnabled(project);
    }

    @Override
    public void onPreferenceChange(IProject project)
    {
        ensureEnabled(project);
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event)
    {
        if (event == null || event.getDelta() == null)
        {
            return;
        }
        boolean projectOpened = false;
        for (IResourceDelta delta : event.getDelta().getAffectedChildren())
        {
            if (delta.getResource() instanceof IProject project && delta.getKind() == IResourceDelta.CHANGED
                && (delta.getFlags() & IResourceDelta.OPEN) != 0)
            {
                projectOpened = true;
                // Settings are rarely loaded at the open event itself:
                // retry shortly, when the project services are up.
                scheduleEnsure(project, OPEN_RETRY_DELAY_MS);
            }
        }
        if (projectOpened)
        {
            for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects())
            {
                if (project.isAccessible())
                {
                    ensureEnabled(project);
                }
            }
        }
    }

    private void scheduleEnsure(IProject project, long delayMillis)
    {
        String projectName = project.getName();
        Job retry = new Job("Enabling the bsl-ql-hub check: " + projectName) //$NON-NLS-1$
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                ensureEnabled(project);
                return Status.OK_STATUS;
            }
        };
        retry.setSystem(true);
        retry.schedule(delayMillis);
    }

    /**
     * Enables the hub check for the project if its setting is untouched
     * and the hub was not enabled by this enabler before. Safe to call
     * repeatedly; acts only when the project settings are readable.
     *
     * @param project the project, cannot be {@code null}
     */
    public void ensureEnabled(IProject project)
    {
        if (project == null || !project.isAccessible())
        {
            return;
        }
        if (wasAutoEnabled(project))
        {
            return;
        }
        try
        {
            // Warm up the check registry (see StandardChecksProjectOptionProvider).
            String profile = checkRepository.getActiveSettingsProfile(project);
            checkRepository.getDefaultSettingsForProfile(profile, project);

            CheckUid hubUid = new CheckUid(HUB_CHECK_ID, HUB_PLUGIN_ID);
            ICheckSettings settings = checkRepository.getSettings(hubUid, project);
            if (settings == null || settings.isEnabled() || !settings.isDefault())
            {
                // Already enabled, or the user configured the setting
                // consciously (e.g. disabled it): never touch it.
                return;
            }
            settings.setEnabled(true);
            checkRepository.applyChanges(List.of(settings), project);
            markAutoEnabled(project);
            String message = MessageFormat.format(ENABLED_MESSAGE, HUB_CHECK_ID, project.getName());
            CorePlugin.log(new Status(IStatus.INFO, CorePlugin.PLUGIN_ID, message));
        }
        catch (Exception e)
        {
            // Settings are not ready yet (project loading) or cannot be
            // read: the next event retries, nothing is cached here.
            if (!(e instanceof java.util.NoSuchElementException))
            {
                CorePlugin.logError(e);
            }
        }
    }

    private boolean wasAutoEnabled(IProject project)
    {
        return InstanceScope.INSTANCE.getNode(CorePlugin.PLUGIN_ID)
            .getBoolean(AUTO_ENABLED_KEY_PREFIX + project.getName(), false);
    }

    private void markAutoEnabled(IProject project)
    {
        IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(CorePlugin.PLUGIN_ID);
        prefs.putBoolean(AUTO_ENABLED_KEY_PREFIX + project.getName(), true);
        try
        {
            prefs.flush();
        }
        catch (Exception e)
        {
            CorePlugin.logError(e);
        }
    }

}
