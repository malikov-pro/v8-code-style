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
import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.metadata.mdclass.CompatibilityMode;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.v8codestyle.bsl.check.DeprecatedMethodCallCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class DeprecatedMethodCallCheckTest extends AbstractSingleModuleTestBase
{
    public DeprecatedMethodCallCheckTest()
    {
        super(DeprecatedMethodCallCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "PlatformApiModern"; //$NON-NLS-1$
    }

    @Test
    public void testBoundPropertiesDoNotDuplicateNativeMethodsAndConstructors() throws Exception
    {
        var settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        assertTrue(settings.isEnabled());
        assertEquals(IssueSeverity.MINOR, settings.getSeverity());
        assertTrue(settings.getParameters().isEmpty());
        var module = updateAndGetModule(FOLDER_RESOURCE + "deprecated-platform-property.bsl"); //$NON-NLS-1$
        var node = NodeModelUtils.findActualNodeFor(module);
        for (var leaf : node.getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(List.of(3, 5, 6), getModuleMarkers().stream()
            .map(m -> (Integer)m.getExtraInfo().get(StandardExtraInfo.TEXT_LINE)).sorted().toList());
        for (var marker : getModuleMarkers())
        {
            int offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            int length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            String text = node.getRootNode().getText().substring(offset, offset + length);
            assertTrue(text, List.of("PrintParametersName", "ИмяПараметровПечати").contains(text)); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    @Test
    public void testSourceReplacementCycles() throws Exception
    {
        String bad;
        try (var input = getClass().getResourceAsStream(FOLDER_RESOURCE + "deprecated-platform-property.bsl")) //$NON-NLS-1$
        {
            bad = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        String clean = bad.replace("PrintParametersName", "PrintParametersKey") //$NON-NLS-1$ //$NON-NLS-2$
            .replace("ИмяПараметровПечати", "КлючПараметровПечати"); //$NON-NLS-1$ //$NON-NLS-2$
        writeSource(bad);
        assertLines(List.of(3, 5, 6));
        writeSource(clean);
        assertLines(List.of());
        writeSource(bad);
        assertLines(List.of(3, 5, 6));
    }

    @Test
    public void testCompatibilityOnlyCycleWithUnchangedConsumer() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "deprecated-platform-property.bsl"); //$NON-NLS-1$
        assertLines(List.of(3, 5, 6));
        var file = getProject().getWorkspaceProject().getFile(getModuleFileName());
        long stamp = file.getModificationStamp();
        byte[] before;
        try (var input = file.getContents())
        {
            before = input.readAllBytes();
        }
        try
        {
            setCompatibility(CompatibilityMode.VERSION8_1);
            assertLines(List.of());
            setCompatibility(CompatibilityMode.VERSION8_213);
            assertLines(List.of(3, 5, 6));
            setCompatibility(CompatibilityMode.VERSION8_323);
            assertLines(List.of(3, 5, 6));
            assertEquals(stamp, file.getModificationStamp());
            try (var input = file.getContents())
            {
                assertTrue(java.util.Arrays.equals(before, input.readAllBytes()));
            }
        }
        finally
        {
            setCompatibility(CompatibilityMode.VERSION8_323);
        }
    }

    @Test
    public void testModuleBodyAndMultipleUnionOwners() throws Exception
    {
        String source = """
            Procedure Probe(Flag) Export
                If Flag Then
                    Receiver = New SpreadsheetDocument();
                Else
                    Receiver = New Structure("PrintParametersName", "custom");
                EndIf;
                Result = Receiver.PrintParametersName;
            EndProcedure

            Sheet = New SpreadsheetDocument();
            OldName = Sheet.PrintParametersName;
            """; //$NON-NLS-1$
        writeSource(source);
        assertLines(List.of(7, 11));
        writeSource(source.replace("PrintParametersName", "PrintParametersKey")); //$NON-NLS-1$ //$NON-NLS-2$
        assertLines(List.of());
        writeSource(source);
        assertLines(List.of(7, 11));
    }

    @Test
    public void testFileCompatibilityOnlyCycle() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "deprecated-platform-property.bsl"); //$NON-NLS-1$
        var file = getProject().getWorkspaceProject().getFile("/src/Configuration/Configuration.mdo"); //$NON-NLS-1$
        byte[] original;
        try (var input = file.getContents())
        {
            original = input.readAllBytes();
        }
        String text = new String(original, StandardCharsets.UTF_8);
        String changed = text.replace("<compatibilityMode>8.3.23</compatibilityMode>", //$NON-NLS-1$
            "<compatibilityMode>8.1.0</compatibilityMode>"); //$NON-NLS-1$
        assertTrue(!text.equals(changed));
        long stamp = getProject().getWorkspaceProject().getFile(getModuleFileName()).getModificationStamp();
        try
        {
            file.setContents(new ByteArrayInputStream(changed.getBytes(StandardCharsets.UTF_8)), true, true,
                new NullProgressMonitor());
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(getProject());
            assertEquals(CompatibilityMode.VERSION8_1,
                ((Configuration)getTopObjectByFqn("Configuration", getProject())).getCompatibilityMode()); //$NON-NLS-1$
            assertLines(List.of());
            file.setContents(new ByteArrayInputStream(original), true, true, new NullProgressMonitor());
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(getProject());
            assertLines(List.of(3, 5, 6));
            assertEquals(stamp, getProject().getWorkspaceProject().getFile(getModuleFileName()).getModificationStamp());
        }
        finally
        {
            file.setContents(new ByteArrayInputStream(original), true, true, new NullProgressMonitor());
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
            waitForDD(getProject());
        }
    }

    @Test
    public void testExplicitDisabledCheckCycle() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "deprecated-platform-property.bsl"); //$NON-NLS-1$
        assertLines(List.of(3, 5, 6));
        try
        {
            setCheckEnable(false);
            assertLines(List.of());
            setCheckEnable(true);
            assertLines(List.of(3, 5, 6));
        }
        finally
        {
            setCheckEnable(true);
        }
    }

    private void setCompatibility(CompatibilityMode mode)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Change compatibility only") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var configuration = (Configuration)transaction.getTopObjectByFqn("Configuration"); //$NON-NLS-1$
                configuration.setCompatibilityMode(mode);
                return null;
            }
        });
        waitForDD(getProject());
    }

    private void writeSource(String source) throws Exception
    {
        var file = getProject().getWorkspaceProject().getFile(getModuleFileName());
        file.setContents(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), true, true,
            new NullProgressMonitor());
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
        waitForDD(getProject());
    }

    private void assertLines(List<Integer> expected)
    {
        for (var leaf : NodeModelUtils.findActualNodeFor(getModule()).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(expected, getModuleMarkers().stream()
            .map(m -> (Integer)m.getExtraInfo().get(StandardExtraInfo.TEXT_LINE)).sorted().toList());
    }
}
