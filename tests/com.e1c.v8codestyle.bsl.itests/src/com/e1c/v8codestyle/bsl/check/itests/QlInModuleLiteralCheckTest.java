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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.Test;

import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com.e1c.g5.v8.dt.testing.check.CheckTestBase;

/**
 * Research proof for the QL-in-module-literal bridge (EDT check
 * {@code bsl-ql-hub}): a query written as a string literal of a BSL module
 * is parsed into the QL model, and standard QL delegate checks of this
 * project (here: {@code ql-join-to-sub-query} from the ql bundle)
 * report issues that are mapped back into the module lines.
 *
 * This unlocks porting the remaining query-* diagnostics as QL delegate
 * checks instead of the BSL AST channel.
 *
 * @author malikov-pro
 */
public class QlInModuleLiteralCheckTest
    extends CheckTestBase
{

    private static final String PROJECT_NAME = "CommonModule"; //$NON-NLS-1$

    private static final String MODULE_FILE = "/src/CommonModules/CommonModule/Module.bsl"; //$NON-NLS-1$

    private static final String CHECK_ID = "ql-join-to-sub-query"; //$NON-NLS-1$

    private static final String CHECK_CONTRIBUTOR = "com.e1c.v8codestyle.ql"; //$NON-NLS-1$

    private static final String HUB_CHECK_ID = "bsl-ql-hub"; //$NON-NLS-1$

    private static final String HUB_CHECK_CONTRIBUTOR = "com.e1c.g5.v8.dt.bsl.check"; //$NON-NLS-1$

    private static final long WAIT_MILLIS = TimeUnit.SECONDS.toMillis(90);

    private IDtProject dtProject;

    /**
     * The query literal with a join to a sub-query is reported by the QL
     * delegate check.
     *
     * @throws Exception the exception
     */
    @Test
    public void testQueryLiteralViolationIsReported() throws Exception
    {
        updateModule("/resources/ql-module-join-to-sub-query-violation.bsl"); //$NON-NLS-1$

        List<Marker> markers = pollMarkers();
        assertEquals(allMarkersSummary(), 1, markers.size());
    }

    /**
     * A compliant query literal produces no QL delegate check issues.
     *
     * @throws Exception the exception
     */
    @Test
    public void testCleanQueryLiteralHasNoIssues() throws Exception
    {
        updateModule("/resources/ql-module-join-to-sub-query-clean.bsl"); //$NON-NLS-1$

        List<Marker> markers = currentMarkers();
        assertEquals("Clean query literal must not be reported", 0, markers.size()); //$NON-NLS-1$
    }

    private List<Marker> pollMarkers() throws Exception
    {
        long deadline = System.currentTimeMillis() + WAIT_MILLIS;
        List<Marker> markers = List.of();
        while (System.currentTimeMillis() < deadline)
        {
            markers = currentMarkers();
            if (!markers.isEmpty())
            {
                return markers;
            }
            Thread.sleep(500);
        }
        return markers;
    }

    private List<Marker> currentMarkers()
    {
        assertNotNull(dtProject);
        IProject project = dtProject.getWorkspaceProject();
        String moduleId = Path.ROOT.append(PROJECT_NAME).append(MODULE_FILE).toString();
        CheckUid checkUid = new CheckUid(CHECK_ID, CHECK_CONTRIBUTOR);

        List<Marker> markers = List.of(markerManager.getMarkers(project, moduleId));
        return markers.stream()
            .filter(m -> checkUid.equals(checkRepository.getUidForShortUid(m.getCheckId(), project)))
            .toList();
    }

    private String allMarkersSummary()
    {
        assertNotNull(dtProject);
        IProject project = dtProject.getWorkspaceProject();
        String moduleId = Path.ROOT.append(PROJECT_NAME).append(MODULE_FILE).toString();
        StringBuilder sb = new StringBuilder("All module markers: ");
        for (Marker marker : markerManager.getMarkers(project, moduleId))
        {
            sb.append('[')
                .append(marker.getCheckId())
                .append("] ")
                .append(String.valueOf(marker.getMessage()))
                .append("; ");
        }
        return sb.toString();
    }

    private void enableCheck(String checkId, String contributorId)
    {
        IProject project = dtProject.getWorkspaceProject();

        // Warm up the check registry (see StandardChecksProjectOptionProvider:
        // "pre-load all checks to ensure fill-up registry").
        String profile = checkRepository.getActiveSettingsProfile(project);
        checkRepository.getDefaultSettingsForProfile(profile, project);

        CheckUid checkUid = new CheckUid(checkId, contributorId);
        ICheckSettings settings;
        try
        {
            settings = checkRepository.getSettings(checkUid, project);
        }
        catch (NoSuchElementException e)
        {
            // Delegate checks may have no per-project settings entry:
            // they are driven by the hub check enablement alone.
            return;
        }
        if (!settings.isEnabled())
        {
            settings.setEnabled(true);
            checkRepository.applyChanges(List.of(settings), project);
            waitForDD(dtProject);
        }
    }

    private void updateModule(String pathToResource) throws Exception
    {
        dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertNotNull(dtProject);

        // The bsl-ql-hub bridge check is disabled by default in EDT
        // (.disable() in its configuration): enable it and the delegate check.
        enableCheck(HUB_CHECK_ID, HUB_CHECK_CONTRIBUTOR);
        enableCheck(CHECK_ID, CHECK_CONTRIBUTOR);

        IProject project = dtProject.getWorkspaceProject();
        IFile file = project.getFile(MODULE_FILE);
        try (InputStream in = getClass().getResourceAsStream(pathToResource))
        {
            if (file.exists())
            {
                file.setContents(in, true, true, new NullProgressMonitor());
            }
            else
            {
                file.create(in, true, new NullProgressMonitor());
            }
        }
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
        project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        try
        {
            Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
            Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
        }
        catch (OperationCanceledException | InterruptedException e)
        {
            throw new IllegalStateException("Cannot update module file", e); //$NON-NLS-1$
        }
        waitForDD(dtProject);
    }

}
