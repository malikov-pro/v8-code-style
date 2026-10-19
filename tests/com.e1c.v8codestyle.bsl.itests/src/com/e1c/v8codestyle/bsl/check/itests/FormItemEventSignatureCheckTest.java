/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormCommandHandlerContainer;
import com._1c.g5.v8.dt.form.model.FormField;
import com._1c.g5.v8.dt.form.model.FormFactory;
import com._1c.g5.v8.dt.form.model.InputFieldExtInfo;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.EventHandlerInvalidSignatureCheck;

/** Product regressions for actual serialized item/extInfo/command bindings. */
public class FormItemEventSignatureCheckTest extends AbstractSingleModuleTestBase
{
    public FormItemEventSignatureCheckTest()
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
        return "/src/CommonForms/ItemContracts/Module.bsl"; //$NON-NLS-1$
    }

    @Override
    protected Module getModule()
    {
        return ((Form)getTopObjectByFqn("CommonForm.ItemContracts.Form", getProject())).getModule(); //$NON-NLS-1$
    }

    @Test
    public void testItemCommandAndExtInfoSourceCycles() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "form-item-event-clean.bsl"); //$NON-NLS-1$
        assertLines(List.of());
        updateModule(FOLDER_RESOURCE + "form-item-event-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(2, 7, 12));
        updateModule(FOLDER_RESOURCE + "form-item-event-clean.bsl"); //$NON-NLS-1$
        assertLines(List.of());
        updateModule(FOLDER_RESOURCE + "form-item-event-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(2, 7, 12));
    }

    @Test
    public void testBindingNamesOnlyCycles() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "form-item-event-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(2, 7, 12));
        var file = getProject().getWorkspaceProject().getFile(getModuleFileName());
        long stamp = file.getModificationStamp();
        byte[] source;
        try (var input = file.getContents())
        {
            source = input.readAllBytes();
        }
        try
        {
            bindNames(false);
            assertLines(List.of());
            bindNames(true);
            assertLines(List.of(2, 7, 12));
            assertEquals(stamp, file.getModificationStamp());
            try (var input = file.getContents())
            {
                assertTrue(java.util.Arrays.equals(source, input.readAllBytes()));
            }
        }
        finally
        {
            bindNames(true);
        }
    }

    @Test
    public void testDeclaredCountBoundariesAndUnboundLookalike() throws Exception
    {
        String clean = resourceText("form-item-event-clean.bsl"); //$NON-NLS-1$
        writeModule(clean);
        assertLines(List.of());
        writeModule(clean.replace("FieldChange(Item)", "FieldChange(Item, Extra = Undefined)") //$NON-NLS-1$ //$NON-NLS-2$
            .replace("FieldChoice(Item, ChoiceData, StandardProcessing)", //$NON-NLS-1$
                "FieldChoice(Item, A, B, C = Undefined)") //$NON-NLS-1$
            .replace("CommandAction(Command)", "CommandAction(Command, Extra = Undefined)")); //$NON-NLS-1$ //$NON-NLS-2$
        assertLines(List.of(2, 7, 12));
        // The platform's event-specific parameters may all be omitted; Item may not.
        writeModule(clean.replace("FieldChoice(Item, ChoiceData, StandardProcessing)", "FieldChoice(Item)")); //$NON-NLS-1$ //$NON-NLS-2$
        assertLines(List.of());
        writeModule(clean.replace("Procedure FieldChange(Item)", "Function FieldChange(Item)") //$NON-NLS-1$ //$NON-NLS-2$
            .replace("    Result = Item;\nEndProcedure", "    Return Item;\nEndFunction")); //$NON-NLS-1$ //$NON-NLS-2$
        assertLines(List.of());
        writeModule(clean + "\n&AtClient\nProcedure OnChange()\n    Result = 1;\nEndProcedure\n"); //$NON-NLS-1$
        assertLines(List.of());
    }

    @Test
    public void testCommandBindingAloneCycle() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "form-item-event-bad.bsl"); //$NON-NLS-1$
        long stamp = getProject().getWorkspaceProject().getFile(getModuleFileName()).getModificationStamp();
        try
        {
            changeCommandName("MissingCommand"); //$NON-NLS-1$
            assertLines(List.of(2, 7));
            changeCommandName("CommandAction"); //$NON-NLS-1$
            assertLines(List.of(2, 7, 12));
            assertEquals(stamp, getProject().getWorkspaceProject().getFile(getModuleFileName()).getModificationStamp());
        }
        finally
        {
            changeCommandName("CommandAction"); //$NON-NLS-1$
        }
    }

    private void changeCommandName(String name)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Rebind only command") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var form = (Form)transaction.getTopObjectByFqn("CommonForm.ItemContracts.Form"); //$NON-NLS-1$
                ((FormCommandHandlerContainer)form.getFormCommands().get(0).getAction()).getHandler().setName(name);
                return null;
            }
        });
        waitForDD(getProject());
    }

    @Test
    public void testExtInfoBindingAloneCycle() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "form-item-event-bad.bsl"); //$NON-NLS-1$
        long stamp = getProject().getWorkspaceProject().getFile(getModuleFileName()).getModificationStamp();
        try
        {
            changeChoiceName("MissingChoice"); //$NON-NLS-1$
            assertLines(List.of(2, 12));
            changeChoiceName("FieldChoice"); //$NON-NLS-1$
            assertLines(List.of(2, 7, 12));
            assertEquals(stamp, getProject().getWorkspaceProject().getFile(getModuleFileName()).getModificationStamp());
        }
        finally
        {
            changeChoiceName("FieldChoice"); //$NON-NLS-1$
        }
    }

    private void changeChoiceName(String name)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Rebind only extInfo") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var form = (Form)transaction.getTopObjectByFqn("CommonForm.ItemContracts.Form"); //$NON-NLS-1$
                var field = (FormField)form.getItems().get(0);
                ((InputFieldExtInfo)field.getExtInfo()).getHandlers().get(0).setName(name);
                return null;
            }
        });
        waitForDD(getProject());
    }

    @Test
    public void testCommandHandlerReplacementCycle() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "form-item-event-bad.bsl"); //$NON-NLS-1$
        long stamp = getProject().getWorkspaceProject().getFile(getModuleFileName()).getModificationStamp();
        try
        {
            replaceCommandHandler(false);
            assertLines(List.of(2, 7));
            replaceCommandHandler(true);
            assertLines(List.of(2, 7, 12));
            assertEquals(stamp, getProject().getWorkspaceProject().getFile(getModuleFileName()).getModificationStamp());
        }
        finally
        {
            replaceCommandHandler(true);
        }
    }

    private void replaceCommandHandler(boolean present)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Replace command action") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var form = (Form)transaction.getTopObjectByFqn("CommonForm.ItemContracts.Form"); //$NON-NLS-1$
                if (present)
                {
                    var container = FormFactory.eINSTANCE.createFormCommandHandlerContainer();
                    var handler = FormFactory.eINSTANCE.createCommandHandler();
                    handler.setName("CommandAction"); //$NON-NLS-1$
                    container.setHandler(handler);
                    form.getFormCommands().get(0).setAction(container);
                }
                else
                {
                    form.getFormCommands().get(0).setAction(null);
                }
                return null;
            }
        });
        waitForDD(getProject());
    }

    @Test
    public void testRepeatedSameEventDoesNotOverwriteEarlierBinding() throws Exception
    {
        writeModule(resourceText("form-item-event-clean.bsl").replace("FieldChange(Item)", "FieldChange()")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertLines(List.of(2));
        long stamp = getProject().getWorkspaceProject().getFile(getModuleFileName()).getModificationStamp();
        try
        {
            bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Add another OnChange binding") //$NON-NLS-1$
            {
                @Override
                public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
                {
                    var form = (Form)transaction.getTopObjectByFqn("CommonForm.ItemContracts.Form"); //$NON-NLS-1$
                    var field = EcoreUtil.copy((FormField)form.getItems().get(0));
                    field.setName("SecondInput"); //$NON-NLS-1$
                    field.setId(2);
                    field.getHandlers().get(0).setName("CommandAction"); //$NON-NLS-1$
                    ((InputFieldExtInfo)field.getExtInfo()).getHandlers().clear();
                    form.getItems().add(field);
                    return null;
                }
            });
            waitForDD(getProject());
            assertLines(List.of(2));
            assertEquals(stamp, getProject().getWorkspaceProject().getFile(getModuleFileName()).getModificationStamp());
        }
        finally
        {
            bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Remove added item") //$NON-NLS-1$
            {
                @Override
                public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
                {
                    var form = (Form)transaction.getTopObjectByFqn("CommonForm.ItemContracts.Form"); //$NON-NLS-1$
                    form.getItems().removeIf(item -> "SecondInput".equals(item.getName())); //$NON-NLS-1$
                    return null;
                }
            });
            waitForDD(getProject());
        }
    }

    private String resourceText(String name) throws Exception
    {
        try (var input = getClass().getResourceAsStream(FOLDER_RESOURCE + name))
        {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void writeModule(String source) throws Exception
    {
        var file = getProject().getWorkspaceProject().getFile(getModuleFileName());
        file.setContents(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), true, true,
            new NullProgressMonitor());
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
        waitForDD(getProject());
    }

    private void bindNames(boolean bound)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Rebind item and command") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var form = (Form)transaction.getTopObjectByFqn("CommonForm.ItemContracts.Form"); //$NON-NLS-1$
                var field = (FormField)form.getItems().get(0);
                field.getHandlers().get(0).setName(bound ? "FieldChange" : "MissingChange"); //$NON-NLS-1$ //$NON-NLS-2$
                ((InputFieldExtInfo)field.getExtInfo()).getHandlers().get(0)
                    .setName(bound ? "FieldChoice" : "MissingChoice"); //$NON-NLS-1$ //$NON-NLS-2$
                ((FormCommandHandlerContainer)form.getFormCommands().get(0).getAction()).getHandler()
                    .setName(bound ? "CommandAction" : "MissingCommand"); //$NON-NLS-1$ //$NON-NLS-2$
                return null;
            }
        });
        waitForDD(getProject());
    }

    @Test
    public void testBindingFileOnlyCycles() throws Exception
    {
        var formFile = getProject().getWorkspaceProject().getFile("/src/CommonForms/ItemContracts/Form.form"); //$NON-NLS-1$
        byte[] original;
        try (var input = formFile.getContents())
        {
            original = input.readAllBytes();
        }
        String text = new String(original, StandardCharsets.UTF_8);
        String changed = text.replace("<name>FieldChange</name>", "<name>MissingChange</name>") //$NON-NLS-1$ //$NON-NLS-2$
            .replace("<name>FieldChoice</name>", "<name>MissingChoice</name>") //$NON-NLS-1$ //$NON-NLS-2$
            .replace("<name>CommandAction</name>", "<name>MissingCommand</name>"); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(!text.equals(changed));
        updateModule(FOLDER_RESOURCE + "form-item-event-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(2, 7, 12));
        long stamp = getProject().getWorkspaceProject().getFile(getModuleFileName()).getModificationStamp();
        try
        {
            writeForm(changed.getBytes(StandardCharsets.UTF_8));
            assertLines(List.of());
            writeForm(original);
            assertLines(List.of(2, 7, 12));
            assertEquals(stamp, getProject().getWorkspaceProject().getFile(getModuleFileName()).getModificationStamp());
        }
        finally
        {
            writeForm(original);
        }
    }

    private void writeForm(byte[] text) throws Exception
    {
        var file = getProject().getWorkspaceProject().getFile("/src/CommonForms/ItemContracts/Form.form"); //$NON-NLS-1$
        file.setContents(new ByteArrayInputStream(text), true, true, new NullProgressMonitor());
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
        waitForDD(getProject());
    }

    private void assertLines(List<Integer> expected)
    {
        var node = NodeModelUtils.findActualNodeFor(getModule());
        for (var leaf : node.getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(expected, getModuleMarkers().stream()
            .map(marker -> (Integer)marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE)).sorted().toList());
        for (var marker : getModuleMarkers())
        {
            int offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            int length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            int line = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
            String name = switch (line)
            {
                case 2 -> "FieldChange"; //$NON-NLS-1$
                case 7 -> "FieldChoice"; //$NON-NLS-1$
                case 12 -> "CommandAction"; //$NON-NLS-1$
                default -> "unexpected"; //$NON-NLS-1$
            };
            assertEquals(name, node.getRootNode().getText().substring(offset, offset + length));
        }
    }
}
