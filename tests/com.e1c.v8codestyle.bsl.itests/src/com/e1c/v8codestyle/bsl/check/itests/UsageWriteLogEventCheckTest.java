/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.v8codestyle.bsl.check.UsageWriteLogEventCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class UsageWriteLogEventCheckTest extends AbstractSingleModuleTestBase
{
    private static final List<Integer> LINES = List.of(2, 6, 10, 17, 25, 42, 70, 73);
    private static final List<String> MESSAGES = List.of(
        "Укажите не менее пяти аргументов записи в журнал регистрации", //$NON-NLS-1$
        "Укажите второй аргумент — уровень журнала регистрации", //$NON-NLS-1$
        "Укажите пятый аргумент — комментарий", //$NON-NLS-1$
        "Укажите уровень Ошибка при записи в журнал внутри Исключения", //$NON-NLS-1$
        "Добавьте ПодробноеПредставлениеОшибки(ИнформацияОбОшибке()) в комментарий записи", //$NON-NLS-1$
        "Добавьте ПодробноеПредставлениеОшибки(ИнформацияОбОшибке()) в комментарий записи", //$NON-NLS-1$
        "Укажите не менее пяти аргументов записи в журнал регистрации", //$NON-NLS-1$
        "Укажите не менее пяти аргументов записи в журнал регистрации"); //$NON-NLS-1$

    public UsageWriteLogEventCheckTest()
    {
        super(UsageWriteLogEventCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "PlatformApiModern"; //$NON-NLS-1$
    }

    @Test
    public void testFivePrioritiesDefaultsRangesAndCycles() throws Exception
    {
        var defaults = checkRepository.getDefaultSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        assertTrue(defaults.isEnabled());
        assertEquals(IssueSeverity.TRIVIAL, defaults.getSeverity());
        assertEquals(0, defaults.getParameters().size());
        try
        {
            updateModule(FOLDER_RESOURCE + "usage-write-log-event.bsl"); //$NON-NLS-1$
            assertMarkers(LINES, MESSAGES);
            updateModule(FOLDER_RESOURCE + "usage-write-log-event-clean.bsl"); //$NON-NLS-1$
            assertMarkers(List.of(), List.of());
            updateModule(FOLDER_RESOURCE + "usage-write-log-event.bsl"); //$NON-NLS-1$
            assertMarkers(LINES, MESSAGES);
            setCheckEnable(false);
            assertMarkers(List.of(), List.of());
            setCheckEnable(true);
            assertMarkers(LINES, MESSAGES);
        }
        finally
        {
            setCheckEnable(true);
        }
    }

    private void assertMarkers(List<Integer> lines, List<String> messages)
    {
        var module = getModule();
        for (var leaf : NodeModelUtils.findActualNodeFor(module).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        var markers = getModuleMarkers().stream().sorted(java.util.Comparator.comparingInt(
            m -> (Integer)m.getExtraInfo().get(StandardExtraInfo.TEXT_LINE))).toList();
        assertEquals(lines, markers.stream()
            .map(m -> (Integer)m.getExtraInfo().get(StandardExtraInfo.TEXT_LINE)).toList());
        assertEquals(messages, markers.stream().map(m -> m.getMessage()).toList());
        for (var marker : markers)
        {
            int offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            int length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            assertTrue(EcoreUtil2.getAllContentsOfType(module, Invocation.class).stream()
                .map(NodeModelUtils::getNode).anyMatch(n -> n.getOffset() == offset
                    && n.getEndOffset() == offset + length));
        }
    }

    @Test
    public void testPinnedBoundaryMatrix() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "usage-write-log-event-boundaries.bsl"); //$NON-NLS-1$
        // Expected values are pinned-LS differential results, not inference from EDT AST.
        assertMarkerLines(List.of(5, 6, 11, 13, 17, 31, 34, 40, 51, 63));
    }

    private void assertMarkerLines(List<Integer> lines)
    {
        for (var leaf : NodeModelUtils.findActualNodeFor(getModule()).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(lines, getModuleMarkers().stream()
            .map(m -> (Integer)m.getExtraInfo().get(StandardExtraInfo.TEXT_LINE)).sorted().toList());
    }

    @Test
    public void testOriginalPinnedCorpusExactRanges() throws Exception
    {
        // BSL LS fixture at 33a1400681e342ad4bd12281dcee8c7db781ee3d.
        updateModule(FOLDER_RESOURCE + "usage-write-log-event-ls-original.bsl"); //$NON-NLS-1$
        assertMarkerLines(List.of(4, 5, 6, 8, 12, 17, 24, 32, 39, 46, 191, 205, 220, 287, 355, 369, 384, 440));
        int[][] ranges = {
            {4, 4, 4, 39}, {5, 4, 5, 73}, {6, 4, 6, 77}, {8, 4, 10, 61}, {12, 4, 12, 79},
            {17, 6, 18, 25}, {24, 6, 25, 24}, {32, 6, 33, 45}, {39, 6, 40, 37}, {46, 6, 47, 21},
            {191, 6, 193, 56}, {205, 6, 207, 22}, {220, 6, 222, 22}, {287, 12, 292, 39},
            {355, 6, 357, 73}, {369, 6, 371, 22}, {384, 6, 386, 22}, {440, 12, 445, 39}
        };
        var markers = getModuleMarkers().stream().sorted(java.util.Comparator.comparingInt(
            m -> (Integer)m.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET))).toList();
        String text = NodeModelUtils.findActualNodeFor(getModule()).getRootNode().getText();
        for (int i = 0; i < ranges.length; i++)
        {
            int[] range = ranges[i];
            int start = sourceOffset(text, range[0], range[1]);
            int end = sourceOffset(text, range[2], range[3]);
            assertEquals(start, (int)markers.get(i).getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET));
            assertEquals(end - start, (int)markers.get(i).getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH));
        }
    }

    private static int sourceOffset(String text, int line, int column)
    {
        int offset = 0;
        for (int i = 1; i < line; i++)
        {
            offset = text.indexOf('\n', offset) + 1;
            assertTrue(offset > 0);
        }
        return offset + column;
    }
}
