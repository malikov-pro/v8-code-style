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
 *     malikov-pro - port of the APK check АПК_00305
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.TypeByMetadataNameCheck;

/**
 * Tests for {@link TypeByMetadataNameCheck} — port of the APK check АПК_00305
 * (standard 442: determine the type of a value by comparing it with a type,
 * not by a metadata name).
 *
 * @author malikov-pro
 */
public class TypeByMetadataNameCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATING = FOLDER_RESOURCE + "apk-00305-type-by-metadata-name.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-00305-type-by-metadata-name-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public TypeByMetadataNameCheckTest()
    {
        super(TypeByMetadataNameCheck.class);
    }

    /**
     * Comparing {@code Metadata().Name} or {@code Metadata().FullName()} of an
     * expression with a string literal (operators {@code =} and {@code <>},
     * literal on either side) is reported; each comparison is one issue.
     */
    @Test
    public void testMetadataNameComparedWithStringLiteral() throws Exception
    {
        updateModule(RESOURCE_VIOLATING);

        List<Marker> markers = getModuleMarkers();
        assertEquals(4, markers.size());
        assertTrue(markers.stream().allMatch(m -> m.getMessage() != null && !m.getMessage().isEmpty()));
    }

    /**
     * Comparing the value type with a type, or a metadata name with a
     * non-literal expression, produces no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
