/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.bsl.model.BslFactory;
import com._1c.g5.v8.dt.bsl.model.IfStatement;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.v8codestyle.bsl.check.ExcessiveAutoTestCheck;
import com.e1c.v8codestyle.bsl.check.ModuleTypeFilter;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class ExcessiveAutoTestCheckTest extends AbstractSingleModuleTestBase
{
    private static final List<Integer> LINES = List.of(2, 5, 8, 11, 24, 38, 44, 47, 54, 61, 67);

    public ExcessiveAutoTestCheckTest()
    {
        super(ExcessiveAutoTestCheck.class);
    }

    @Test
    public void testPinnedScopeDefaultsExactRangesAndCycles() throws Exception
    {
        var defaults = checkRepository.getDefaultSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        assertFalse(defaults.isEnabled());
        assertEquals(IssueSeverity.MINOR, defaults.getSeverity());
        assertEquals(0, defaults.getParameters().size());
        try
        {
            updateModule(FOLDER_RESOURCE + "excessive-auto-test-check.bsl"); //$NON-NLS-1$
            assertLines(LINES);
            updateModule(FOLDER_RESOURCE + "excessive-auto-test-check-clean.bsl"); //$NON-NLS-1$
            assertLines(List.of());
            updateModule(FOLDER_RESOURCE + "excessive-auto-test-check.bsl"); //$NON-NLS-1$
            assertLines(LINES);
            setCheckEnable(false);
            assertLines(List.of());
            setCheckEnable(true);
            assertLines(LINES);
            updateModule(FOLDER_RESOURCE + "excessive-auto-test-boundaries.bsl"); //$NON-NLS-1$
            assertLines(List.of(4, 7));
        }
        finally
        {
            setCheckEnable(false);
        }
    }

    /** Filter contract only; does not replace real module-owner runtime coverage. */
    @Test
    public void testFourModuleTypeFilterBoundaries()
    {
        var included = Set.of(ModuleType.FORM_MODULE, ModuleType.OBJECT_MODULE,
            ModuleType.RECORDSET_MODULE, ModuleType.COMMON_MODULE);
        var filter = ModuleTypeFilter.onlyTypes(included.toArray(ModuleType[]::new));
        for (var type : ModuleType.VALUES)
        {
            var module = BslFactory.eINSTANCE.createModule();
            module.setModuleType(type);
            var method = BslFactory.eINSTANCE.createProcedure();
            module.getMethods().add(method);
            assertEquals(type.toString(), included.contains(type),
                filter.preCheck(method, null, new NullProgressMonitor()));
        }
    }

    private void assertLines(List<Integer> expected)
    {
        var module = getModule();
        var root = NodeModelUtils.findActualNodeFor(module);
        for (var leaf : root.getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        var markers = getModuleMarkers();
        String diagnostic = "type=" + module.getModuleType() + "; ifs=" //$NON-NLS-1$ //$NON-NLS-2$
            + EcoreUtil2.getAllContentsOfType(module, IfStatement.class).stream().map(s -> {
                var branch = s.getIfPart();
                var predicate = NodeModelUtils.getNode(branch.getPredicate());
                return branch.getStatements().stream().map(v -> v.eClass().getName()).toList()
                    + ":" + (predicate == null ? "null" : predicate.getText()); //$NON-NLS-1$ //$NON-NLS-2$
            }).toList();
        assertEquals(diagnostic, expected, markers.stream()
            .map(m -> (Integer)m.getExtraInfo().get(StandardExtraInfo.TEXT_LINE)).sorted().toList());
        var statements = EcoreUtil2.getAllContentsOfType(module, IfStatement.class);
        for (var marker : markers)
        {
            int line = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
            int offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            int length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            var statement = statements.stream().map(NodeModelUtils::getNode)
                .filter(n -> n.getStartLine() <= line && n.getEndLine() >= line)
                .filter(n -> n.getOffset() <= offset && n.getEndOffset() >= offset + length)
                .min(java.util.Comparator.comparingInt(n -> n.getLength())).orElseThrow();
            var significant = new java.util.ArrayList<org.eclipse.xtext.nodemodel.ILeafNode>();
            for (var leaf : statement.getLeafNodes())
            {
                if (!leaf.isHidden())
                {
                    significant.add(leaf);
                }
            }
            assertEquals(significant.get(0).getOffset(), offset);
            // LS range ends at EndIf, excluding its optional semicolon.
            var end = significant.stream().filter(n -> n.getText().equalsIgnoreCase("EndIf") //$NON-NLS-1$
                || n.getText().equalsIgnoreCase("КонецЕсли")).reduce((a, b) -> b).orElseThrow(); //$NON-NLS-1$
            assertEquals(end.getEndOffset() - offset, length);
        }
    }
}
