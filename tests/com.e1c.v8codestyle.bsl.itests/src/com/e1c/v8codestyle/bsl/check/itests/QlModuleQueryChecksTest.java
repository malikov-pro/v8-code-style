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
import java.util.Map;
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
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com.e1c.g5.v8.dt.testing.check.CheckTestBase;

/**
 * Tests for the query-* checks batch (ql bundle) running on query string
 * literals of a BSL module through the EDT {@code bsl-ql-hub} bridge:
 * <ul>
 * <li>{@code ql-full-outer-join} — full outer join;</li>
 * <li>{@code ql-logical-or-in-join} — logical OR in join conditions;</li>
 * <li>{@code ql-logical-or-in-where} — logical OR in the WHERE section;</li>
 * <li>{@code ql-assign-alias-fields} — select fields without explicit alias.</li>
 * </ul>
 *
 * @author malikov-pro
 */
public class QlModuleQueryChecksTest
    extends CheckTestBase
{

    private static final String PROJECT_NAME = "CommonModule"; //$NON-NLS-1$

    private static final String MODULE_FILE = "/src/CommonModules/CommonModule/Module.bsl"; //$NON-NLS-1$

    private static final String CHECK_CONTRIBUTOR = "com.e1c.v8codestyle.ql"; //$NON-NLS-1$

    private static final String HUB_CHECK_ID = "bsl-ql-hub"; //$NON-NLS-1$

    private static final String HUB_PLUGIN_ID = "com.e1c.g5.v8.dt.bsl.check"; //$NON-NLS-1$

    private static final String FULL_OUTER_JOIN = "ql-full-outer-join"; //$NON-NLS-1$

    private static final String LOGICAL_OR_IN_JOIN = "ql-logical-or-in-join"; //$NON-NLS-1$

    private static final String LOGICAL_OR_IN_WHERE = "ql-logical-or-in-where"; //$NON-NLS-1$

    private static final String ASSIGN_ALIAS_FIELDS = "ql-assign-alias-fields"; //$NON-NLS-1$

    private static final long WAIT_MILLIS = TimeUnit.SECONDS.toMillis(120);

    /**
     * Each of the four checks reports exactly one violation in the module
     * query literals.
     *
     * @throws Exception the exception
     */
    @Test
    public void testViolationsReported() throws Exception
    {
        updateModule("/resources/ql-module-query-checks-violations.bsl"); //$NON-NLS-1$

        Map<String, Long> expected = Map.of(FULL_OUTER_JOIN, 1L, LOGICAL_OR_IN_JOIN, 1L, LOGICAL_OR_IN_WHERE, 1L,
            ASSIGN_ALIAS_FIELDS, 1L);
        Map<String, Long> counts = pollCounts(expected);
        for (Map.Entry<String, Long> entry : expected.entrySet())
        {
            assertEquals("Markers of " + entry.getKey(), entry.getValue(), counts.get(entry.getKey())); //$NON-NLS-1$
        }
        assertMarkerLine(LOGICAL_OR_IN_JOIN, 37);
        assertMarkerLine(LOGICAL_OR_IN_WHERE, 54);
    }

    /**
     * A compliant module produces no markers of the four checks.
     *
     * @throws Exception the exception
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule("/resources/ql-module-query-checks-clean.bsl"); //$NON-NLS-1$

        // Markers of the previous module state are removed asynchronously:
        // wait until the counts settle, then assert zeros.
        Map<String, Long> zeros = Map.of(FULL_OUTER_JOIN, 0L, LOGICAL_OR_IN_JOIN, 0L, LOGICAL_OR_IN_WHERE, 0L,
            ASSIGN_ALIAS_FIELDS, 0L);
        Map<String, Long> counts = pollSettled(zeros);
        for (Map.Entry<String, Long> entry : zeros.entrySet())
        {
            assertEquals("Markers of " + entry.getKey(), entry.getValue(), counts.get(entry.getKey())); //$NON-NLS-1$
        }
    }

    private Map<String, Long> pollCounts(Map<String, Long> expected) throws InterruptedException
    {
        long deadline = System.currentTimeMillis() + WAIT_MILLIS;
        Map<String, Long> counts = currentCounts();
        while (System.currentTimeMillis() < deadline && !counts.equals(expected))
        {
            Thread.sleep(1000);
            counts = currentCounts();
        }
        return counts;
    }

    private Map<String, Long> pollSettled(Map<String, Long> zeros) throws InterruptedException
    {
        long deadline = System.currentTimeMillis() + WAIT_MILLIS;
        Map<String, Long> previous = null;
        Map<String, Long> counts = currentCounts();
        while (System.currentTimeMillis() < deadline)
        {
            if (zeros.equals(counts) && counts.equals(previous))
            {
                break;
            }
            previous = counts;
            Thread.sleep(1500);
            counts = currentCounts();
        }
        return counts;
    }

    private Map<String, Long> currentCounts()
    {
        assertNotNull(dtProject);
        IProject project = dtProject.getWorkspaceProject();
        String moduleId = Path.ROOT.append(PROJECT_NAME).append(MODULE_FILE).toString();

        List<Marker> markers = List.of(markerManager.getMarkers(project, moduleId));
        return Map.of(FULL_OUTER_JOIN, count(markers, FULL_OUTER_JOIN, project), LOGICAL_OR_IN_JOIN,
            count(markers, LOGICAL_OR_IN_JOIN, project), LOGICAL_OR_IN_WHERE, count(markers, LOGICAL_OR_IN_WHERE,
                project), ASSIGN_ALIAS_FIELDS, count(markers, ASSIGN_ALIAS_FIELDS, project));
    }

    private long count(List<Marker> markers, String checkId, IProject project)
    {
        CheckUid checkUid = new CheckUid(checkId, CHECK_CONTRIBUTOR);
        return markers.stream()
            .filter(m -> checkUid.equals(checkRepository.getUidForShortUid(m.getCheckId(), project)))
            .count();
    }

    private void assertMarkerLine(String checkId, int expectedLine)
    {
        IProject project = dtProject.getWorkspaceProject();
        String moduleId = Path.ROOT.append(PROJECT_NAME).append(MODULE_FILE).toString();
        CheckUid checkUid = new CheckUid(checkId, CHECK_CONTRIBUTOR);
        Marker marker = List.of(markerManager.getMarkers(project, moduleId)).stream()
            .filter(m -> checkUid.equals(checkRepository.getUidForShortUid(m.getCheckId(), project)))
            .findFirst().orElseThrow();
        assertNotNull(marker.getExtraInfo());
        Integer actualLine = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
        assertEquals("Marker line of " + checkId, Integer.valueOf(expectedLine), actualLine); //$NON-NLS-1$
    }

    private void updateModule(String pathToResource) throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertNotNull(dtProject);
        this.dtProject = dtProject;
        IProject project = dtProject.getWorkspaceProject();

        // The bridge check is disabled by default in EDT: enable it (the
        // auto-enabler usually did it already, this is a no-op then).
        enableCheck(HUB_CHECK_ID, HUB_PLUGIN_ID, project);

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

    private void enableCheck(String checkId, String contributorId, IProject project)
    {
        String profile = checkRepository.getActiveSettingsProfile(project);
        checkRepository.getDefaultSettingsForProfile(profile, project);
        CheckUid checkUid = new CheckUid(checkId, contributorId);
        try
        {
            ICheckSettings settings = checkRepository.getSettings(checkUid, project);
            if (settings != null && !settings.isEnabled())
            {
                settings.setEnabled(true);
                checkRepository.applyChanges(List.of(settings), project);
                waitForDD(dtProject);
            }
        }
        catch (Exception e)
        {
            // Registry not ready or check unknown: the auto-enabler and
            // defaults cover it.
        }
    }

    private IDtProject dtProject;

}
