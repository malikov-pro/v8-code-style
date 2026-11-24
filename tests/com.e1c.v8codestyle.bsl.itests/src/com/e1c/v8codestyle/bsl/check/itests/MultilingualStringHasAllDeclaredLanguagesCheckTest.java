/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.v8codestyle.bsl.check.MultilingualStringHasAllDeclaredLanguagesCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Explicit languages and mutually exclusive template classification on valid source. */
public class MultilingualStringHasAllDeclaredLanguagesCheckTest extends AbstractSingleModuleTestBase
{
    public MultilingualStringHasAllDeclaredLanguagesCheckTest()
    {
        super(MultilingualStringHasAllDeclaredLanguagesCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "Localization"; //$NON-NLS-1$
    }

    @Test
    public void testLanguagesAndExactRanges() throws Exception
    {
        try
        {
            updateModule(FOLDER_RESOURCE + "localization-overlap.bsl"); //$NON-NLS-1$
            assertLines(List.of(4));
            languages("ru,en"); //$NON-NLS-1$
            assertLines(List.of(3, 4));
            languages("en"); //$NON-NLS-1$
            assertLines(List.of(3));
            languages("ru"); //$NON-NLS-1$
            assertLines(List.of(4));
        }
        finally
        {
            languages("ru"); //$NON-NLS-1$
        }
    }

    @Test
    public void testTemplateVariableClassificationSourceCycles() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "localization-classification.bsl"); //$NON-NLS-1$
        assertLines(List.of(4, 6, 9));
        updateModule(FOLDER_RESOURCE + "localization-classification-clean.bsl"); //$NON-NLS-1$
        assertLines(List.of());
        updateModule(FOLDER_RESOURCE + "localization-classification.bsl"); //$NON-NLS-1$
        assertLines(List.of(4, 6, 9));
    }

    @Test
    public void testPinnedCorpusAndExactRanges() throws Exception
    {
        MultilingualStringPinnedAssertions.verify(this, false, this::languages);
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
        var node = NodeModelUtils.getNode(getModule());
        for (var leaf : node.getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(getModuleMarkers().toString(), lines, getModuleMarkers().stream()
            .map(m -> (Integer)m.getExtraInfo().get(StandardExtraInfo.TEXT_LINE)).sorted().toList());
        for (var marker : getModuleMarkers())
        {
            int offset = (Integer)marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            int length = (Integer)marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            String text = node.getRootNode().getText().substring(offset, offset + length);
            org.junit.Assert.assertTrue(text, text.startsWith("NStr(") && text.endsWith(")")); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}
