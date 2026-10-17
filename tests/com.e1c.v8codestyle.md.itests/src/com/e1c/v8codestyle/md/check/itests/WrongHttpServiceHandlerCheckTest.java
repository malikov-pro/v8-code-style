/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.metadata.mdclass.HTTPService;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.md.check.WrongHttpServiceHandlerCheck;
import com.e1c.v8codestyle.internal.md.CorePlugin;

public class WrongHttpServiceHandlerCheckTest extends SingleProjectReadOnlyCheckTestBase
{
    private com._1c.g5.v8.dt.core.platform.IDtProject project;

    @Override
    public void setUp() throws org.eclipse.core.runtime.CoreException
    {
        var resource = ResourcesPlugin.getWorkspace().getRoot().getProject(getTestConfigurationName());
        project = resource.exists() ? dtProjectManager.getDtProject(resource)
            : openProjectAndWaitForValidationFinish(getTestConfigurationName());
        waitForDD(project);
    }

    @Override
    public com._1c.g5.v8.dt.core.platform.IDtProject getProject()
    {
        return project;
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "ScheduledJobs"; //$NON-NLS-1$
    }

    @Test
    public void testDeclaredCountAndMethodKindBoundaries() throws Exception
    {
        var settings = checkRepository.getSettings(new CheckUid(WrongHttpServiceHandlerCheck.CHECK_ID,
            CorePlugin.PLUGIN_ID), getProject().getWorkspaceProject());
        assertTrue(settings.isEnabled());
        assertEquals(IssueSeverity.CRITICAL, settings.getSeverity());
        assertEquals(0, settings.getParameters().size());
        var file = getProject().getWorkspaceProject().getFile("/src/HTTPServices/HandlerProbe/Module.bsl"); //$NON-NLS-1$
        String original = readFile(file);
        try
        {
            for (String header : java.util.List.of("Function Handle()", //$NON-NLS-1$
                "Function Handle(Request, Extra = Undefined)")) //$NON-NLS-1$
            {
                writeFile(file, original.replace("Function Handle(Request)", header)); //$NON-NLS-1$
                assertCount(1);
            }
            writeFile(file, original.replace("Handle(Request)", "Handle(Request = Undefined)")); //$NON-NLS-1$ //$NON-NLS-2$
            assertCount(0);
            writeFile(file, "Procedure Handle(Request)\nEndProcedure\n"); //$NON-NLS-1$
            assertCount(0);
            writeFile(file, "Процедура Handle(Запрос) Экспорт\nКонецПроцедуры\n"); //$NON-NLS-1$
            assertCount(0);
            assign("hAnDlE"); //$NON-NLS-1$
            assertCount(0);
            assign("PredefinedJob"); //$NON-NLS-1$
            assertCount(1); // A method in the common module must not satisfy this service assignment.
            for (String empty : java.util.List.of("", " ")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                assign(empty);
                assertCount(0);
            }
            assign(null);
            assertCount(0);
        }
        finally
        {
            writeFile(file, original);
            assign("Handle"); //$NON-NLS-1$
        }
    }

    @Test
    public void testMetadataFileAssignmentOnlyCycle() throws Exception
    {
        var file = getProject().getWorkspaceProject().getFile("/src/HTTPServices/HandlerProbe/HandlerProbe.mdo"); //$NON-NLS-1$
        String original = readFile(file);
        var source = getProject().getWorkspaceProject().getFile("/src/HTTPServices/HandlerProbe/Module.bsl"); //$NON-NLS-1$
        long stamp = source.getModificationStamp();
        try
        {
            writeFile(file, original.replace("<handler>Handle</handler>", "<handler>Missing</handler>")); //$NON-NLS-1$ //$NON-NLS-2$
            assertCount(1);
            writeFile(file, original);
            assertCount(0);
            assertEquals(stamp, source.getModificationStamp());
        }
        finally
        {
            writeFile(file, original);
        }
    }

    @Test
    public void testMultipleAssignmentsHaveIndependentAnchors() throws Exception
    {
        var file = getProject().getWorkspaceProject().getFile("/src/HTTPServices/HandlerProbe/HandlerProbe.mdo"); //$NON-NLS-1$
        String original = readFile(file);
        String extra = "<methods uuid=\"035d8acb-c140-4e4a-bddd-d368e7062127\">" //$NON-NLS-1$
            + "<name>POST</name><httpMethod>POST</httpMethod><handler>Missing</handler></methods>"; //$NON-NLS-1$
        try
        {
            writeFile(file, original.replace("</urlTemplates>", extra + "</urlTemplates>")); //$NON-NLS-1$ //$NON-NLS-2$
            assign("Missing"); //$NON-NLS-1$
            assertCount(2);
            assign("Handle"); //$NON-NLS-1$
            assertCount(1);
        }
        finally
        {
            writeFile(file, original);
        }
    }

    private String readFile(org.eclipse.core.resources.IFile file) throws Exception
    {
        try (var input = file.getContents())
        {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void writeFile(org.eclipse.core.resources.IFile file, String text) throws Exception
    {
        file.setContents(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), true, true,
            new NullProgressMonitor());
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
        waitForDD(getProject());
    }

    @Test
    public void testMetadataAssignmentOnlyCycle() throws Exception
    {
        try
        {
            assign("Missing"); //$NON-NLS-1$
            assertCount(1);
            assign("Handle"); //$NON-NLS-1$
            assertCount(0);
        }
        finally
        {
            assign("Handle"); //$NON-NLS-1$
        }
    }

    @Test
    public void testSourceParameterCycle() throws Exception
    {
        var file = getProject().getWorkspaceProject().getFile("/src/HTTPServices/HandlerProbe/Module.bsl"); //$NON-NLS-1$
        byte[] original;
        try (var input = file.getContents())
        {
            original = input.readAllBytes();
        }
        String text = new String(original, StandardCharsets.UTF_8);
        var consumer = getProject().getWorkspaceProject().getFile("/src/HTTPServices/HandlerProbe/HandlerProbe.mdo"); //$NON-NLS-1$
        long stamp = consumer.getModificationStamp();
        try
        {
            file.setContents(new ByteArrayInputStream(text.replace("Handle(Request)", //$NON-NLS-1$
                "Handle(Request, Extra)").getBytes(StandardCharsets.UTF_8)), true, true, new NullProgressMonitor()); //$NON-NLS-1$
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(getProject());
            assertCount(1);
            file.setContents(new ByteArrayInputStream(original), true, true, new NullProgressMonitor());
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(getProject());
            assertCount(0);
            assertEquals(stamp, consumer.getModificationStamp());
        }
        finally
        {
            file.setContents(new ByteArrayInputStream(original), true, true, new NullProgressMonitor());
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(getProject());
        }
    }

    private void assign(String handler)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Change HTTP assignment") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var service = (HTTPService)transaction.getTopObjectByFqn("HTTPService.HandlerProbe"); //$NON-NLS-1$
                service.getUrlTemplates().get(0).getMethods().get(0).setHandler(handler);
                return null;
            }
        });
        waitForDD(getProject());
    }

    private void assertCount(int expected)
    {
        var owner = getTopObjectByFqn("HTTPService.HandlerProbe", getProject()); //$NON-NLS-1$
        var markers = getMarkersByCheckIds(Set.of(WrongHttpServiceHandlerCheck.CHECK_ID), null, getProject(),
            markerManager.getNestedMarkers(getProject().getWorkspaceProject(), owner.bmGetId()));
        assertEquals(expected, markers.size());
        var ids = new java.util.HashSet<Object>();
        for (var marker : markers)
        {
            assertEquals(MdClassPackage.METHOD__HANDLER, marker.getFeatureId());
            assertTrue(marker.getMessage(), !marker.getMessage().isBlank());
            assertTrue("Duplicate marker on the same HTTP assignment", ids.add(marker.getMarkerObjectId())); //$NON-NLS-1$
        }
    }
}
