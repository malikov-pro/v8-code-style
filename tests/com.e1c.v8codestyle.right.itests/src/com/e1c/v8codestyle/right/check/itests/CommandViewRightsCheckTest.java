/*******************************************************************************
 * Copyright (C) 2021, 1C-Soft LLC and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     1C-Soft LLC - initial API and implementation
 *******************************************************************************/
package com.e1c.v8codestyle.right.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.rights.model.ObjectRight;
import com._1c.g5.v8.dt.rights.model.ObjectRights;
import com._1c.g5.v8.dt.rights.model.Rls;
import com._1c.g5.v8.dt.rights.model.RoleDescription;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.testing.check.CheckTestBase;
import com.e1c.v8codestyle.right.check.CommandViewRightsCheck;

/**
 * Tests for {@link RoleRightHasRls} check.
 *
 * @author Dmitriy Marmyshev
 */
public class CommandViewRightsCheckTest
    extends CheckTestBase
{

    private static final String CHECK_ID = CommandViewRightsCheck.CHECK_ID;

    private static final String PROJECT_NAME = "CommandViewRights";

    /**
     * A command of an object included in a subsystem with the "Include in
     * command interface" flag set is reported when no role grants the View
     * right for the owner object, including the role that explicitly denies
     * the right.
     */
    @Test
    public void testCommandWithoutViewRightIsReported() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertNotNull(dtProject);

        IBmObject catalog = getTopObjectByFqn("Catalog.OrphanCatalog", dtProject); //$NON-NLS-1$
        assertTrue(catalog instanceof com._1c.g5.v8.dt.metadata.mdclass.Catalog);
        Object command = ((com._1c.g5.v8.dt.metadata.mdclass.Catalog)catalog).getCommands().get(0);

        Marker marker = getFirstMarker(CHECK_ID, command, dtProject);
        assertNotNull(marker);
    }

    /**
     * A command of an object included in a subsystem with the "Include in
     * command interface" flag set is clean when at least one role grants the
     * View right for the owner object.
     */
    @Test
    public void testCommandWithViewRightIsClean() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertNotNull(dtProject);

        IBmObject catalog = getTopObjectByFqn("Catalog.GrantedCatalog", dtProject); //$NON-NLS-1$
        Object command = ((com._1c.g5.v8.dt.metadata.mdclass.Catalog)catalog).getCommands().get(0);

        assertNull(getFirstMarker(CHECK_ID, command, dtProject));
    }

    /**
     * A command of an object included in a subsystem with the "Include in
     * command interface" flag not set is clean regardless of the rights.
     */
    @Test
    public void testCommandOutsideCommandInterfaceIsClean() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertNotNull(dtProject);

        IBmObject catalog = getTopObjectByFqn("Catalog.NoCiCatalog", dtProject); //$NON-NLS-1$
        Object command = ((com._1c.g5.v8.dt.metadata.mdclass.Catalog)catalog).getCommands().get(0);

        assertNull(getFirstMarker(CHECK_ID, command, dtProject));
    }
}
