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
 *     malikov-pro - port of the APK check 00150 (register self-sufficiency)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
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
import com.e1c.v8codestyle.bsl.check.ApkRegistrarAccessCheck;

/**
 * Tests for {@link ApkRegistrarAccessCheck} — port of the APK check АПК_00150
 * (standard 477: a register must be logically independent from its recorders,
 * the register record set module must not dereference the Recorder attribute).
 *
 * @author malikov-pro
 */
public class ApkRegistrarAccessCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String PROJECT_NAME = "RegisterRecorderAccess"; //$NON-NLS-1$

    private static final String RECORD_SET_MODULE_FILE_NAME =
        "/src/AccumulationRegisters/Stocks/RecordSetModule.bsl"; //$NON-NLS-1$

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "apk-00150-registrar-access.bsl"; //$NON-NLS-1$

    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-00150-registrar-access-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public ApkRegistrarAccessCheckTest()
    {
        super(ApkRegistrarAccessCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * Each dereferencing of the Recorder attribute is reported: in code
     * and in a query text inside a string literal. Allowed accesses
     * (Recorder.Metadata()) are not reported.
     */
    @Test
    public void testRecorderAccessIsReported() throws Exception
    {
        updateModuleFile(RECORD_SET_MODULE_FILE_NAME, RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers(RECORD_SET_MODULE_FILE_NAME);
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 2, markers.size()); //$NON-NLS-1$
        assertTrue(markers.stream().allMatch(m -> m.getMessage() != null && !m.getMessage().isEmpty()));
    }

    /**
     * Allowed accesses from the APK algorithm list (Filter.Recorder,
     * StandardAttributes.Recorder, Recorder.ValueType, Recorder.Metadata(),
     * Recorder.GetObject(), Recorder.MomentOfTime()) are not reported.
     */
    @Test
    public void testAllowedAccessesAreNotReported() throws Exception
    {
        updateModuleFile(RECORD_SET_MODULE_FILE_NAME, RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers(RECORD_SET_MODULE_FILE_NAME);
        assertTrue(markers.isEmpty());
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

        // As well as AUTO_BUILD-family job is being scheduled synchronously
        // So all we need is to wait for auto-build job is being finished
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

    private List<Marker> getModuleMarkers(String moduleFileName)
    {
        String moduleId = Path.ROOT.append(getTestConfigurationName()).append(moduleFileName).toString();
        List<Marker> markers = List.of(markerManager.getMarkers(getProject().getWorkspaceProject(), moduleId));

        String checkId = getCheckId();
        return markers.stream()
            .filter(m -> checkId.equals(getCheckIdFromMarker(m, getProject())))
            .collect(Collectors.toList());
    }
}
