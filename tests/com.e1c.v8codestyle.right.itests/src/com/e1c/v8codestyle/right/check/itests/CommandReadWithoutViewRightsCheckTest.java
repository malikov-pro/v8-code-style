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
 *     malikov-pro - port of the upstream issue #646 (standard 689)
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
import com.e1c.v8codestyle.right.check.CommandReadWithoutViewRightsCheck;

/**
 * Tests for {@link CommandReadWithoutViewRightsCheck} check: a role granting
 * the Read or View right for the owner object of a command shall also grant
 * the View right for the command itself.
 *
 * @author malikov-pro
 */
public class CommandReadWithoutViewRightsCheckTest
    extends CheckTestBase
{

    private static final String CHECK_ID = CommandReadWithoutViewRightsCheck.CHECK_ID;

    private static final String PROJECT_NAME = "CommandViewRights";

    /**
     * A role grants the Read right for the owner object but grants no right
     * for the command, so the command is reported.
     */
    @Test
    public void testObjectReadWithoutCommandViewIsReported() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertNotNull(dtProject);

        IBmObject catalog = getTopObjectByFqn("Catalog.ReadNoViewCatalog", dtProject); //$NON-NLS-1$
        Object command = firstCommand(catalog);

        Marker marker = getFirstMarker(CHECK_ID, command, dtProject);
        assertNotNull(marker);
    }

    /**
     * A role grants the View right for the owner object while the command has
     * no custom rights in the role and the role default denies the command
     * view right, so the command is reported.
     */
    @Test
    public void testObjectViewWithoutCommandViewIsReported() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertNotNull(dtProject);

        IBmObject catalog = getTopObjectByFqn("Catalog.GrantedCatalog", dtProject); //$NON-NLS-1$
        Object command = firstCommand(catalog);

        assertNotNull(getFirstMarker(CHECK_ID, command, dtProject));
    }

    /**
     * A role grants the View right for the owner object and explicitly denies
     * the View right for the command, so the command is reported.
     */
    @Test
    public void testExplicitDeniedCommandViewIsReported() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertNotNull(dtProject);

        IBmObject catalog = getTopObjectByFqn("Catalog.NoCmdViewCatalog", dtProject); //$NON-NLS-1$
        Object command = firstCommand(catalog);

        assertNotNull(getFirstMarker(CHECK_ID, command, dtProject));
    }

    /**
     * A role grants the View right for the owner object and grants the View
     * right for the command, so the command is clean.
     */
    @Test
    public void testCommandViewGrantedIsClean() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertNotNull(dtProject);

        IBmObject catalog = getTopObjectByFqn("Catalog.FullCmdCatalog", dtProject); //$NON-NLS-1$
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
