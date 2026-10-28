/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - initial API and implementation
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.IncorrectUseOfStrTemplateCheck;

/** Template corpus, unknown-template exclusions and incremental dependency updates. */
public class IncorrectUseOfStrTemplateCheckTest
    extends AbstractSingleModuleTestBase
{
    public IncorrectUseOfStrTemplateCheckTest()
    {
        super(IncorrectUseOfStrTemplateCheck.class);
    }

    @Test
    public void testCorpusAndModuleBody() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "incorrect-use-of-str-template.bsl"); //$NON-NLS-1$
        assertMarkers(List.of(2, 3, 4, 5, 6, 7, 8, 9, 19, 21, 27));
    }

    @Test
    public void testIncrementalTemplateAssignment() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "str-template-incremental-bad.bsl"); //$NON-NLS-1$
        assertMarkers(List.of(3));
        updateModule(FOLDER_RESOURCE + "str-template-incremental-clean.bsl"); //$NON-NLS-1$
        assertMarkers(List.of());
        updateModule(FOLDER_RESOURCE + "str-template-incremental-bad.bsl"); //$NON-NLS-1$
        assertMarkers(List.of(3));
    }

    @Test
    public void testTemplateBoundaries() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "str-template-boundaries.bsl"); //$NON-NLS-1$
        assertMarkers(List.of(4, 18, 20, 21, 25, 31));
    }

    private void assertMarkers(List<Integer> expected)
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
            String text = node.getRootNode().getText().substring(offset, offset + length).strip();
            assertEquals(true, text.startsWith("СтрШаблон(") || text.startsWith("StrTemplate(")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(true, text.endsWith(")")); //$NON-NLS-1$
        }
    }
}
