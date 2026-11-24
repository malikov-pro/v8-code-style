/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.right.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.junit.Test;

import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.rights.model.RoleDescription;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.internal.right.CorePlugin;
import com.e1c.v8codestyle.right.check.RightSetPermissionsForNewObjectsCheck;

/**
 * Tests for {@link RightSetPermissionsForNewObjectsCheck}.
 * <p>
 * Fixture project {@code RightSetPermissionsForNewObjects} has five roles:
 * {@code CustomRole} (flag on, reported), {@code ConfiguredRole} (flag on, reported by default),
 * {@code FullAccess} and {@code ПолныеПрава} (flag on, allowed), {@code PlainRole} (flag off).
 *
 * @author malikov-pro
 */
public class RightSetPermissionsForNewObjectsCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{
    private static final String PROJECT_NAME = "RightSetPermissionsForNewObjects"; //$NON-NLS-1$

    private static final String CHECK_ID = "right-set-permissions-for-new-objects"; //$NON-NLS-1$

    private static final String PARAM_NAMES_FULL_ACCESS_ROLE = "namesFullAccessRole"; //$NON-NLS-1$

    private static final String DEFAULT_FULL_ACCESS_ROLES = "FullAccess,ПолныеПрава"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    @Test
    public void testDefaultConfiguration()
    {
        setFullAccessRoleNames(DEFAULT_FULL_ACCESS_ROLES);

        assertEquals(dump("CustomRole"), 1, getRoleMarkers("CustomRole").size()); //$NON-NLS-1$
        assertEquals(dump("ConfiguredRole"), 1, getRoleMarkers("ConfiguredRole").size()); //$NON-NLS-1$
        assertTrue(dump("FullAccess"), getRoleMarkers("FullAccess").isEmpty()); //$NON-NLS-1$
        assertTrue(dump("ПолныеПрава"), getRoleMarkers("ПолныеПрава").isEmpty()); //$NON-NLS-1$
        assertTrue(dump("PlainRole"), getRoleMarkers("PlainRole").isEmpty()); //$NON-NLS-1$

        Marker marker = getRoleMarkers("CustomRole").get(0); //$NON-NLS-1$
        assertTrue(marker.getMessage(), marker.getMessage().contains("CustomRole")); //$NON-NLS-1$
    }

    @Test
    public void testConfiguredFullAccessRole()
    {
        setFullAccessRoleNames(DEFAULT_FULL_ACCESS_ROLES + ",ConfiguredRole"); //$NON-NLS-1$

        assertTrue(dump("ConfiguredRole"), getRoleMarkers("ConfiguredRole").isEmpty()); //$NON-NLS-1$
        assertEquals(dump("CustomRole"), 1, getRoleMarkers("CustomRole").size()); //$NON-NLS-1$
    }

    @Test
    public void testFullAccessNamesAreCaseInsensitive()
    {
        // The configured list uses lower case, the role is named "FullAccess".
        setFullAccessRoleNames("fullaccess,полныеправа"); //$NON-NLS-1$

        assertTrue(dump("FullAccess"), getRoleMarkers("FullAccess").isEmpty()); //$NON-NLS-1$
        assertTrue(dump("ПолныеПрава"), getRoleMarkers("ПолныеПрава").isEmpty()); //$NON-NLS-1$
        assertEquals(dump("CustomRole"), 1, getRoleMarkers("CustomRole").size()); //$NON-NLS-1$
    }

    private void setFullAccessRoleNames(String names)
    {
        IDtProject project = getProject();
        IProject workspaceProject = project.getWorkspaceProject();
        CheckUid uid = new CheckUid(CHECK_ID, CorePlugin.PLUGIN_ID);
        ICheckSettings settings = checkRepository.getSettings(uid, workspaceProject);
        assertNotNull(settings.getParameters().get(PARAM_NAMES_FULL_ACCESS_ROLE));
        settings.getParameters().get(PARAM_NAMES_FULL_ACCESS_ROLE).setValue(names);
        checkRepository.applyChanges(List.of(settings), workspaceProject);
        waitForDD(project);
    }

    private List<Marker> getRoleMarkers(String roleName)
    {
        Object top = getTopObjectByFqn("Role." + roleName + ".Rights", getProject()); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue("Not a RoleDescription: " + roleName, top instanceof RoleDescription); //$NON-NLS-1$
        List<Marker> markers = getMarkersByCheckIds(Set.of(CHECK_ID), top, getProject());
        return markers.stream()
            .filter(marker -> CHECK_ID.equals(getCheckIdFromMarker(marker, getProject())))
            .collect(Collectors.toList());
    }

    private String dump(String roleName)
    {
        // Markers of metadata objects carry no text line: dump the messages only.
        return getRoleMarkers(roleName).stream()
            .map(Marker::getMessage)
            .collect(Collectors.joining("; ")); //$NON-NLS-1$
    }
}
