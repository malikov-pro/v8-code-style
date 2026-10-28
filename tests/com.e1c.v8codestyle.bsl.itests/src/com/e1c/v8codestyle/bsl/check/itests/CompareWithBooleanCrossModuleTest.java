/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.CompareWithBooleanCheck;

/** Changes only the provider file after the initial consumer validation. */
public class CompareWithBooleanCrossModuleTest
    extends AbstractSingleModuleTestBase
{
    private static final String PROVIDER = "/src/CommonModules/BaseLibraryModule/Module.bsl"; //$NON-NLS-1$

    public CompareWithBooleanCrossModuleTest()
    {
        super(CompareWithBooleanCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "OverridableCommonModules"; //$NON-NLS-1$
    }

    @Override
    protected String getModuleFileName()
    {
        return "/src/CommonModules/OverridableModule/Module.bsl"; //$NON-NLS-1$
    }

    @Override
    protected Module getModule()
    {
        return ((CommonModule)getTopObjectByFqn("CommonModule.OverridableModule", getProject())).getModule(); //$NON-NLS-1$
    }

    @Test
    public void testProviderOnlyReturnTypeInvalidation() throws Exception
    {
        var provider = getProject().getWorkspaceProject().getFile(PROVIDER);
        byte[] original;
        try (InputStream input = provider.getContents())
        {
            original = input.readAllBytes();
        }
        try
        {
            writeProvider(FOLDER_RESOURCE + "boolean-cross-provider.bsl"); //$NON-NLS-1$
            updateModule(FOLDER_RESOURCE + "boolean-cross-consumer.bsl"); //$NON-NLS-1$
            assertLines(List.of(2));
            var consumer = getProject().getWorkspaceProject().getFile(getModuleFileName());
            long stamp = consumer.getModificationStamp();
            String content;
            try (InputStream input = consumer.getContents())
            {
                content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            writeProvider(FOLDER_RESOURCE + "boolean-cross-provider-union.bsl"); //$NON-NLS-1$
            assertLines(List.of());
            assertEquals(stamp, consumer.getModificationStamp());
            writeProvider(FOLDER_RESOURCE + "boolean-cross-provider.bsl"); //$NON-NLS-1$
            assertLines(List.of(2));
            assertEquals(stamp, consumer.getModificationStamp());
            try (InputStream input = consumer.getContents())
            {
                assertEquals(content, new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        finally
        {
            provider.setContents(new ByteArrayInputStream(original), true, true, new NullProgressMonitor());
            buildAndWait();
        }
    }

    private void writeProvider(String fixture) throws Exception
    {
        try (InputStream input = getClass().getResourceAsStream(fixture))
        {
            getProject().getWorkspaceProject().getFile(PROVIDER)
                .setContents(input, true, true, new NullProgressMonitor());
        }
        buildAndWait();
    }

    private void buildAndWait() throws Exception
    {
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
        getProject().getWorkspaceProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
        Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
        waitForDD(getProject());
    }

    private void assertLines(List<Integer> expected)
    {
        for (var leaf : NodeModelUtils.findActualNodeFor(getModule()).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(expected, getModuleMarkers().stream().map(marker -> {
            Integer line = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
            return line;
        }).sorted().toList());
    }
}
