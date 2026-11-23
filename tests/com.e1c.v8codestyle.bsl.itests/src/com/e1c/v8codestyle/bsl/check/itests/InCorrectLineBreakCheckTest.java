/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com.e1c.v8codestyle.bsl.check.InCorrectLineBreakCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Tests for {@link InCorrectLineBreakCheck}.
 *
 * @author malikov-pro
 */
public class InCorrectLineBreakCheckTest
    extends AbstractSingleModuleTestBase
{
    private static final String FIXTURE = FOLDER_RESOURCE + "incorrect-line-break.bsl"; //$NON-NLS-1$

    private static final String PARAM_CHECK_FIRST_SYMBOL = "checkFirstSymbol"; //$NON-NLS-1$

    private static final String PARAM_CHECK_LAST_SYMBOL = "checkLastSymbol"; //$NON-NLS-1$

    public InCorrectLineBreakCheckTest()
    {
        super(InCorrectLineBreakCheck.class);
    }

    @Test
    public void testDefaultConfiguration() throws Exception
    {
        setParameters(Boolean.TRUE.toString(), Boolean.TRUE.toString());
        Module module = updateAndGetModule(FIXTURE);
        assertNoSyntaxErrors(module);

        List<Marker> markers = getModuleMarkers();
        assertEquals(dump(markers), List.of(4, 8, 17, 21, 25), lines(markers));
        assertEquals("+", textAtLine(module, markers, 4)); //$NON-NLS-1$
        assertEquals("И", textAtLine(module, markers, 8)); //$NON-NLS-1$
        assertEquals(")", textAtLine(module, markers, 17)); //$NON-NLS-1$
        // The LS default list contains "," followed by any non-space run, so the closing ");" belongs to the marker.
        assertEquals(",СуммаРучнойСкидки);", textAtLine(module, markers, 21)); //$NON-NLS-1$
        assertEquals(";", textAtLine(module, markers, 25)); //$NON-NLS-1$
    }

    @Test
    public void testFirstSymbolDisabled() throws Exception
    {
        setParameters(Boolean.FALSE.toString(), Boolean.TRUE.toString());
        Module module = updateAndGetModule(FIXTURE);
        assertNoSyntaxErrors(module);

        assertEquals(dump(getModuleMarkers()), List.of(4, 8), lines(getModuleMarkers()));
    }

    @Test
    public void testLastSymbolDisabled() throws Exception
    {
        setParameters(Boolean.TRUE.toString(), Boolean.FALSE.toString());
        Module module = updateAndGetModule(FIXTURE);
        assertNoSyntaxErrors(module);

        assertEquals(dump(getModuleMarkers()), List.of(17, 21, 25), lines(getModuleMarkers()));
    }

    private void setParameters(String checkFirstSymbol, String checkLastSymbol)
    {
        IProject project = getProject().getWorkspaceProject();
        CheckUid uid = new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID);
        ICheckSettings settings = checkRepository.getSettings(uid, project);
        assertEquals(4, settings.getParameters().size());
        assertNotNull(settings.getParameters().get(PARAM_CHECK_FIRST_SYMBOL));
        assertNotNull(settings.getParameters().get(PARAM_CHECK_LAST_SYMBOL));
        settings.getParameters().get(PARAM_CHECK_FIRST_SYMBOL).setValue(checkFirstSymbol);
        settings.getParameters().get(PARAM_CHECK_LAST_SYMBOL).setValue(checkLastSymbol);
        checkRepository.applyChanges(List.of(settings), project);
    }

    private static List<Integer> lines(List<Marker> markers)
    {
        return markers.stream()
            .map(marker -> (Integer)marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE))
            .sorted()
            .collect(Collectors.toList());
    }

    private static String textAtLine(Module module, List<Marker> markers, int line)
    {
        Marker marker = markers.stream()
            .filter(item -> Integer.valueOf(line).equals(item.getExtraInfo().get(StandardExtraInfo.TEXT_LINE)))
            .findFirst()
            .orElse(null);
        assertNotNull("No marker at line " + line, marker); //$NON-NLS-1$
        Integer offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
        Integer length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
        assertTrue("Marker has no offset/length", offset != null && length != null); //$NON-NLS-1$
        return NodeModelUtils.findActualNodeFor(module).getRootNode().getText().substring(offset, offset + length);
    }

    private static void assertNoSyntaxErrors(Module module)
    {
        var node = NodeModelUtils.findActualNodeFor(module);
        assertNotNull("No node model", node); //$NON-NLS-1$
        for (var leaf : node.getLeafNodes())
        {
            assertNull("Syntax error at line " + leaf.getStartLine() + ": " + leaf.getSyntaxErrorMessage(), //$NON-NLS-1$ //$NON-NLS-2$
                leaf.getSyntaxErrorMessage());
        }
    }

    private static String dump(List<Marker> markers)
    {
        return markers.stream()
            .map(marker -> marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE) + " " + marker.getMessage()) //$NON-NLS-1$
            .collect(Collectors.joining("; ")); //$NON-NLS-1$
    }
}
