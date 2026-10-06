/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: 1C-Soft LLC
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the APK check АПК_01219
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

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

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.ApkCutCommentsFormatCheck;

/**
 * Tests for {@link ApkCutCommentsFormatCheck} — port of the APK check
 * АПК_01219 (standard 769: cut service comments formatting).
 *
 * @author malikov-pro
 */
public class ApkCutCommentsFormatCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String PROJECT_NAME = "ApkCutCommentsCheck"; //$NON-NLS-1$

    private static final String ORDINARY_MODULE_FILE = "/src/CommonModules/ОбычныйМодуль/Module.bsl"; //$NON-NLS-1$

    private static final String LOCALIZATION_MODULE_FILE = "/src/CommonModules/МойМодульЛокализация/Module.bsl"; //$NON-NLS-1$

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "apk-01219-cut-comments-format.bsl"; //$NON-NLS-1$

    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-01219-cut-comments-format-clean.bsl"; //$NON-NLS-1$

    private static final String RESOURCE_POSTFIX_NO_COMMENTS =
        FOLDER_RESOURCE + "apk-01219-cut-comments-format-postfix.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public ApkCutCommentsFormatCheckTest()
    {
        super(ApkCutCommentsFormatCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected String getModuleFileName()
    {
        return ORDINARY_MODULE_FILE;
    }

    /**
     * In a module without the "Локализация" postfix: a non-canonical comment
     * similar to a cut service comment is reported as a wrong format, each
     * canonical "Локализация" comment is reported as forbidden. The balanced
     * "_Демо" pair is allowed and not reported.
     */
    @Test
    public void testFormatViolationsAndForbiddenComments() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(4, markers.size());
        assertTrue(markers.stream().allMatch(m -> m.getMessage() != null && !m.getMessage().isEmpty()));
    }

    /**
     * Comments similar to service comments in wording ("локализация
     * интерфейса ..."), service comment text inside a string literal and
     * ordinary comments produce no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }

    /**
     * In a common module with the "Локализация" postfix the module-level
     * pair check passes for the balanced "Локализация" comments, but an
     * unbalanced "_Демо" pair is reported once.
     */
    @Test
    public void testLocalizationModuleBalancedLocUnbalancedDemo() throws Exception
    {
        List<Marker> markers = getMarkers(LOCALIZATION_MODULE_FILE);
        assertEquals(1, markers.size());
    }

    /**
     * A common module with the "Локализация" postfix without any cut service
     * comments is reported once at the module level.
     */
    @Test
    public void testLocalizationModuleMissingComments() throws Exception
    {
        updateModuleFile(LOCALIZATION_MODULE_FILE, RESOURCE_POSTFIX_NO_COMMENTS);

        List<Marker> markers = getMarkers(LOCALIZATION_MODULE_FILE);
        assertEquals(1, markers.size());
    }

    private void updateModuleFile(String moduleFileName, String pathToResource) throws Exception
    {
        IProject project = getProject().getWorkspaceProject();
        IFile file = project.getFile(moduleFileName);
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
            throw new IllegalStateException("Cannot update file:" + file.toString(), e); //$NON-NLS-1$
        }
        waitForDD(getProject());
    }

    private List<Marker> getMarkers(String moduleFileName)
    {
        String moduleId = Path.ROOT.append(getTestConfigurationName()).append(moduleFileName).toString();
        List<Marker> markers = List.of(markerManager.getMarkers(getProject().getWorkspaceProject(), moduleId));

        String checkId = getCheckId();
        assertNotNull(checkId);

        return markers.stream()
            .filter(marker -> checkId.equals(getCheckIdFromMarker(marker, getProject())))
            .collect(Collectors.toList());
    }
}
