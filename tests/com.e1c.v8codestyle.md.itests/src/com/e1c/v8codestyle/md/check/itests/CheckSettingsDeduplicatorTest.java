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
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.junit.After;
import org.junit.Test;

import com._1c.g5.v8.dt.core.platform.IDtProject;
import com.e1c.g5.v8.dt.check.settings.CheckSettingsChange;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com.e1c.g5.v8.dt.testing.check.CheckTestBase;
import com.e1c.v8codestyle.check.CheckSettingsDeduplicator;
import com.e1c.v8codestyle.check.CheckUtils;
import com.e1c.v8codestyle.internal.md.CorePlugin;

/**
 * Tests for {@link CheckSettingsDeduplicator} on the real check repository:
 * enabling the fine {@code apk-00126-md-no-yo-letter} check disables the
 * superseded {@code mdo-ru-name-unallowed-letter} check, and nothing is ever
 * enabled back.
 *
 * The deduplicator is driven by a direct {@code onChange} call — check
 * settings change events of the framework are asynchronous (profile store
 * file watching), which would make waiting flaky.
 *
 * @author malikov-pro
 */
public class CheckSettingsDeduplicatorTest
    extends CheckTestBase
{

    private static final String PROJECT_NAME = "MdObjectNameUnallowedLetter"; //$NON-NLS-1$

    private static final String FINE_CHECK_ID = "apk-00126-md-no-yo-letter"; //$NON-NLS-1$

    private static final String COARSE_CHECK_ID = "mdo-ru-name-unallowed-letter"; //$NON-NLS-1$

    private CheckSettingsDeduplicator deduplicator;

    /**
     * Enables the fine check on the current state and fires the change event:
     * the superseded coarse check is disabled, the fine one stays enabled.
     *
     * @throws Exception the exception
     */
    @Test
    public void testFineCheckEnabledDisablesSubstitutedCheck() throws Exception
    {
        IProject project = openProject();
        setDedupEnabled(project, true);
        try
        {
            enableCheck(FINE_CHECK_ID, project);
            enableCheck(COARSE_CHECK_ID, project);

            deduplicator().onChange(project, change(FINE_CHECK_ID));

            assertFalse("Superseded check should be disabled", settings(COARSE_CHECK_ID, project).isEnabled()); //$NON-NLS-1$
            assertTrue("Substituting check should stay enabled", settings(FINE_CHECK_ID, project).isEnabled()); //$NON-NLS-1$
        }
        finally
        {
            setDedupEnabled(project, false);
        }
    }

    /**
     * Fires the change event for the coarse check alone (the user consciously
     * enabled it): nothing is changed — the fine check is disabled in this
     * test, so no pair fires.
     *
     * @throws Exception the exception
     */
    @Test
    public void testCoarseCheckChangeEventDoesNotTrigger() throws Exception
    {
        IProject project = openProject();
        setDedupEnabled(project, true);
        try
        {
            disableCheck(FINE_CHECK_ID, project);
            enableCheck(COARSE_CHECK_ID, project);

            deduplicator().onChange(project, change(COARSE_CHECK_ID));

            assertTrue("Consciously enabled check should not be touched", settings(COARSE_CHECK_ID, project) //$NON-NLS-1$
                .isEnabled());
            assertFalse("Fine check should stay disabled", settings(FINE_CHECK_ID, project).isEnabled()); //$NON-NLS-1$
        }
        finally
        {
            setDedupEnabled(project, false);
        }
    }

    /**
     * The deduplication preference is off (default): settings are never
     * touched implicitly.
     *
     * @throws Exception the exception
     */
    @Test
    public void testDisabledByPreference() throws Exception
    {
        IProject project = openProject();
        assertFalse("Deduplication must be off by default", CheckUtils.isDedupSubstitutedChecksEnable(project)); //$NON-NLS-1$
        try
        {
            enableCheck(FINE_CHECK_ID, project);
            enableCheck(COARSE_CHECK_ID, project);

            deduplicator().onChange(project, change(FINE_CHECK_ID));

            assertTrue("Preference is off: check should stay enabled", settings(COARSE_CHECK_ID, project) //$NON-NLS-1$
                .isEnabled());
        }
        finally
        {
            setDedupEnabled(project, false);
        }
    }

    /**
     * Unidirectionality: a change event for the coarse check never enables it
     * back.
     *
     * @throws Exception the exception
     */
    @Test
    public void testSubstitutedCheckIsNeverEnabledBack() throws Exception
    {
        IProject project = openProject();
        setDedupEnabled(project, true);
        try
        {
            enableCheck(FINE_CHECK_ID, project);
            disableCheck(COARSE_CHECK_ID, project);

            deduplicator().onChange(project, change(COARSE_CHECK_ID));

            assertFalse("Superseded check must never be enabled back", settings(COARSE_CHECK_ID, project) //$NON-NLS-1$
                .isEnabled());
        }
        finally
        {
            setDedupEnabled(project, false);
        }
    }

    /**
     * Stops the deduplicator instance created by the test (tests drive
     * {@code onChange} manually and never register the listener); the
     * preference is restored by each test's {@code finally} block.
     */
    @After
    public void tearDown()
    {
        CheckSettingsDeduplicator deduplicator = this.deduplicator;
        this.deduplicator = null;
        if (deduplicator != null)
        {
            deduplicator.stop();
        }
    }

    private CheckSettingsDeduplicator deduplicator()
    {
        if (deduplicator == null)
        {
            deduplicator = new CheckSettingsDeduplicator(checkRepository);
        }
        return deduplicator;
    }

    private IProject openProject() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertNotNull(dtProject);
        return dtProject.getWorkspaceProject();
    }

    private List<CheckSettingsChange> change(String checkId)
    {
        return List.of(new CheckSettingsChange(new CheckUid(checkId, CorePlugin.PLUGIN_ID)));
    }

    private ICheckSettings settings(String checkId, IProject project)
    {
        ICheckSettings settings = checkRepository.getSettings(new CheckUid(checkId, CorePlugin.PLUGIN_ID), project);
        assertNotNull("Settings for check " + checkId, settings); //$NON-NLS-1$
        return settings;
    }

    private void enableCheck(String checkId, IProject project)
    {
        ICheckSettings settings = settings(checkId, project);
        if (!settings.isEnabled())
        {
            settings.setEnabled(true);
            checkRepository.applyChanges(List.of(settings), project);
        }
    }

    private void disableCheck(String checkId, IProject project)
    {
        ICheckSettings settings = settings(checkId, project);
        if (settings.isEnabled())
        {
            settings.setEnabled(false);
            checkRepository.applyChanges(List.of(settings), project);
        }
    }

    private void setDedupEnabled(IProject project, boolean value)
    {
        CheckUtils.setDedupSubstitutedChecksEnable(project, value);
    }

}
