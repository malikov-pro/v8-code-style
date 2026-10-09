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
 *     malikov-pro - check for the mandatory roles of the configuration
 *******************************************************************************/
package com.e1c.v8codestyle.md.configuration.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.testing.check.CheckTestBase;
import com.e1c.v8codestyle.md.configuration.check.ConfigurationRequiredRolesCheck;

/**
 * Tests for {@link ConfigurationRequiredRolesCheck} — issue #701, standard
 * 488 (section 2). A configuration that misses one of the three mandatory
 * roles gets an issue per missing role; a configuration with all three
 * roles is clean.
 *
 * @author malikov-pro
 */
public class ConfigurationRequiredRolesCheckTest
    extends CheckTestBase
{

    private static final String CHECK_ID = ConfigurationRequiredRolesCheck.CHECK_ID;

    private static final String PROJECT_NAME = "RequiredRoles"; //$NON-NLS-1$

    private static final String PROJECT_CLEAN_NAME = "RequiredRolesClean"; //$NON-NLS-1$

    /**
     * The configuration contains only the role "ПолныеПрава" — the two other
     * mandatory roles are reported on the configuration.
     *
     * @throws Exception the exception
     */
    @Test
    public void testMissingRolesAreReported() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertEquals(2, markers(dtProject).size());
    }

    /**
     * The configuration contains all three mandatory roles — no issues.
     *
     * @throws Exception the exception
     */
    @Test
    public void testAllRolesPresentIsClean() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_CLEAN_NAME);
        assertEquals(0, markers(dtProject).size());
    }

    private List<Marker> markers(IDtProject dtProject)
    {
        long id = getTopObjectIdByFqn("Configuration", dtProject); //$NON-NLS-1$
        return getMarkersByCheckIds(Set.of(CHECK_ID), id, dtProject);
    }
}
