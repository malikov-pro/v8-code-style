/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: 1C-Soft LLC
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the BSL Language Server diagnostic EmptyCodeBlock
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.EmptyCodeBlockCheck;

/**
 * Tests for {@link EmptyCodeBlockCheck} — port of the BSL Language Server
 * diagnostic EmptyCodeBlock (empty conditional and loop code blocks).
 *
 * @author malikov-pro
 */
public class EmptyCodeBlockCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_EMPTY = FOLDER_RESOURCE + "empty-code-block.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "empty-code-block-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public EmptyCodeBlockCheckTest()
    {
        super(EmptyCodeBlockCheck.class);
    }

    /**
     * Clean module first (filled blocks, empty "Except" block covered by a
     * separate check, empty method bodies — no issues), then the violating
     * module: 6 empty blocks of If/ElsIf/Else and loop bodies are reported.
     */
    @Test
    public void testEmptyBlocks() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> cleanMarkers = getModuleMarkers();
        String cleanDump = cleanMarkers.stream().map(m -> m.getMessage()).collect(Collectors.joining("\n")); //$NON-NLS-1$
        assertTrue("Expected 0 markers, got " + cleanMarkers.size() + ":\n" + cleanDump, cleanMarkers.isEmpty()); //$NON-NLS-1$ //$NON-NLS-2$

        updateModule(RESOURCE_EMPTY);

        List<Marker> markers = getModuleMarkers();
        String dump = markers.stream().map(m -> m.getMessage()).collect(Collectors.joining("\n")); //$NON-NLS-1$
        assertTrue("Expected 6 markers, got " + markers.size() + ":\n" + dump, markers.size() == 6); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
