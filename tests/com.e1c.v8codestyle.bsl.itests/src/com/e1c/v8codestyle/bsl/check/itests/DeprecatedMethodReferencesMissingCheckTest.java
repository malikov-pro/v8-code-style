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
 *     malikov-pro - checks for references in descriptions of deprecated methods
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.DeprecatedMethodReferencesMissingCheck;

/**
 * Tests for {@link DeprecatedMethodReferencesMissingCheck} — a deprecated
 * procedure (function) references a non-existent procedure (function) in the
 * "См. Модуль.ИмяМетода" replacement reference of its description (section
 * 5.7 of the standard 453, issue #768).
 *
 * @author malikov-pro
 */
public class DeprecatedMethodReferencesMissingCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "up-768-obsolete-refs-missing.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "up-768-obsolete-refs-missing-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public DeprecatedMethodReferencesMissingCheckTest()
    {
        super(DeprecatedMethodReferencesMissingCheck.class);
    }

    /**
     * A "see" reference of a deprecated method to a non-existent method or
     * module is reported.
     */
    @Test
    public void testReferenceToMissingMethodReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 2, markers.size());
    }

    /**
     * References of a deprecated method to an existing method and references
     * of a non-deprecated method have no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), markers.isEmpty());
    }
}
