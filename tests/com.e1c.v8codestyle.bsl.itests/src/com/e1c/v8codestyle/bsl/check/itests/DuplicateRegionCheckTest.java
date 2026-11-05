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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.v8codestyle.bsl.check.DuplicateRegionCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Ranges, aliases, module-level scope and settings ownership, without clean between transitions. */
public class DuplicateRegionCheckTest extends AbstractSingleModuleTestBase
{
    public DuplicateRegionCheckTest()
    {
        super(DuplicateRegionCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "PlatformApiModern"; //$NON-NLS-1$
    }

    @Test
    public void testAliasesCustomCaseNestedMethodsAndCycles() throws Exception
    {
        var defaults = checkRepository.getDefaultSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        assertTrue(defaults.isEnabled());
        assertEquals(IssueSeverity.TRIVIAL, defaults.getSeverity());
        assertEquals(0, defaults.getParameters().size());
        String source = "#Region Public\n#EndRegion\n#Область программныйинтерфейс\n#КонецОбласти\n" //$NON-NLS-1$
            + "#Region Custom\n#EndRegion\n#Region Custom\n#EndRegion\n" //$NON-NLS-1$
            + "#Region custom\n#EndRegion\n#Region Wrapper\n#Region Public\n#EndRegion\n#EndRegion\n"; //$NON-NLS-1$
        write(source);
        assertRanges(List.of("Region Public", "Region Custom"), List.of(1, 5)); //$NON-NLS-1$ //$NON-NLS-2$
        write(source.replace("#Область программныйинтерфейс", "#Область Other") //$NON-NLS-1$ //$NON-NLS-2$
            .replace("#Region Custom", "#Region Unique")); //$NON-NLS-1$ //$NON-NLS-2$
        // Replacing both identical custom names retains that group.
        assertRanges(List.of("Region Unique"), List.of(5)); //$NON-NLS-1$
        write("#Region Public\n#EndRegion\n#Region Custom\n#EndRegion\n"); //$NON-NLS-1$
        assertRanges(List.of(), List.of());
        write(source);
        assertRanges(List.of("Region Public", "Region Custom"), List.of(1, 5)); //$NON-NLS-1$ //$NON-NLS-2$
        setCheckEnable(false);
        assertRanges(List.of(), List.of());
        setCheckEnable(true);
        assertRanges(List.of("Region Public", "Region Custom"), List.of(1, 5)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testStandardAliasesAndCustomCase() throws Exception
    {
        String[][] pairs = {
            {"Public", "ПрограммныйИнтерфейс"}, {"Internal", "СлужебныйПрограммныйИнтерфейс"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            {"Private", "СлужебныеПроцедурыИФункции"}, {"Variables", "ОписаниеПеременных"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            {"Initialize", "Инициализация"}, {"EventHandlers", "ОбработчикиСобытий"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            {"FormEventHandlers", "ОбработчикиСобытийФормы"}, //$NON-NLS-1$ //$NON-NLS-2$
            {"FormHeaderItemsEventHandlers", "ОбработчикиСобытийЭлементовШапкиФормы"}, //$NON-NLS-1$ //$NON-NLS-2$
            {"FormCommandsEventHandlers", "ОбработчикиКомандФормы"} //$NON-NLS-1$ //$NON-NLS-2$
        };
        for (String[] pair : pairs)
        {
            String first = "#Region " + pair[0]; //$NON-NLS-1$
            write(first + "\n#EndRegion\n#Область " + pair[1] + "\n#КонецОбласти\n"); //$NON-NLS-1$ //$NON-NLS-2$
            assertRanges(List.of(first.substring(1)), List.of(1));
        }
        write("#Region Custom\n#EndRegion\n#Region custom\n#EndRegion\n" //$NON-NLS-1$
            + "#Region Deprecated\n#EndRegion\n#Область УстаревшиеПроцедурыИФункции\n#КонецОбласти\n"); //$NON-NLS-1$
        assertRanges(List.of(), List.of());
    }

    @Test
    public void testLegacyOwnershipSettingsTransitions() throws Exception
    {
        var uid = new CheckUid("module-structure-top-region", BslPlugin.PLUGIN_ID); //$NON-NLS-1$
        var settings = checkRepository.getSettings(uid, getProject().getWorkspaceProject());
        boolean oldEnabled = settings.isEnabled();
        try
        {
            settings.setEnabled(true);
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
            write("#Region Public\n#EndRegion\n#Region Public\n#EndRegion\n"); //$NON-NLS-1$
            assertRanges(List.of("Region Public"), List.of(1)); //$NON-NLS-1$
            assertLegacyDuplicates(0);
            setCheckEnable(false);
            assertRanges(List.of(), List.of());
            assertLegacyDuplicates(2);
            setCheckEnable(true);
            assertRanges(List.of("Region Public"), List.of(1)); //$NON-NLS-1$
            assertLegacyDuplicates(0);
        }
        finally
        {
            settings = checkRepository.getSettings(uid, getProject().getWorkspaceProject());
            settings.setEnabled(oldEnabled);
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            setCheckEnable(true);
            waitForDD(getProject());
        }
    }

    private void assertLegacyDuplicates(int expected)
    {
        var markers = getMarkersByCheckIds(java.util.Set.of("module-structure-top-region"), getModuleId(), //$NON-NLS-1$
            getProject());
        assertEquals(expected, markers.stream().filter(m ->
            Messages.ModuleStructureTopRegionCheck_Region_has_duplicate.equals(m.getMessage())).count());
    }

    @Test
    public void testOriginalPinnedCorpus() throws Exception
    {
        // Original BSL LS fixture at 33a1400681e342ad4bd12281dcee8c7db781ee3d.
        updateModule(FOLDER_RESOURCE + "duplicate-region-ls-original.bsl"); //$NON-NLS-1$
        assertRanges(List.of("Область СлужебныйПрограммныйИнтерфейс", //$NON-NLS-1$
            "Область СлужебныеПроцедурыИФункции", "Region EventHandlers"), List.of(12, 16, 21)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testPreprocessorAndRepeatedCustomGroups() throws Exception
    {
        write("#If Server Then\n#Region Public\n#EndRegion\n#Region PUBLIC\n#EndRegion\n#EndIf\n" //$NON-NLS-1$
            + "#Region Custom\n#EndRegion\n#Region Custom\n#EndRegion\n#Region Custom\n#EndRegion\n"); //$NON-NLS-1$
        assertRanges(List.of("Region Public", "Region Custom"), List.of(2, 7)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testLegacyParameterOffAndNewOwnerIndependent() throws Exception
    {
        var uid = new CheckUid("module-structure-top-region", BslPlugin.PLUGIN_ID); //$NON-NLS-1$
        var settings = checkRepository.getSettings(uid, getProject().getWorkspaceProject());
        boolean oldEnabled = settings.isEnabled();
        String previous = settings.getParameters().get("checkDuplicates").getValue(); //$NON-NLS-1$
        try
        {
            settings.setEnabled(true);
            settings.getParameters().get("checkDuplicates").setValue("false"); //$NON-NLS-1$ //$NON-NLS-2$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
            write("#Region Public\n#EndRegion\n#Region Public\n#EndRegion\n"); //$NON-NLS-1$
            assertRanges(List.of("Region Public"), List.of(1)); //$NON-NLS-1$
            assertLegacyDuplicates(0);
            setCheckEnable(false);
            assertLegacyDuplicates(0);
            setCheckEnable(true);
            settings = checkRepository.getSettings(uid, getProject().getWorkspaceProject());
            settings.setEnabled(false);
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
            assertRanges(List.of("Region Public"), List.of(1)); //$NON-NLS-1$
        }
        finally
        {
            settings = checkRepository.getSettings(uid, getProject().getWorkspaceProject());
            settings.setEnabled(oldEnabled);
            settings.getParameters().get("checkDuplicates").setValue(previous); //$NON-NLS-1$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            setCheckEnable(true);
            waitForDD(getProject());
        }
    }

    @Test
    public void testRegionsInsideMethodAreExcluded() throws Exception
    {
        write("Procedure Probe()\n#Region Public\nA = 1;\n#EndRegion\n" //$NON-NLS-1$
            + "#Region Public\nA = 2;\n#EndRegion\nEndProcedure\n"); //$NON-NLS-1$
        assertRanges(List.of(), List.of());
    }

    @Test
    public void testProfileSwitchWithoutSourceChange() throws Exception
    {
        var project = getProject().getWorkspaceProject();
        String original = checkRepository.getActiveSettingsProfile(project);
        String alternate = "DuplicateRegionOff"; //$NON-NLS-1$
        var uid = new CheckUid("module-structure-top-region", BslPlugin.PLUGIN_ID); //$NON-NLS-1$
        var legacy = checkRepository.getSettings(uid, project);
        boolean wasEnabled = legacy.isEnabled();
        try
        {
            legacy.setEnabled(true);
            checkRepository.applyChanges(List.of(legacy), project);
            waitForDD(getProject());
            write("#Region Public\n#EndRegion\n#Region Public\n#EndRegion\n"); //$NON-NLS-1$
            var file = project.getFile(getModuleFileName());
            long stamp = file.getModificationStamp();
            checkRepository.duplicateSettingsProfile(original, alternate, project);
            checkRepository.setActiveSettingsProfile(alternate, List.of(), project);
            waitForDD(getProject());
            setCheckEnable(false);
            assertLegacyDuplicates(2);
            checkRepository.setActiveSettingsProfile(original, List.of(), project);
            waitForDD(getProject());
            assertRanges(List.of("Region Public"), List.of(1)); //$NON-NLS-1$
            assertLegacyDuplicates(0);
            checkRepository.setActiveSettingsProfile(alternate, List.of(), project);
            waitForDD(getProject());
            assertRanges(List.of(), List.of());
            assertLegacyDuplicates(2);
            assertEquals(stamp, file.getModificationStamp());
        }
        finally
        {
            checkRepository.setActiveSettingsProfile(original, List.of(), project);
            checkRepository.deleteSettingsProfile(alternate, project);
            legacy = checkRepository.getSettings(uid, project);
            legacy.setEnabled(wasEnabled);
            checkRepository.applyChanges(List.of(legacy), project);
            setCheckEnable(true);
            waitForDD(getProject());
        }
    }

    private void assertRanges(List<String> texts, List<Integer> lines)
    {
        var node = NodeModelUtils.findActualNodeFor(getModule());
        for (var leaf : node.getLeafNodes())
        {
            assertNull("line=" + leaf.getStartLine() + " offset=" + leaf.getOffset() + " text=" + leaf.getText(), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                leaf.getSyntaxErrorMessage());
        }
        var markers = getModuleMarkers().stream().sorted(java.util.Comparator.comparingInt(
            m -> (Integer)m.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET))).toList();
        assertEquals(lines, markers.stream().map(m -> (Integer)m.getExtraInfo().get(StandardExtraInfo.TEXT_LINE)).toList());
        String source = node.getRootNode().getText();
        for (int i = 0; i < markers.size(); i++)
        {
            int offset = markers.get(i).getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            int length = markers.get(i).getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            assertEquals(texts.get(i), source.substring(offset, offset + length));
        }
    }

    private void write(String source) throws Exception
    {
        var file = getProject().getWorkspaceProject().getFile(getModuleFileName());
        file.setContents(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), true, true,
            new NullProgressMonitor());
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
        waitForDD(getProject());
    }
}
