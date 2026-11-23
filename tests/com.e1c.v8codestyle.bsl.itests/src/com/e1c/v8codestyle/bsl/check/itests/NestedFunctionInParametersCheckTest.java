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
import com.e1c.v8codestyle.bsl.check.NestedFunctionInParametersCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Tests for {@link NestedFunctionInParametersCheck}.
 *
 * @author malikov-pro
 */
public class NestedFunctionInParametersCheckTest
    extends AbstractSingleModuleTestBase
{
    private static final String FIXTURE = FOLDER_RESOURCE + "nested-function-in-parameters.bsl"; //$NON-NLS-1$

    private static final String PARAM_ALLOW_ONELINER = "allowOneliner"; //$NON-NLS-1$

    private static final String PARAM_ALLOWED_METHOD_NAMES = "allowedMethodNames"; //$NON-NLS-1$

    private static final String DEFAULT_ALLOWED_METHOD_NAMES =
        "НСтр,NStr,ПредопределенноеЗначение,PredefinedValue"; //$NON-NLS-1$

    public NestedFunctionInParametersCheckTest()
    {
        super(NestedFunctionInParametersCheck.class);
    }

    @Test
    public void testDefaultConfiguration() throws Exception
    {
        setParameters(Boolean.TRUE.toString(), DEFAULT_ALLOWED_METHOD_NAMES);
        Module module = updateAndGetModule(FIXTURE);
        assertNoSyntaxErrors(module);

        List<Marker> markers = getModuleMarkers();
        assertEquals(dump(markers), List.of(4, 8, 12, 27), lines(markers));
        assertEquals("Заполнить", textAtLine(module, markers, 4)); //$NON-NLS-1$
        assertEquals("Структура", textAtLine(module, markers, 8)); //$NON-NLS-1$
        assertEquals("Метод", textAtLine(module, markers, 12)); //$NON-NLS-1$
        assertTrue(dump(markers), messages(markers).stream().anyMatch(message -> message.contains("конструктора"))); //$NON-NLS-1$
        assertTrue(dump(markers), messages(markers).stream().anyMatch(message -> message.contains("метода"))); //$NON-NLS-1$
    }

    @Test
    public void testAllowOnelinerFalse() throws Exception
    {
        setParameters(Boolean.FALSE.toString(), DEFAULT_ALLOWED_METHOD_NAMES);
        Module module = updateAndGetModule(FIXTURE);
        assertNoSyntaxErrors(module);

        List<Marker> markers = getModuleMarkers();
        // The multi-line call with parameters on their own lines (line 23) and the whitelisted NStr call
        // that contains a forbidden global call (line 27, the second marker of that line) are reported too.
        assertEquals(dump(markers), List.of(4, 8, 12, 23, 27, 27), lines(markers));
    }

    @Test
    public void testAllowedMethodNamesCaseInsensitive() throws Exception
    {
        setParameters(Boolean.TRUE.toString(),
            "нстр,NStr,ПредопределенноеЗначение,PredefinedValue,Представление,ДругаяФункция"); //$NON-NLS-1$
        Module module = updateAndGetModule(FIXTURE);
        assertNoSyntaxErrors(module);

        List<Marker> markers = getModuleMarkers();
        assertEquals(dump(markers), List.of(4, 8), lines(markers));
    }

    private void setParameters(String allowOneliner, String allowedMethodNames)
    {
        IProject project = getProject().getWorkspaceProject();
        CheckUid uid = new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID);
        ICheckSettings settings = checkRepository.getSettings(uid, project);
        assertNotNull(settings.getParameters().get(PARAM_ALLOW_ONELINER));
        assertNotNull(settings.getParameters().get(PARAM_ALLOWED_METHOD_NAMES));
        settings.getParameters().get(PARAM_ALLOW_ONELINER).setValue(allowOneliner);
        settings.getParameters().get(PARAM_ALLOWED_METHOD_NAMES).setValue(allowedMethodNames);
        checkRepository.applyChanges(List.of(settings), project);
    }

    private static List<Integer> lines(List<Marker> markers)
    {
        return markers.stream()
            .map(marker -> (Integer)marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE))
            .sorted()
            .collect(Collectors.toList());
    }

    private static List<String> messages(List<Marker> markers)
    {
        return markers.stream().map(Marker::getMessage).collect(Collectors.toList());
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
