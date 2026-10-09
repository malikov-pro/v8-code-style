/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: malikov-pro
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the upstream issue 1C-Company/v8-code-style#1354
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.ObjectModuleParameterMatchesAttributeCheck;

/**
 * Tests for {@link ObjectModuleParameterMatchesAttributeCheck} — a parameter
 * of an object module method shall not be named like an attribute or a
 * tabular section of the same metadata object.
 *
 * @author malikov-pro
 */
public class ObjectModuleParameterMatchesAttributeCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String PROJECT_NAME = "ParamMatchesAttribute"; //$NON-NLS-1$

    private static final String OBJECT_MODULE_FILE_NAME = "/src/Catalogs/Goods/ObjectModule.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public ObjectModuleParameterMatchesAttributeCheckTest()
    {
        super(ObjectModuleParameterMatchesAttributeCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * Parameters named like the attribute "Barcode" and like the tabular
     * section "Remains" (case-insensitive) are reported — one issue per
     * parameter.
     */
    @Test
    public void testParameterMatchesAttributeOrTabularSection() throws Exception
    {
        updateModuleFile(FOLDER_RESOURCE + "up-1354-param-matches-attribute.bsl"); //$NON-NLS-1$

        List<Marker> markers = getObjectModuleMarkers();
        assertEquals(2, markers.size());
    }

    /**
     * Parameters named like a standard attribute ("Code"), like an attribute
     * of a tabular section ("Quantity") or matching nothing are not reported.
     */
    @Test
    public void testStandardAndNestedAttributesAreClean() throws Exception
    {
        updateModuleFile(FOLDER_RESOURCE + "up-1354-param-matches-attribute-clean.bsl"); //$NON-NLS-1$

        List<Marker> markers = getObjectModuleMarkers();
        assertTrue(markers.isEmpty());
    }

    private void updateModuleFile(String pathToResource) throws CoreException, IOException
    {
        IProject project = getProject().getWorkspaceProject();
        IFile file = project.getFile(OBJECT_MODULE_FILE_NAME);
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

    private List<Marker> getObjectModuleMarkers()
    {
        String moduleId = Path.ROOT.append(getTestConfigurationName()).append(OBJECT_MODULE_FILE_NAME).toString();
        List<Marker> markers = List.of(markerManager.getMarkers(getProject().getWorkspaceProject(), moduleId));

        String checkId = getCheckId();
        return markers.stream()
            .filter(m -> checkId.equals(getCheckIdFromMarker(m, getProject())))
            .collect(Collectors.toList());
    }
}
