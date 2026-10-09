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
 *     malikov-pro - port of the upstream issue #645 (standard 689)
 *******************************************************************************/
package com.e1c.v8codestyle.right.check.itests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.testing.check.CheckTestBase;
import com.e1c.v8codestyle.right.check.CommandViewWithoutReadRightsCheck;

/**
 * Tests for {@link CommandViewWithoutReadRightsCheck} check: a role granting
 * the View right for a command shall also grant the Read or View right for
 * the owner object of the command.
 *
 * @author malikov-pro
 */
public class CommandViewWithoutReadRightsCheckTest
    extends CheckTestBase
{

    private static final String CHECK_ID = CommandViewWithoutReadRightsCheck.CHECK_ID;

    private static final String PROJECT_NAME = "CommandViewRights";

    /**
     * A role grants the View right for the command but grants no right for
     * the owner object, so the command is reported.
     */
    @Test
    public void testCommandViewWithoutObjectRightsIsReported() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertNotNull(dtProject);

        IBmObject catalog = getTopObjectByFqn("Catalog.ViewNoReadCatalog", dtProject); //$NON-NLS-1$
        Object command = firstCommand(catalog);

        Marker marker = getFirstMarker(CHECK_ID, command, dtProject);
        assertNotNull(marker);
    }

    /**
     * A role grants the View right for the command and the View right for the
     * owner object, so the command is clean.
     */
    @Test
    public void testCommandViewWithObjectAccessIsClean() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertNotNull(dtProject);

        IBmObject catalog = getTopObjectByFqn("Catalog.FullCmdCatalog", dtProject); //$NON-NLS-1$
        Object command = firstCommand(catalog);

        assertNull(getFirstMarker(CHECK_ID, command, dtProject));
    }

    /**
     * No role grants the View right for the command (the role default denies
     * it), so the command is clean for this check.
     */
    @Test
    public void testCommandWithoutViewRightIsClean() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertNotNull(dtProject);

        IBmObject catalog = getTopObjectByFqn("Catalog.OrphanCatalog", dtProject); //$NON-NLS-1$
        Object command = firstCommand(catalog);

        assertNull(getFirstMarker(CHECK_ID, command, dtProject));
    }

    /**
     * The command of an object included in a subsystem with the "Include in
     * command interface" flag not set is clean regardless of the rights.
     */
    @Test
    public void testCommandOutsideCommandInterfaceIsClean() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertNotNull(dtProject);

        IBmObject catalog = getTopObjectByFqn("Catalog.NoCiCatalog", dtProject); //$NON-NLS-1$
        Object command = firstCommand(catalog);

        assertNull(getFirstMarker(CHECK_ID, command, dtProject));
    }

    private static Object firstCommand(IBmObject catalog)
    {
        assertTrue(catalog instanceof com._1c.g5.v8.dt.metadata.mdclass.Catalog);
        return ((com._1c.g5.v8.dt.metadata.mdclass.Catalog)catalog).getCommands().get(0);
    }

}
