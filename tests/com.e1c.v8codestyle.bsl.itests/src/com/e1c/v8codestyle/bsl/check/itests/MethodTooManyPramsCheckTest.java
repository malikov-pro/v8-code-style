/*******************************************************************************
 * Copyright (C) 2021, 1C-Soft LLC and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     1C-Soft LLC - initial API and implementation
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.MethodTooManyPramsCheck;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * The test of {@link MethodTooManyPramsCheck} check.
 *
 * @author Dmitriy Marmyshev
 */
public class MethodTooManyPramsCheckTest
    extends AbstractSingleModuleTestBase
{

    public MethodTooManyPramsCheckTest()
    {
        super(MethodTooManyPramsCheck.class);
    }

    /**
     * Test method has too many parameters.
     *
     * @throws Exception the exception
     */
    @Test
    public void testMethodHasTooManyParameters() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "method-too-many-params.bsl");

        List<Marker> markers = getModuleMarkers();
        assertEquals(14, markers.size());

        Set<Integer> errorLines = new HashSet<>();
        for (Marker marker : markers)
        {
            Integer line = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
            switch (line)
            {
            // Noncompliant1
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
                // Noncompliant2
            case 24:
                // Noncompliant3: 29, 30, 31 - not error yet
                // Noncompliant4
            case 40:
                errorLines.add(line);
                break;

            default:
                String message =
                    MessageFormat.format("Unexpected erron on line: {0} and message: {1}", line, marker.getMessage());
                assertFalse(message, true);
                break;
            }
        }
        // check found all errors
        assertEquals(Set.of(7, 8, 9, 10, 11, 12, 13, 24, 40), errorLines);
    }

    @Test
    public void testSevenEightNineAndOptionalBoundaries() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "parameter-count-boundaries.bsl"); //$NON-NLS-1$
        ICheckSettings settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        String originalMax = settings.getParameters().get("maxParams").getValue(); //$NON-NLS-1$
        String originalDefault = settings.getParameters().get("maxDefaultValueParams").getValue(); //$NON-NLS-1$
        assertEquals("7", originalMax); //$NON-NLS-1$
        assertEquals("3", originalDefault); //$NON-NLS-1$
        try
        {
            assertLines(List.of(4, 7, 7, 14));
            setParameter(settings, "maxParams", "8"); //$NON-NLS-1$ //$NON-NLS-2$
            assertLines(List.of(7, 14));
            setParameter(settings, "maxParams", "7"); //$NON-NLS-1$ //$NON-NLS-2$
            setParameter(settings, "maxDefaultValueParams", "0"); //$NON-NLS-1$ //$NON-NLS-2$
            assertLines(List.of(4, 7, 7));
            setParameter(settings, "maxParams", "0"); //$NON-NLS-1$ //$NON-NLS-2$
            assertLines(List.of());
            setParameter(settings, "maxDefaultValueParams", "3"); //$NON-NLS-1$ //$NON-NLS-2$
            assertLines(List.of(14));
        }
        finally
        {
            settings.getParameters().get("maxParams").setValue(originalMax); //$NON-NLS-1$
            settings.getParameters().get("maxDefaultValueParams").setValue(originalDefault); //$NON-NLS-1$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
        }
    }

    private void setParameter(ICheckSettings settings, String key, String value)
    {
        settings.getParameters().get(key).setValue(value);
        checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
        waitForDD(getProject());
    }

    @Test
    public void testIncrementalParameterCount() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "parameter-count-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(1));
        updateModule(FOLDER_RESOURCE + "parameter-count-clean.bsl"); //$NON-NLS-1$
        assertLines(List.of());
        updateModule(FOLDER_RESOURCE + "parameter-count-bad.bsl"); //$NON-NLS-1$
        assertLines(List.of(1));
    }

    private void assertLines(List<Integer> expected)
    {
        INode node = NodeModelUtils.findActualNodeFor(getModule());
        for (INode leaf : node.getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(expected, getModuleMarkers().stream().map(marker -> {
            Integer line = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
            return line;
        }).sorted().toList());
        for (var marker : getModuleMarkers())
        {
            Integer offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            Integer length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            assertEquals(true, node.getRootNode().getText().substring(offset, offset + length).matches("П[1-9]")); //$NON-NLS-1$
        }
    }
}
