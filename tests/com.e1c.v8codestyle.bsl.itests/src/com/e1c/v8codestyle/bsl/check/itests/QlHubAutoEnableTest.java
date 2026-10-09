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
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.junit.Test;

import com._1c.g5.v8.dt.core.platform.IDtProject;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com.e1c.g5.v8.dt.testing.check.CheckTestBase;
import com.e1c.v8codestyle.check.QlHubAutoEnabler;

/**
 * Tests for {@link QlHubAutoEnabler} logic against the real check
 * repository: {@link QlHubAutoEnabler#ensureEnabled(IProject)} enables the
 * EDT {@code bsl-ql-hub} check for a project with untouched settings, and
 * does not re-enable it after the user disables it explicitly (the
 * once-per-project preference marker).
 * <p>
 * The background triggers of the enabler are disabled in test runtimes
 * (system property {@code v8codestyle.qlHubAutoEnableDisabled}, see
 * tests/pom.xml): their asynchronous enablement restarts project validation
 * and races tests reading markers.
 *
 * @author malikov-pro
 */
public class QlHubAutoEnableTest
    extends CheckTestBase
{

    private static final String PROJECT_NAME = "CommonModule"; //$NON-NLS-1$

    private static final String HUB_CHECK_ID = "bsl-ql-hub"; //$NON-NLS-1$

    private static final String HUB_PLUGIN_ID = "com.e1c.g5.v8.dt.bsl.check"; //$NON-NLS-1$

    /**
     * The enabler enables the hub for untouched settings once; an explicit
     * user disable afterwards is never reverted.
     *
     * @throws Exception the exception
     */
    @Test
    public void testEnableOnceThenRespectExplicitDisable() throws Exception
    {        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertNotNull(dtProject);
        IProject project = dtProject.getWorkspaceProject();

        QlHubAutoEnabler enabler = new QlHubAutoEnabler(checkRepository);
        assertTrue("Fresh settings: the hub must be enabled", hubSettings(project).isDefault()); //$NON-NLS-1$
        assertFalse(hubSettings(project).isEnabled());

        enabler.ensureEnabled(project);
        assertTrue("ensureEnabled must enable the hub for untouched settings", hubSettings(project).isEnabled()); //$NON-NLS-1$

        // Idempotent: a repeated call changes nothing.
        enabler.ensureEnabled(project);
        assertTrue(hubSettings(project).isEnabled());

        ICheckSettings settings = hubSettings(project);
        settings.setEnabled(false);
        checkRepository.applyChanges(List.of(settings), project);

        // applyChanges updates the settings cache asynchronously.
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline && hubSettings(project).isEnabled())
        {
            Thread.sleep(500);
        }
        assertFalse("Explicit disable must be applied", hubSettings(project).isEnabled()); //$NON-NLS-1$

        enabler.ensureEnabled(project);
        assertFalse("Explicit user disable must never be reverted", hubSettings(project).isEnabled()); //$NON-NLS-1$
    }

    private ICheckSettings hubSettings(IProject project)
    {
        return checkRepository.getSettings(new CheckUid(HUB_CHECK_ID, HUB_PLUGIN_ID), project);
    }

}
