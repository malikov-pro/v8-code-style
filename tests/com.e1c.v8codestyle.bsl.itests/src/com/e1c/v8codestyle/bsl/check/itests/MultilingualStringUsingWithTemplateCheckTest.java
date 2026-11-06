/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
import com.e1c.v8codestyle.bsl.check.MultilingualStringUsingWithTemplateCheck;
import com.e1c.v8codestyle.bsl.check.MultilingualStringHasAllDeclaredLanguagesCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Direct, nested and later-variable template uses share the ordinary classifier. */
public class MultilingualStringUsingWithTemplateCheckTest extends AbstractSingleModuleTestBase
{
    public MultilingualStringUsingWithTemplateCheckTest()
    {
        super(MultilingualStringUsingWithTemplateCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "Localization"; //$NON-NLS-1$
    }

    @Test
    public void testExplicitLanguagesAndSourceClassificationCycles() throws Exception
    {
        try
        {
            languages("ru"); //$NON-NLS-1$
            updateModule(FOLDER_RESOURCE + "localization-overlap.bsl"); //$NON-NLS-1$
            assertLines(List.of(6));
            languages("ru,en"); //$NON-NLS-1$
            assertLines(List.of(5, 6, 7));
            languages("en"); //$NON-NLS-1$
            assertLines(List.of(5, 7));
            languages("ru"); //$NON-NLS-1$
            updateModule(FOLDER_RESOURCE + "localization-classification.bsl"); //$NON-NLS-1$
            assertLines(List.of(2, 5, 7));
            updateModule(FOLDER_RESOURCE + "localization-template-clean.bsl"); //$NON-NLS-1$
            assertLines(List.of());
            updateModule(FOLDER_RESOURCE + "localization-classification.bsl"); //$NON-NLS-1$
            assertLines(List.of(2, 5, 7));
            setCheckEnable(false);
            assertLines(List.of());
            setCheckEnable(true);
            assertLines(List.of(2, 5, 7));
        }
        finally
        {
            languages("ru"); //$NON-NLS-1$
            setCheckEnable(true);
        }
    }

    @Test
    public void testPinnedCorpusAndExactRanges() throws Exception
    {
        MultilingualStringPinnedAssertions.verify(this, true, this::languages);
    }

    @Test
    public void testPairedIncrementalUseChanges() throws Exception
    {
        var project = getProject().getWorkspaceProject();
        var ordinaryUid = new CheckUid(MultilingualStringHasAllDeclaredLanguagesCheck.CHECK_ID, BslPlugin.PLUGIN_ID);
        var ordinary = checkRepository.getSettings(ordinaryUid, project);
        boolean enabled = ordinary.isEnabled();
        String declared = ordinary.getParameters().get("declaredLanguages").getValue(); //$NON-NLS-1$
        String source = "Function Probe(Value) Export\n" //$NON-NLS-1$
            + "    Text = NStr(\"en='Hello'\");\n" //$NON-NLS-1$
            + "    Result = StrTemplate(Text);\n    Return Result;\nEndFunction\n"; //$NON-NLS-1$
        try
        {
            ordinary.setEnabled(true);
            ordinary.getParameters().get("declaredLanguages").setValue("ru"); //$NON-NLS-1$ //$NON-NLS-2$
            checkRepository.applyChanges(List.of(ordinary), project);
            languages("ru"); //$NON-NLS-1$
            writeSource(source);
            assertPair(ordinaryUid, List.of(), List.of(2));
            writeSource(source.replace("StrTemplate(Text)", "StrTemplate(Value)")); //$NON-NLS-1$ //$NON-NLS-2$
            assertPair(ordinaryUid, List.of(2), List.of());
            writeSource(source);
            assertPair(ordinaryUid, List.of(), List.of(2));
            writeSource(source.replace("    Result = StrTemplate(Text);\n", "")); //$NON-NLS-1$ //$NON-NLS-2$
            assertPair(ordinaryUid, List.of(2), List.of());
            writeSource(source);
            assertPair(ordinaryUid, List.of(), List.of(2));
            writeSource(source.replace("StrTemplate(Text)", "StrTemplate((Text))")); //$NON-NLS-1$ //$NON-NLS-2$
            assertPair(ordinaryUid, List.of(2), List.of());
            writeSource(source.replace("en='Hello'", "ru='Привет';en='Hello'")); //$NON-NLS-1$ //$NON-NLS-2$
            assertPair(ordinaryUid, List.of(), List.of());
            writeSource(source);
            assertPair(ordinaryUid, List.of(), List.of(2));
            writeSource(source.replace("Text = NStr(\"en='Hello'\")", "Text = StrTemplate(NStr(\"en='Hello'\"))") //$NON-NLS-1$ //$NON-NLS-2$
                .replace("StrTemplate(Text)", "StrTemplate(Value)")); //$NON-NLS-1$ //$NON-NLS-2$
            assertPair(ordinaryUid, List.of(), List.of(2));
        }
        finally
        {
            var restore = checkRepository.getSettings(ordinaryUid, project);
            restore.setEnabled(enabled);
            restore.getParameters().get("declaredLanguages").setValue(declared); //$NON-NLS-1$
            checkRepository.applyChanges(List.of(restore), project);
            languages("ru"); //$NON-NLS-1$
        }
    }

    private void writeSource(String source) throws Exception
    {
        var file = getProject().getWorkspaceProject().getFile(getModuleFileName());
        file.setContents(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), true, true,
            new NullProgressMonitor());
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
        waitForDD(getProject());
    }

    private void assertPair(CheckUid ordinaryUid, List<Integer> ordinary, List<Integer> template)
    {
        assertLines(template);
        var project = getProject().getWorkspaceProject();
        var markers = List.of(markerManager.getMarkers(project, getModuleId())).stream()
            .filter(m -> ordinaryUid.equals(checkRepository.getUidForShortUid(m.getCheckId(), project))).toList();
        assertEquals(ordinary, markers.stream()
            .map(m -> (Integer)m.getExtraInfo().get(StandardExtraInfo.TEXT_LINE)).sorted().toList());
        var templateOffsets = getModuleMarkers().stream()
            .map(m -> m.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET)).toList();
        for (var marker : markers)
        {
            org.junit.Assert.assertFalse(templateOffsets.contains(marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET)));
        }
    }

    private void languages(String value)
    {
        var settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        settings.getParameters().get("declaredLanguages").setValue(value); //$NON-NLS-1$
        checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
        waitForDD(getProject());
    }

    private void assertLines(List<Integer> lines)
    {
        for (var leaf : NodeModelUtils.getNode(getModule()).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(getModuleMarkers().toString(), lines, getModuleMarkers().stream()
            .map(m -> (Integer)m.getExtraInfo().get(StandardExtraInfo.TEXT_LINE)).sorted().toList());
    }
}
