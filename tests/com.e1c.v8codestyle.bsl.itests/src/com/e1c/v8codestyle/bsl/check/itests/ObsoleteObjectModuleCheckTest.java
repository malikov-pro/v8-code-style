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
 *     malikov-pro - port of the APK check АПК_01179
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
import com.e1c.v8codestyle.bsl.check.ObsoleteObjectModuleCheck;

/**
 * Tests for {@link ObsoleteObjectModuleCheck} — port of the APK check
 * АПК_01179 (standard 534: all modules of an obsolete metadata object
 * "Удалить"/"Delete"-prefixed must be empty).
 *
 * @author malikov-pro
 */
public class ObsoleteObjectModuleCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String PROJECT_NAME = "ObsoleteObjectModules"; //$NON-NLS-1$

    private static final String RU_MODULE_FILE_NAME = "/src/CommonModules/УдалитьМодуль/Module.bsl"; //$NON-NLS-1$
    private static final String EN_MODULE_FILE_NAME = "/src/CommonModules/DeleteGoodsSync/Module.bsl"; //$NON-NLS-1$
    private static final String PLAIN_MODULE_FILE_NAME = "/src/CommonModules/ОбычныйМодуль/Module.bsl"; //$NON-NLS-1$
    private static final String CATALOG_OBJECT_MODULE_FILE_NAME = "/src/Catalogs/DeleteGoods/ObjectModule.bsl"; //$NON-NLS-1$
    private static final String CATALOG_MANAGER_MODULE_FILE_NAME = "/src/Catalogs/DeleteGoods/ManagerModule.bsl"; //$NON-NLS-1$

    private static final String RESOURCE_WITH_CODE = FOLDER_RESOURCE + "apk-01179-module-with-code.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_COMMENTS_ONLY = FOLDER_RESOURCE + "apk-01179-module-comments-only.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public ObsoleteObjectModuleCheckTest()
    {
        super(ObsoleteObjectModuleCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * A module of the common module with the Russian obsolete prefix «Удалить»
     * containing code is reported once.
     */
    @Test
    public void testRussianObsoleteCommonModuleHasCode() throws Exception
    {
        updateModuleFile(RU_MODULE_FILE_NAME, RESOURCE_WITH_CODE);

        List<Marker> markers = getModuleMarkers(RU_MODULE_FILE_NAME);
        assertEquals(1, markers.size());
    }

    /**
     * A module of the common module with the English obsolete prefix "Delete"
     * containing code is reported once.
     */
    @Test
    public void testEnglishObsoleteCommonModuleHasCode() throws Exception
    {
        updateModuleFile(EN_MODULE_FILE_NAME, RESOURCE_WITH_CODE);

        List<Marker> markers = getModuleMarkers(EN_MODULE_FILE_NAME);
        assertEquals(1, markers.size());
    }

    /**
     * The object module of a catalog with the obsolete prefix containing code
     * is reported once.
     */
    @Test
    public void testObsoleteCatalogObjectModuleHasCode() throws Exception
    {
        updateModuleFile(CATALOG_OBJECT_MODULE_FILE_NAME, RESOURCE_WITH_CODE);

        List<Marker> markers = getModuleMarkers(CATALOG_OBJECT_MODULE_FILE_NAME);
        assertEquals(1, markers.size());
    }

    /**
     * The manager module of a catalog with the obsolete prefix containing code
     * is reported once.
     */
    @Test
    public void testObsoleteCatalogManagerModuleHasCode() throws Exception
    {
        updateModuleFile(CATALOG_MANAGER_MODULE_FILE_NAME, RESOURCE_WITH_CODE);

        List<Marker> markers = getModuleMarkers(CATALOG_MANAGER_MODULE_FILE_NAME);
        assertEquals(1, markers.size());
    }

    /**
     * A module of a regular (not obsolete) object is not reported.
     */
    @Test
    public void testRegularModuleIsNotChecked() throws Exception
    {
        updateModuleFile(PLAIN_MODULE_FILE_NAME, RESOURCE_WITH_CODE);

        List<Marker> markers = getModuleMarkers(PLAIN_MODULE_FILE_NAME);
        assertTrue(markers.isEmpty());
    }

    /**
     * A module of an obsolete object containing only comments (no executable
     * statements) is not reported: comments and blank lines are allowed.
     */
    @Test
    public void testCommentOnlyModuleIsEmpty() throws Exception
    {
        updateModuleFile(RU_MODULE_FILE_NAME, RESOURCE_COMMENTS_ONLY);

        List<Marker> markers = getModuleMarkers(RU_MODULE_FILE_NAME);
        assertTrue(markers.isEmpty());
    }

    private void updateModuleFile(String moduleFileName, String pathToResource) throws CoreException, IOException
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
