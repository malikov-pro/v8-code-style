/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com.e1c.v8codestyle.bsl.check.LatinAndCyrillicSymbolInWordCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Pinned LS corpus, parameter variants and incremental updates. */
public class LatinAndCyrillicSymbolInWordCheckTest
    extends AbstractSingleModuleTestBase
{
    public LatinAndCyrillicSymbolInWordCheckTest()
    {
        super(LatinAndCyrillicSymbolInWordCheck.class);
    }

    @Test
    public void testCorpus() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "latin-and-cyrillic.bsl"); //$NON-NLS-1$
        assertLines(List.of(1, 6, 13, 14, 17, 20, 28, 31, 31, 32, 35, 36, 37, 38));
    }

    @Test
    public void testParametersAndIncremental() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "latin-and-cyrillic-parameters.bsl"); //$NON-NLS-1$
        assertLines(List.of(2));
        setParameter(LatinAndCyrillicSymbolInWordCheck.PARAM_ALLOW_TRAILING, "false"); //$NON-NLS-1$
        assertLines(List.of(1, 2));
        setParameter(LatinAndCyrillicSymbolInWordCheck.PARAM_EXCLUDE_WORDS, "Namе"); //$NON-NLS-1$ //$NON-NLS-2$
        assertLines(List.of(1));
        setParameter(LatinAndCyrillicSymbolInWordCheck.PARAM_EXCLUDE_WORDS, ""); //$NON-NLS-1$
        assertLines(List.of(1, 2));
        setParameter(LatinAndCyrillicSymbolInWordCheck.PARAM_ALLOW_TRAILING, "true"); //$NON-NLS-1$
        updateModule(FOLDER_RESOURCE + "latin-and-cyrillic-clean.bsl"); //$NON-NLS-1$
        assertLines(List.of());
        updateModule(FOLDER_RESOURCE + "latin-and-cyrillic-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(1));
        updateModule(FOLDER_RESOURCE + "latin-and-cyrillic-clean.bsl"); //$NON-NLS-1$
        assertLines(List.of());
    }

    private void setParameter(String name, String value)
    {
        IProject project = getProject().getWorkspaceProject();
        ICheckSettings settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID), project);
        settings.getParameters().get(name).setValue(value);
        checkRepository.applyChanges(Collections.singleton(settings), project);
        waitForDD(getProject());
    }

    private void assertLines(List<Integer> expected)
    {
        INode node = NodeModelUtils.findActualNodeFor(getModule());
        for (INode leaf : node.getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(expected, getModuleMarkers().stream()
            .map(marker -> (Integer)marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE))
            .sorted().toList());
        for (var marker : getModuleMarkers())
        {
            Integer offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            Integer length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            assertEquals(Integer.valueOf(length), Integer.valueOf(
                node.getRootNode().getText().substring(offset, offset + length).length()));
        }
    }
}
