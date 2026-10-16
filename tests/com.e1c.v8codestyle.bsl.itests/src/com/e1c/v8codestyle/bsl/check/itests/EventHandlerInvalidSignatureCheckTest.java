/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.mcore.Event;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.v8codestyle.bsl.check.EventHandlerInvalidSignatureCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class EventHandlerInvalidSignatureCheckTest extends AbstractSingleModuleTestBase
{
    public EventHandlerInvalidSignatureCheckTest()
    {
        super(EventHandlerInvalidSignatureCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "SelfReferenceCheck"; //$NON-NLS-1$
    }

    @Override
    protected String getModuleFileName()
    {
        return "/src/CommonForms/DynamicRows/Module.bsl"; //$NON-NLS-1$
    }

    @Override
    protected Module getModule()
    {
        return ((Form)getTopObjectByFqn("CommonForm.DynamicRows.Form", getProject())).getModule(); //$NON-NLS-1$
    }

    @Test
    public void testOptionalCountsMultipleBindingsAndDefaults() throws Exception
    {
        var settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        assertTrue(settings.isEnabled());
        assertEquals(IssueSeverity.MAJOR, settings.getSeverity());
        assertEquals(0, settings.getParameters().size());
        try
        {
            bindNames("OpenZero", "CloseFour"); //$NON-NLS-1$ //$NON-NLS-2$
            updateModule(FOLDER_RESOURCE + "event-signature-boundaries.bsl"); //$NON-NLS-1$
            assertLines(List.of());
            bindNames("OpenOne", "CloseFour"); //$NON-NLS-1$ //$NON-NLS-2$
            assertLines(List.of());
            bindNames("OpenTwo", "CloseFour"); //$NON-NLS-1$ //$NON-NLS-2$
            // Formal defaults do not change the declared parameter count of the handler.
            assertLines(List.of(8));
            bindNames("OpenTwo", "CloseFive"); //$NON-NLS-1$ //$NON-NLS-2$
            assertLines(List.of(8, 14));
            // A single method assigned to two different events must satisfy both contracts.
            bindNames("OpenTwo", "OpenTwo"); //$NON-NLS-1$ //$NON-NLS-2$
            assertLines(List.of(8));
        }
        finally
        {
            bindNames("InitialOpen", "InitialClose"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private void bindNames(String open, String close)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Bind test events") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var form = (Form)transaction.getTopObjectByFqn("CommonForm.DynamicRows.Form"); //$NON-NLS-1$
                form.getHandlers().get(0).setName(open);
                form.getHandlers().get(1).setName(close);
                return null;
            }
        });
        waitForDD(getProject());
    }

    @Test
    public void testEventReferenceOnlyCycle() throws Exception
    {
        var form = (Form)getTopObjectByFqn("CommonForm.DynamicRows.Form", getProject()); //$NON-NLS-1$
        var open = form.getHandlers().get(0).getEvent();
        var close = form.getHandlers().get(1).getEvent();
        setBinding(true);
        try
        {
            updateModule(FOLDER_RESOURCE + "event-signature-bad.bsl"); //$NON-NLS-1$
            assertLines(List.of(2));
            var file = getProject().getWorkspaceProject().getFile(getModuleFileName());
            long stamp = file.getModificationStamp();
            changeEvent(close);
            assertLines(List.of());
            changeEvent(open);
            assertLines(List.of(2));
            assertEquals(stamp, file.getModificationStamp());
        }
        finally
        {
            changeEvent(open);
            setBinding(false);
        }
    }

    private void changeEvent(Event event)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Change bound event") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var form = (Form)transaction.getTopObjectByFqn("CommonForm.DynamicRows.Form"); //$NON-NLS-1$
                form.getHandlers().get(0).setEvent(event);
                return null;
            }
        });
        waitForDD(getProject());
    }

    @Test
    public void testFormFileBindingOnlyCycle() throws Exception
    {
        var formFile = getProject().getWorkspaceProject().getFile("/src/CommonForms/DynamicRows/Form.form"); //$NON-NLS-1$
        byte[] original;
        try (var input = formFile.getContents())
        {
            original = input.readAllBytes();
        }
        String text = new String(original, StandardCharsets.UTF_8);
        try
        {
            writeForm(formFile, text.replace("<name>InitialOpen</name>", "<name>OpenHandler</name>")); //$NON-NLS-1$ //$NON-NLS-2$
            updateModule(FOLDER_RESOURCE + "event-signature-bad.bsl"); //$NON-NLS-1$
            assertLines(List.of(2));
            var file = getProject().getWorkspaceProject().getFile(getModuleFileName());
            long stamp = file.getModificationStamp();
            writeForm(formFile, text);
            assertLines(List.of());
            writeForm(formFile, text.replace("<name>InitialOpen</name>", "<name>OpenHandler</name>")); //$NON-NLS-1$ //$NON-NLS-2$
            assertLines(List.of(2));
            assertEquals(stamp, file.getModificationStamp());
        }
        finally
        {
            writeForm(formFile, text);
        }
    }

    private void writeForm(IFile file, String text) throws Exception
    {
        file.setContents(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), true, true,
            new NullProgressMonitor());
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
        waitForDD(getProject());
    }

    @Test
    public void testBindingOnlyCycle() throws Exception
    {
        setBinding(true);
        try
        {
            updateModule(FOLDER_RESOURCE + "event-signature-bad.bsl"); //$NON-NLS-1$
            assertLines(List.of(2));
            var file = getProject().getWorkspaceProject().getFile(getModuleFileName());
            long stamp = file.getModificationStamp();
            setBinding(false);
            assertLines(List.of());
            setBinding(true);
            assertLines(List.of(2));
            assertEquals(stamp, file.getModificationStamp());
        }
        finally
        {
            setBinding(false);
        }
    }

    @Test
    public void testBoundFormEventSourceCycle() throws Exception
    {
        setBinding(true);
        try
        {
            updateModule(FOLDER_RESOURCE + "event-signature-bad.bsl"); //$NON-NLS-1$
            assertLines(List.of(2));
            updateModule(FOLDER_RESOURCE + "event-signature-clean.bsl"); //$NON-NLS-1$
            assertLines(List.of());
            updateModule(FOLDER_RESOURCE + "event-signature-bad.bsl"); //$NON-NLS-1$
            assertLines(List.of(2));
        }
        finally
        {
            setBinding(false);
        }
    }

    private void setBinding(boolean enabled)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Bind form test event") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var form = (Form)transaction.getTopObjectByFqn("CommonForm.DynamicRows.Form"); //$NON-NLS-1$
                // The form resource parser resolves the serialized OnOpen reference to a platform Event.
                form.getHandlers().get(0).setName(enabled ? "OpenHandler" : "InitialOpen"); //$NON-NLS-1$ //$NON-NLS-2$
                return null;
            }
        });
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
        var node = NodeModelUtils.findActualNodeFor(getModule());
        for (var marker : getModuleMarkers())
        {
            Integer offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            Integer length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            String actual = node.getRootNode().getText().substring(offset, offset + length);
            assertTrue(actual, List.of("OpenHandler", "OpenTwo", "CloseFive").contains(actual)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }
}
