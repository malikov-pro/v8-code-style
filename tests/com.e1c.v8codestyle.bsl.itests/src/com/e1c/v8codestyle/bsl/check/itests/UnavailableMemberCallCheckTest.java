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
import com.e1c.v8codestyle.bsl.check.UnavailableMemberCallCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class UnavailableMemberCallCheckTest extends AbstractSingleModuleTestBase
{
    public UnavailableMemberCallCheckTest()
    {
        super(UnavailableMemberCallCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "PlatformApiModern"; //$NON-NLS-1$
    }

    @Test
    public void testBoundCategoryBelowAtAndAboveBoundary() throws Exception
    {
        var settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        assertTrue(settings.isEnabled());
        assertEquals(IssueSeverity.MAJOR, settings.getSeverity());
        assertTrue(settings.getParameters().isEmpty());
        try
        {
            compatibility(CompatibilityMode.VERSION8_320);
            var module = updateAndGetModule(FOLDER_RESOURCE + "unavailable-error-category.bsl"); //$NON-NLS-1$
            for (var leaf : NodeModelUtils.findActualNodeFor(module).getLeafNodes())
            {
                assertNull(leaf.getSyntaxErrorMessage());
            }
            assertLines(List.of(3, 4));
            var file = getProject().getWorkspaceProject().getFile(getModuleFileName());
            long stamp = file.getModificationStamp();
            compatibility(CompatibilityMode.VERSION8_321);
            assertLines(List.of());
            compatibility(CompatibilityMode.VERSION8_323);
            assertLines(List.of());
            compatibility(CompatibilityMode.VERSION8_320);
            assertLines(List.of(3, 4));
            assertEquals(stamp, file.getModificationStamp());
        }
        finally
        {
            compatibility(CompatibilityMode.VERSION8_323);
        }
    }

    @Test
    public void testSourceFixAndModuleBodyUnion() throws Exception
    {
        try
        {
            compatibility(CompatibilityMode.VERSION8_320);
            String source = """
                Procedure Probe(Flag, Unknown) Export
                    If Flag Then
                        Value = ErrorCategory;
                    Else
                        Value = New Structure("InvalidPassword", True);
                    EndIf;
                    Result = Value.InvalidPassword;
                    Other = Unknown.InvalidPassword;
                EndProcedure

                Result = ErrorCategory.InvalidPassword;
                """; //$NON-NLS-1$
            writeSource(source);
            assertLines(List.of(7, 11));
            writeSource(source.replace("InvalidPassword", "AllErrors")); //$NON-NLS-1$ //$NON-NLS-2$
            assertLines(List.of());
            writeSource(source);
            assertLines(List.of(7, 11));
        }
        finally
        {
            compatibility(CompatibilityMode.VERSION8_323);
        }
    }

    @Test
    public void testVersionFamiliesAndNativeUnresolvedExclusions() throws Exception
    {
        try
        {
            String source = """
                Procedure Probe() Export
                    A = ErrorCategory.AllErrors;
                    B = ErrorCategory.DatabaseCopyError;
                    C = ErrorCategory.MultimediaToolsError;
                    D = ErrorCategory.DocumentConversionError;
                    E = ErrorCategory.ConfigurationError;
                    F = ErrorCategory.SpeechProcessingError;
                    G = ErrorCategory.DatabaseTablespaceError;
                    Unknown = ErrorCategory.NotAPlatformCategory;
                    Buffer = New BinaryDataBuffer(10);
                EndProcedure
                """; //$NON-NLS-1$
            compatibility(CompatibilityMode.VERSION8_316);
            writeSource(source);
            assertLines(List.of(2, 3, 4, 5, 6, 7, 8));
            compatibility(CompatibilityMode.VERSION8_317);
            assertLines(List.of(3, 4, 5, 6, 7, 8));
            compatibility(CompatibilityMode.VERSION8_318);
            assertLines(List.of(4, 5, 6, 7, 8));
            compatibility(CompatibilityMode.VERSION8_319);
            assertLines(List.of(5, 6, 7, 8));
            compatibility(CompatibilityMode.VERSION8_320);
            assertLines(List.of(6, 7, 8));
            compatibility(CompatibilityMode.VERSION8_321);
            assertLines(List.of(7, 8));
            compatibility(CompatibilityMode.VERSION8_323);
            assertLines(List.of());
        }
        finally
        {
            compatibility(CompatibilityMode.VERSION8_323);
        }
    }

    @Test
    public void testFileCompatibilityOnlyAndExplicitDisabled() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "unavailable-error-category.bsl"); //$NON-NLS-1$
        assertLines(List.of());
        var config = getProject().getWorkspaceProject().getFile("/src/Configuration/Configuration.mdo"); //$NON-NLS-1$
        byte[] original;
        try (var input = config.getContents())
        {
            original = input.readAllBytes();
        }
        String changed = new String(original, StandardCharsets.UTF_8).replace(
            "<compatibilityMode>8.3.23</compatibilityMode>", "<compatibilityMode>8.3.20</compatibilityMode>"); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(!changed.equals(new String(original, StandardCharsets.UTF_8)));
        var file = getProject().getWorkspaceProject().getFile(getModuleFileName());
        long stamp = file.getModificationStamp();
        byte[] before;
        try (var input = file.getContents())
        {
            before = input.readAllBytes();
        }
        try
        {
            writeConfig(changed.getBytes(StandardCharsets.UTF_8));
            assertEquals(CompatibilityMode.VERSION8_320,
                ((Configuration)getTopObjectByFqn("Configuration", getProject())).getCompatibilityMode()); //$NON-NLS-1$
            assertLines(List.of(3, 4));
            writeConfig(original);
            assertLines(List.of());
            writeConfig(changed.getBytes(StandardCharsets.UTF_8));
            assertLines(List.of(3, 4));
            setCheckEnable(false);
            assertLines(List.of());
            compatibility(CompatibilityMode.VERSION8_321);
            compatibility(CompatibilityMode.VERSION8_320);
            assertLines(List.of());
            assertEquals(stamp, file.getModificationStamp());
            try (var input = file.getContents())
            {
                assertTrue(java.util.Arrays.equals(before, input.readAllBytes()));
            }
        }
        finally
        {
            writeConfig(original);
            setCheckEnable(true);
        }
    }

    private void writeConfig(byte[] text) throws Exception
    {
        var file = getProject().getWorkspaceProject().getFile("/src/Configuration/Configuration.mdo"); //$NON-NLS-1$
        file.setContents(new ByteArrayInputStream(text), true, true, new NullProgressMonitor());
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
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

    private void compatibility(CompatibilityMode mode)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Change category target") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                ((Configuration)transaction.getTopObjectByFqn("Configuration")).setCompatibilityMode(mode); //$NON-NLS-1$
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
        assertEquals(expected, getModuleMarkers().stream()
            .map(marker -> (Integer)marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE)).sorted().toList());
        var text = NodeModelUtils.findActualNodeFor(getModule()).getRootNode().getText();
        for (var marker : getModuleMarkers())
        {
            int offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            int length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            String actual = text.substring(offset, offset + length);
            assertTrue(actual, List.of("InvalidPassword", "НеправильныйПароль", "AllErrors", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                "DatabaseCopyError", "MultimediaToolsError", "DocumentConversionError", "ConfigurationError", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                "SpeechProcessingError", "DatabaseTablespaceError").contains(actual)); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}
