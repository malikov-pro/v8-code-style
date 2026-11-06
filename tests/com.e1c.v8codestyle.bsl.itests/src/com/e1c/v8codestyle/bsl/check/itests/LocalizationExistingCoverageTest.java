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
import com.e1c.v8codestyle.bsl.check.NstrStringLiteralFormatCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Establish the existing optional completeness invariant before adding new checks. */
public class LocalizationExistingCoverageTest extends AbstractSingleModuleTestBase
{
    public LocalizationExistingCoverageTest()
    {
        super(NstrStringLiteralFormatCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "Localization"; //$NON-NLS-1$
    }

    @Test
    public void testOptionalCompletenessIncludesTemplateAndVariableCases() throws Exception
    {
        var settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        String original = settings.getParameters().get("checkEmptyInterface").getValue(); //$NON-NLS-1$
        try
        {
            updateModule(FOLDER_RESOURCE + "localization-overlap.bsl"); //$NON-NLS-1$
            for (var leaf : NodeModelUtils.getNode(getModule()).getLeafNodes())
            {
                assertNull(leaf.getSyntaxErrorMessage());
            }
            assertEquals(0, getModuleMarkers().size());
            settings.getParameters().get("checkEmptyInterface").setValue("true"); //$NON-NLS-1$ //$NON-NLS-2$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
            assertEquals(getModuleMarkers().toString(), 5, getModuleMarkers().size());
            assertEquals(List.of(3, 4, 5, 6, 7), getModuleMarkers().stream()
                .map(m -> (Integer)m.getExtraInfo().get(StandardExtraInfo.TEXT_LINE)).sorted().toList());
            settings.getParameters().get("checkEmptyInterface").setValue("false"); //$NON-NLS-1$ //$NON-NLS-2$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
            assertEquals(0, getModuleMarkers().size());
        }
        finally
        {
            settings.getParameters().get("checkEmptyInterface").setValue(original); //$NON-NLS-1$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
        }
    }
}
