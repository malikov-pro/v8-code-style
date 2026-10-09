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
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IProject;
import org.junit.Test;

import com._1c.g5.v8.dt.core.platform.IDtProject;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com.e1c.g5.v8.dt.testing.check.CheckTestBase;

/**
 * Tests that the EDT {@code bsl-ql-hub} check is enabled automatically
 * for a project with untouched settings, and that an explicitly disabled
 * setting is never re-enabled (see {@code QlHubAutoEnabler}).
 *
 * @author malikov-pro
 */
public class QlHubAutoEnableTest
    extends CheckTestBase
{

    private static final String PROJECT_NAME = "CommonModule"; //$NON-NLS-1$

    private static final String HUB_CHECK_ID = "bsl-ql-hub"; //$NON-NLS-1$

    private static final String HUB_PLUGIN_ID = "com.e1c.g5.v8.dt.bsl.check"; //$NON-NLS-1$

    private static final long WAIT_MILLIS = TimeUnit.SECONDS.toMillis(90);

    /**
     * A fresh project gets {@code bsl-ql-hub} enabled automatically; after
     * the user disables it explicitly and reopens the project, the setting
     * stays disabled.
     *
     * @throws Exception the exception
     */
    @Test
    public void testHubAutoEnabledAndExplicitDisableRespected() throws Exception
    {
        IProject project = openProject();

        long deadline = System.currentTimeMillis() + WAIT_MILLIS;
        while (System.currentTimeMillis() < deadline)
        {
            ICheckSettings settings = hubSettings(project);
            if (settings != null && settings.isEnabled())
            {
                break;
            }
            Thread.sleep(500);
        }
        ICheckSettings enabled = hubSettings(project);
        assertNotNull("bsl-ql-hub settings must be available", enabled); //$NON-NLS-1$
        assertTrue("bsl-ql-hub must be enabled automatically for untouched settings", enabled.isEnabled()); //$NON-NLS-1$

        disableHub(project);
        deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(15);
        ICheckSettings afterDisable = hubSettings(project);
        while (System.currentTimeMillis() < deadline && afterDisable != null && afterDisable.isEnabled())
        {
            Thread.sleep(500);
            afterDisable = hubSettings(project);
        }
        assertNotNull("Settings must be available after explicit disable", afterDisable); //$NON-NLS-1$
        assertFalse("Explicit disable must be applied", afterDisable.isEnabled()); //$NON-NLS-1$

        // The explicit disable itself emits check settings events (profile
        // store file change): the enabler must not revert the user choice.
        // Give all async triggers their window before asserting.
        deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(25);
        ICheckSettings afterEvents = hubSettings(project);
        while (System.currentTimeMillis() < deadline)
        {
            Thread.sleep(500);
            afterEvents = hubSettings(project);
            if (afterEvents != null && afterEvents.isEnabled())
            {
                break;
            }
        }
        assertNotNull("bsl-ql-hub settings must be available", afterEvents); //$NON-NLS-1$
        assertFalse("Explicitly disabled bsl-ql-hub must stay disabled", afterEvents.isEnabled()); //$NON-NLS-1$
    }

    private IProject openProject() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertNotNull(dtProject);
        return dtProject.getWorkspaceProject();
    }

    private ICheckSettings hubSettings(IProject project)
    {
        try
        {
            return checkRepository.getSettings(new CheckUid(HUB_CHECK_ID, HUB_PLUGIN_ID), project);
        }
        catch (Exception e)
        {
            return null; // registry not ready yet
        }
    }

    private void disableHub(IProject project)
    {
        ICheckSettings settings = hubSettings(project);
        settings.setEnabled(false);
        checkRepository.applyChanges(List.of(settings), project);
    }

}
