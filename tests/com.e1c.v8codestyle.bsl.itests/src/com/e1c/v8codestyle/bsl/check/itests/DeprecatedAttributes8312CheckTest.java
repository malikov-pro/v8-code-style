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
import com.e1c.v8codestyle.bsl.check.DeprecatedAttributes8312Check;
import com.e1c.v8codestyle.bsl.check.DeprecatedMethodCallCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class DeprecatedAttributes8312CheckTest extends AbstractSingleModuleTestBase
{
    public DeprecatedAttributes8312CheckTest()
    {
        super(DeprecatedAttributes8312Check.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "PlatformApiModern"; //$NON-NLS-1$
    }

    @Test
    public void testResidualLexicalContractAndNativeEnumExclusions() throws Exception
    {
        var settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        assertTrue(settings.isEnabled());
        assertEquals(IssueSeverity.TRIVIAL, settings.getSeverity());
        assertTrue(settings.getParameters().isEmpty());
        var module = updateAndGetModule(FOLDER_RESOURCE + "deprecated-8312-residual.bsl"); //$NON-NLS-1$
        var node = NodeModelUtils.findActualNodeFor(module);
        for (var leaf : node.getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(List.of(3, 5, 9, 12), getModuleMarkers().stream()
            .map(marker -> (Integer)marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE)).sorted().toList());
        for (var marker : getModuleMarkers())
        {
            int offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            int length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            int line = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
            String expected = line == 3 ? ".ОтображатьЛегенду" : line == 12 ? ".ОтображатьШкалу" : ".ShowLegend"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals(expected, node.getRootNode().getText().substring(offset, offset + length));
        }
    }

    @Test
    public void testSourceFixAndCompatibilityOnlyCycles() throws Exception
    {
        String bad = resourceText();
        String clean = bad.replace("ОтображатьЛегенду", "ОбластьЛегенды") //$NON-NLS-1$ //$NON-NLS-2$
            .replace("ShowLegend", "LegendArea").replace("ОтображатьШкалу", "ОтображатьШкалы"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        writeSource(bad);
        assertLines(List.of(3, 5, 9, 12));
        writeSource(clean);
        assertLines(List.of());
        writeSource(bad);
        assertLines(List.of(3, 5, 9, 12));
        var file = getProject().getWorkspaceProject().getFile(getModuleFileName());
        long stamp = file.getModificationStamp();
        try
        {
            compatibility(CompatibilityMode.VERSION8_311);
            assertLines(List.of());
            compatibility(CompatibilityMode.VERSION8_312);
            assertLines(List.of(3, 5, 9, 12));
            compatibility(CompatibilityMode.VERSION8_323);
            assertLines(List.of(3, 5, 9, 12));
            assertEquals(stamp, file.getModificationStamp());
            try (var input = file.getContents())
            {
                assertEquals(bad, new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        finally
        {
            compatibility(CompatibilityMode.VERSION8_323);
        }
    }

    @Test
    public void testNoOverlapWithResolvedPropertyCheck() throws Exception
    {
        var uid = new CheckUid(DeprecatedMethodCallCheck.CHECK_ID, BslPlugin.PLUGIN_ID);
        var settings = checkRepository.getSettings(uid, getProject().getWorkspaceProject());
        boolean original = settings.isEnabled();
        settings.setEnabled(true);
        checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
        waitForDD(getProject());
        try
        {
            updateModule(FOLDER_RESOURCE + "deprecated-8312-residual.bsl"); //$NON-NLS-1$
            assertLines(List.of(3, 5, 9, 12));
            var markers = markerManager.getMarkers(getProject().getWorkspaceProject(), getModuleId());
            assertEquals(0, java.util.Arrays.stream(markers).filter(marker -> uid.equals(
                checkRepository.getUidForShortUid(marker.getCheckId(), getProject().getWorkspaceProject()))).count());
        }
        finally
        {
            settings = checkRepository.getSettings(uid, getProject().getWorkspaceProject());
            settings.setEnabled(original);
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
        }
    }

    @Test
    public void testAllChartRootsAndPlotSpellings() throws Exception
    {
        String source = """
            Procedure Probe(Chart, GanttChart, PivotChart, ChartPlotArea) Export
                Chart.ShowTitle = True;
                GanttChart.GradientPaletteMaxColors = 10;
                PivotChart.ShowLegend = True;
                ChartPlotArea.ShowScale = True;
                ChartPlotArea.ShowSeriesScaleLabels = True;
                ChartPlotArea.ShowPointsScaleLabels = True;
                ChartPlotArea.ShowValuesScaleLabels = True;
                ChartPlotArea.ShowScaleValueLines = True;
                ChartPlotArea.ValueScaleFormat = True;
                ChartPlotArea.LabelsOrientation = True;
                ChartPlotArea.ScaleLines = True;
                ChartPlotArea.ShowScales = True;
                Text = "Chart.ShowLegend";
                // Chart.ShowLegend = True;
                Chart.ShowLegend();
            EndProcedure
            """; //$NON-NLS-1$
        writeSource(source);
        assertLines(List.of(2, 3, 4, 5, 6, 7, 8, 9, 10, 11));
    }

    @Test
    public void testPinnedChainIndexCallAndHiddenTokenBoundaries() throws Exception
    {
        String source = """
            Procedure Probe(Chart, ChartPlotArea, Alias) Export
                Chart.ShowLegend = True;
                Chart.Child.ShowTitle = True;
                Chart[0].ShowLegend = True;
                Chart().ShowLegend = True;
                Chart.ShowLegend();
                Alias.ShowLegend = True;
                Text = "Chart.ShowLegend";
                // Chart.ShowTitle = True;
                ChartPlotArea.ShowScale = True;
                ChartPlotArea.ScaleLines = True;
                ОбластьПостроенияДиаграммы.ЛинииШкалы = Истина;
                ChartPlotArea . ShowScale = True;
                ChartPlotArea
                    .ShowSeriesScaleLabels = True;
                ChartPlotArea.ShowScales = True;
                ChartPlotArea // before dot
                    .ShowScale = True;
            EndProcedure
            """; //$NON-NLS-1$
        writeSource(source);
        assertLines(List.of(2, 3, 4, 5, 10, 12, 13, 15, 18));
        var node = NodeModelUtils.findActualNodeFor(getModule()).getRootNode();
        for (var marker : getModuleMarkers())
        {
            int line = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
            int offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            int length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            String text = node.getText().substring(offset, offset + length);
            String expected = switch (line)
            {
                case 2, 4, 5 -> ".ShowLegend"; //$NON-NLS-1$
                case 3 -> ".ShowTitle"; //$NON-NLS-1$
                case 12 -> ".ЛинииШкалы"; //$NON-NLS-1$
                case 13 -> ". ShowScale"; //$NON-NLS-1$
                case 15 -> ".ShowSeriesScaleLabels"; //$NON-NLS-1$
                default -> ".ShowScale"; //$NON-NLS-1$
            };
            assertEquals(expected, text);
        }
    }

    @Test
    public void testFileCompatibilityOnlyCycle() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "deprecated-8312-residual.bsl"); //$NON-NLS-1$
        assertLines(List.of(3, 5, 9, 12));
        var configFile = getProject().getWorkspaceProject().getFile("/src/Configuration/Configuration.mdo"); //$NON-NLS-1$
        byte[] original;
        try (var input = configFile.getContents())
        {
            original = input.readAllBytes();
        }
        String changed = new String(original, StandardCharsets.UTF_8).replace(
            "<compatibilityMode>8.3.23</compatibilityMode>", "<compatibilityMode>8.3.11</compatibilityMode>"); //$NON-NLS-1$ //$NON-NLS-2$
        var moduleFile = getProject().getWorkspaceProject().getFile(getModuleFileName());
        long stamp = moduleFile.getModificationStamp();
        byte[] source;
        try (var input = moduleFile.getContents())
        {
            source = input.readAllBytes();
        }
        try
        {
            writeConfig(changed.getBytes(StandardCharsets.UTF_8));
            assertEquals(CompatibilityMode.VERSION8_311,
                ((Configuration)getTopObjectByFqn("Configuration", getProject())).getCompatibilityMode()); //$NON-NLS-1$
            assertLines(List.of());
            writeConfig(original);
            assertLines(List.of(3, 5, 9, 12));
            assertEquals(stamp, moduleFile.getModificationStamp());
            try (var input = moduleFile.getContents())
            {
                assertTrue(java.util.Arrays.equals(source, input.readAllBytes()));
            }
        }
        finally
        {
            writeConfig(original);
        }
    }

    @Test
    public void testDisabledCheckRemainsDisabled() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "deprecated-8312-residual.bsl"); //$NON-NLS-1$
        assertLines(List.of(3, 5, 9, 12));
        try
        {
            setCheckEnable(false);
            compatibility(CompatibilityMode.VERSION8_311);
            assertLines(List.of());
            compatibility(CompatibilityMode.VERSION8_323);
            assertLines(List.of());
        }
        finally
        {
            compatibility(CompatibilityMode.VERSION8_323);
            setCheckEnable(true);
        }
        assertLines(List.of(3, 5, 9, 12));
    }

    private void writeConfig(byte[] text) throws Exception
    {
        var file = getProject().getWorkspaceProject().getFile("/src/Configuration/Configuration.mdo"); //$NON-NLS-1$
        file.setContents(new ByteArrayInputStream(text), true, true, new NullProgressMonitor());
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
        waitForDD(getProject());
    }

    private void compatibility(CompatibilityMode mode)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Change 8312 compatibility") //$NON-NLS-1$
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

    private String resourceText() throws Exception
    {
        try (var input = getClass().getResourceAsStream(FOLDER_RESOURCE + "deprecated-8312-residual.bsl")) //$NON-NLS-1$
        {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void writeSource(String text) throws Exception
    {
        var file = getProject().getWorkspaceProject().getFile(getModuleFileName());
        file.setContents(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), true, true,
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
            .map(marker -> (Integer)marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE)).sorted().toList());
    }
}
