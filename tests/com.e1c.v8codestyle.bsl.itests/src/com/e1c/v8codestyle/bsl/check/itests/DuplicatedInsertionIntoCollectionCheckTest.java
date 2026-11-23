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

import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.DuplicatedInsertionIntoCollectionCheck;

/**
 * Tests for {@link DuplicatedInsertionIntoCollectionCheck}.
 *
 * @author malikov-pro
 */
public class DuplicatedInsertionIntoCollectionCheckTest
    extends AbstractSingleModuleTestBase
{
    public DuplicatedInsertionIntoCollectionCheckTest()
    {
        super(DuplicatedInsertionIntoCollectionCheck.class);
    }

    @Test
    public void testDuplicatedInsertion() throws Exception
    {
        Module module = updateAndGetModule(FOLDER_RESOURCE + "duplicated-insertion-into-collection.bsl"); //$NON-NLS-1$
        assertNoSyntaxErrors(module);

        List<Marker> markers = getModuleMarkers();
        assertEquals(dump(markers), 5, markers.size());

        List<Integer> lines = markers.stream()
            .map(marker -> (Integer)marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE))
            .sorted()
            .collect(Collectors.toList());
        assertEquals(List.of(5, 9, 48, 57, 65), lines);

        Marker first = markers.stream()
            .filter(marker -> Integer.valueOf(5).equals(marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE)))
            .findFirst()
            .orElse(null);
        assertNotNull(dump(markers), first);
        assertTrue(first.getMessage(), first.getMessage().contains("СтрокаТаблицы")); //$NON-NLS-1$
        assertTrue(first.getMessage(), first.getMessage().contains("Массив")); //$NON-NLS-1$

        // The marker is placed on the second insertion, not on the first one.
        String marked = markedText(module, first);
        assertTrue(marked, marked.startsWith("Массив.Добавить(СтрокаТаблицы)")); //$NON-NLS-1$

        Marker structure = markers.stream()
            .filter(marker -> Integer.valueOf(9).equals(marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE)))
            .findFirst()
            .orElse(null);
        assertNotNull(dump(markers), structure);
        assertTrue(structure.getMessage(), structure.getMessage().contains("\"Ключ\"")); //$NON-NLS-1$

        cleanUpProject(getProject().getWorkspaceProject());
    }

    private String markedText(Module module, Marker marker)
    {
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
