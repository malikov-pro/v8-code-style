/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Before;
import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormFactory;
import com._1c.g5.v8.dt.form.model.Table;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.v8codestyle.bsl.check.CurrentRowAccessCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class CurrentRowAccessCheckTest extends AbstractSingleModuleTestBase
{
    public CurrentRowAccessCheckTest()
    {
        super(CurrentRowAccessCheck.class);
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
    public void testFormFileBindingInvalidation() throws Exception
    {
        var formFile = getProject().getWorkspaceProject().getFile("/src/CommonForms/DynamicRows/Form.form"); //$NON-NLS-1$
        byte[] original;
        try (var input = formFile.getContents())
        {
            original = input.readAllBytes();
        }
        String text = new String(original, StandardCharsets.UTF_8);
        var file = getProject().getWorkspaceProject().getFile(getModuleFileName());
        long stamp = file.getModificationStamp();
        try
        {
            assertLines(List.of(3, 6));
            String changed = text.replace("<segments>List</segments>", "<segments>Rows</segments>"); //$NON-NLS-1$ //$NON-NLS-2$
            assertTrue(!changed.equals(text));
            formFile.setContents(new ByteArrayInputStream(changed.getBytes(StandardCharsets.UTF_8)),
                true, true, new org.eclipse.core.runtime.NullProgressMonitor());
            org.eclipse.core.resources.ResourcesPlugin.getWorkspace()
                .build(org.eclipse.core.resources.IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(getProject());
            assertLines(List.of());
            formFile.setContents(new ByteArrayInputStream(original), true, true,
                new org.eclipse.core.runtime.NullProgressMonitor());
            org.eclipse.core.resources.ResourcesPlugin.getWorkspace()
                .build(org.eclipse.core.resources.IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(getProject());
            assertLines(List.of(3, 6));
            assertEquals(stamp, file.getModificationStamp());
        }
        finally
        {
            formFile.setContents(new ByteArrayInputStream(original), true, true,
                new org.eclipse.core.runtime.NullProgressMonitor());
            org.eclipse.core.resources.ResourcesPlugin.getWorkspace()
                .build(org.eclipse.core.resources.IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(getProject());
        }
    }

    @Test
    public void testAttributeTypeOnlyInvalidation() throws Exception
    {
        var form = (Form)getTopObjectByFqn("CommonForm.DynamicRows.Form", getProject()); //$NON-NLS-1$
        var original = List.copyOf(form.getAttributes().get(0).getValueType().getTypes());
        var ordinary = List.copyOf(form.getAttributes().get(1).getValueType().getTypes());
        var file = getProject().getWorkspaceProject().getFile(getModuleFileName());
        long stamp = file.getModificationStamp();
        try
        {
            changeAttributeType(ordinary);
            assertLines(List.of());
            changeAttributeType(original);
            assertLines(List.of(3, 6));
            assertEquals(stamp, file.getModificationStamp());
        }
        finally
        {
            changeAttributeType(original);
        }
    }

    private void changeAttributeType(List<TypeItem> types)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Change attribute type") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var form = (Form)transaction.getTopObjectByFqn("CommonForm.DynamicRows.Form"); //$NON-NLS-1$
                var list = form.getAttributes().get(0);
                // Retain the existing type description and extInfo: exercise a nested-only change.
                list.getValueType().getTypes().clear();
                list.getValueType().getTypes().addAll(types);
                return null;
            }
        });
        waitForDD(getProject());
    }

    @Test
    public void testSourceCycleAndDefaults() throws Exception
    {
        var settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        assertTrue(settings.isEnabled());
        assertEquals(IssueSeverity.MAJOR, settings.getSeverity());
        assertEquals(0, settings.getParameters().size());
        assertLines(List.of(3, 6));
        updateModule(FOLDER_RESOURCE + "current-row-clean.bsl"); //$NON-NLS-1$
        assertLines(List.of());
        updateModule(FOLDER_RESOURCE + "current-row-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(3, 6));
    }

    @Before
    public void resetSource() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "current-row-bad.bsl"); //$NON-NLS-1$
    }

    @Test
    public void testBoundaries() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "current-row-boundaries.bsl"); //$NON-NLS-1$
        assertLines(List.of(3, 4, 5, 10, 12, 13, 15));
    }

    @Test
    public void testDataPathSegmentOnlyInvalidation() throws Exception
    {
        var file = getProject().getWorkspaceProject().getFile(getModuleFileName());
        long stamp = file.getModificationStamp();
        try
        {
            changeSegment("Rows"); //$NON-NLS-1$
            assertLines(List.of());
            changeSegment("List"); //$NON-NLS-1$
            assertLines(List.of(3, 6));
            assertEquals(stamp, file.getModificationStamp());
        }
        finally
        {
            changeSegment("List"); //$NON-NLS-1$
        }
    }

    private void changeSegment(String name)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Change binding segment") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var form = (Form)transaction.getTopObjectByFqn("CommonForm.DynamicRows.Form"); //$NON-NLS-1$
                var table = (Table)form.getItems().get(0);
                table.getDataPath().getSegments().set(0, name);
                return null;
            }
        });
        waitForDD(getProject());
    }

    @Test
    public void testBoundDynamicTableDereference() throws Exception
    {
        assertLines(List.of(3, 6));
    }

    @Test
    public void testDataPathOnlyInvalidation() throws Exception
    {
        assertLines(List.of(3, 6));
        var file = getProject().getWorkspaceProject().getFile(getModuleFileName());
        long stamp = file.getModificationStamp();
        try
        {
            changePath("Rows"); //$NON-NLS-1$
            assertLines(List.of());
            changePath("List"); //$NON-NLS-1$
            assertLines(List.of(3, 6));
            assertEquals(stamp, file.getModificationStamp());
        }
        finally
        {
            changePath("List"); //$NON-NLS-1$
        }
    }

    private void changePath(String attribute)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Change table binding") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var form = (Form)transaction.getTopObjectByFqn("CommonForm.DynamicRows.Form"); //$NON-NLS-1$
                var table = (Table)form.getItems().get(0);
                var path = FormFactory.eINSTANCE.createDataPath();
                path.getSegments().add(attribute);
                table.setDataPath(path);
                return null;
            }
        });
        waitForDD(getProject());
    }

    @Test
    public void testFormOnlyTableKindInvalidation() throws Exception
    {
        assertLines(List.of(3, 6));
        var file = getProject().getWorkspaceProject().getFile(getModuleFileName());
        long stamp = file.getModificationStamp();
        try
        {
            changeTable(false);
            assertLines(List.of());
            changeTable(true);
            assertLines(List.of(3, 6));
            assertEquals(stamp, file.getModificationStamp());
        }
        finally
        {
            changeTable(true);
        }
    }

    private void changeTable(boolean dynamic)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Change table kind") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var form = (Form)transaction.getTopObjectByFqn("CommonForm.DynamicRows.Form"); //$NON-NLS-1$
                var table = (Table)form.getItems().get(0);
                var path = FormFactory.eINSTANCE.createDataPath();
                path.getSegments().add(dynamic ? "List" : "Rows"); //$NON-NLS-1$ //$NON-NLS-2$
                table.setDataPath(path);
                table.setExtInfo(dynamic ? FormFactory.eINSTANCE.createDynamicListTableExtInfo() : null);
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
            String name = node.getRootNode().getText().substring(offset, offset + length);
            assertTrue(name, "CurrentRow".equalsIgnoreCase(name) //$NON-NLS-1$
                || "ТекущаяСтрока".equalsIgnoreCase(name)); //$NON-NLS-1$
        }
    }
}
