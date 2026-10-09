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
import com.e1c.v8codestyle.bsl.check.DeprecatedMethodReferencesObsoleteCheck;

/**
 * Tests for {@link DeprecatedMethodReferencesObsoleteCheck} — a deprecated
 * procedure (function) references another deprecated procedure (function)
 * in the "См. Модуль.ИмяМетода" replacement reference of its description
 * (section 5.7 of the standard 453, issue #769).
 *
 * @author malikov-pro
 */
public class DeprecatedMethodReferencesObsoleteCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "up-769-obsolete-refs-obsolete.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "up-769-obsolete-refs-obsolete-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public DeprecatedMethodReferencesObsoleteCheckTest()
    {
        super(DeprecatedMethodReferencesObsoleteCheck.class);
    }

    /**
     * A "see" reference of a deprecated method to another deprecated method is reported.
     */
    @Test
    public void testReferenceToDeprecatedMethodReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 2, markers.size());
    }

    /**
     * References of a deprecated method to an actual method and references of
     * a non-deprecated method have no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), markers.isEmpty());
    }
}
