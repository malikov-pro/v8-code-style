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

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.MissedRequiredParameterCheck;

public class MissedRequiredParameterCrossModuleTest
    extends AbstractSingleModuleTestBase
{
    private static final String PROVIDER = "/src/CommonModules/BaseLibraryModule/Module.bsl"; //$NON-NLS-1$

    public MissedRequiredParameterCrossModuleTest()
    {
        super(MissedRequiredParameterCheck.class);
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
    public void testProviderOnlyOptionalParameterChange() throws Exception
    {
        var provider = getProject().getWorkspaceProject().getFile(PROVIDER);
        byte[] original;
        try (InputStream input = provider.getContents())
        {
            original = input.readAllBytes();
        }
        try
        {
            writeProvider("required-empty-provider.bsl"); //$NON-NLS-1$
            updateModule(FOLDER_RESOURCE + "required-empty-consumer.bsl"); //$NON-NLS-1$
            assertCount(1);
            var consumer = getProject().getWorkspaceProject().getFile(getModuleFileName());
            long stamp = consumer.getModificationStamp();
            String content;
            try (InputStream input = consumer.getContents())
            {
                content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            writeProvider("required-empty-provider-optional.bsl"); //$NON-NLS-1$
            assertCount(0);
            assertEquals(stamp, consumer.getModificationStamp());
            writeProvider("required-empty-provider.bsl"); //$NON-NLS-1$
            assertCount(1);
            // Both signatures have one required parameter; only its position changes.
            writeProvider("required-empty-provider-swapped.bsl"); //$NON-NLS-1$
            assertCount(0);
            writeProvider("required-empty-provider.bsl"); //$NON-NLS-1$
            assertCount(1);
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
        try (InputStream input = getClass().getResourceAsStream(FOLDER_RESOURCE + fixture))
        {
            getProject().getWorkspaceProject().getFile(PROVIDER)
                .setContents(input, true, true, new NullProgressMonitor());
        }
        buildAndWait();
    }

    private void buildAndWait() throws Exception
    {
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
        Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
        Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
        waitForDD(getProject());
    }

    private void assertCount(int count)
    {
        var node = NodeModelUtils.findActualNodeFor(getModule());
        for (var leaf : node.getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(count, getModuleMarkers().size());
        for (var marker : getModuleMarkers())
        {
            assertEquals(Integer.valueOf(2), marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE));
            Integer offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            Integer length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            assertEquals("Контракт(, 2)", node.getRootNode().getText().substring(offset, offset + length)); //$NON-NLS-1$
        }
    }
}
