/*******************************************************************************
 * Copyright (C) 2025, 1C-Soft LLC and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Volkov - initial API and implementation
 *******************************************************************************/
package com.e1c.v8codestyle.right.check.itests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmObject;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.internal.right.itests.CheckRights;
import com.e1c.v8codestyle.right.check.RoleRightSetCheck;

/**
 * Tests for {@link RoleRightSetCheck} check.
 *
 * @author Andrey Volkov
 */
public class RoleRightSetCheckTest
    extends CheckRights
{

    /**
     * The checks derived from {@link RoleRightSetCheck}: only they are covered by this test.
     * <p>
     * A total marker count of a role cannot be asserted: the allow-all fixture roles intentionally have
     * {@code setForNewObjects=true} to exercise the role-level branch of {@link RoleRightSetCheck}, and v8std 532
     * reports such roles with the check {@code right-set-permissions-for-new-objects}, which is outside this test.
     */
    private static final Set<String> ROLE_RIGHT_SET_CHECK_IDS = Set.of(
        "right-active-users",
        "right-administration",
        "right-all-functions-mode",
        "right-configuration-extensions-administration",
        "right-data-administration",
        "right-delete",
        "right-exclusive-mode",
        "right-interactive-clear-deletion-mark-predefined-data",
        "right-interactive-delete",
        "right-interactive-delete-marked-predefined-data",
        "right-interactive-delete-predefined-data",
        "right-interactive-open-external-data-processors",
        "right-interactive-open-external-reports",
        "right-interactive-set-deletion-mark-predefined-data",
        "right-output-to-printer-file-clipboard",
        "right-save-user-data",
        "right-start-automation",
        "right-start-external-connection",
        "right-start-thick-client",
        "right-start-thin-client",
        "right-start-web-client",
        "right-update-database-configuration",
        "right-view-event-log");
    @Test
    public void testMainProjectRolesCorrect() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish("RoleRightSetCheckTest");
        assertNotNull(dtProject);

        checkMarkerCount(dtProject, "Role.Роль1AllowAll.Rights", 0);
        checkMarkerCount(dtProject, "Role.Роль2DisableAll.Rights", 0);
    }

    @Test
    public void testExtensionProjectRolesCorrect() throws Exception
    {
        IDtProject dtProject =
            openProjectAndWaitForValidationFinish("RoleRightSetCheckTest.RoleRightSetCheckExtensionTest");
        assertNotNull(dtProject);

        checkMarkerCount(dtProject, "Role.Расш1_ОсновнаяРоль.Rights", 6);
        checkMarkerCount(dtProject, "Role.Расш1_Роль1AllowAll.Rights", 0);
        checkMarkerCount(dtProject, "Role.Расш1_Роль2DisableAll.Rights", 0);
        checkMarkerCount(dtProject, "Role.Роль1AllowAll.Rights", 0);
        checkMarkerCount(dtProject, "Role.Роль2DisableAll.Rights", 0);
    }

    private void checkMarkerCount(IDtProject dtProject, String roleDescriptionFqn, int markerCount) throws CoreException
    {
        IBmObject bmRoleDescription = getTopObjectByFqn(roleDescriptionFqn, dtProject);
        assertNotNull(bmRoleDescription);

        Object extractedMarkerContainerId = extractMarkerContainerId(bmRoleDescription.bmGetId());
        Marker[] markers = markerManager.getMarkers(dtProject.getWorkspaceProject(), extractedMarkerContainerId);

        long actual = Arrays.stream(markers)
            .filter(marker -> ROLE_RIGHT_SET_CHECK_IDS.contains(checkIdOf(marker, dtProject)))
            .count();
        assertTrue(MessageFormat.format("Unexpected marker count {0} for {1}: {2}", actual, roleDescriptionFqn,
            Arrays.toString(markers)), actual == markerCount);
    }

    private String checkIdOf(Marker marker, IDtProject dtProject)
    {
        CheckUid uid = checkRepository.getUidForShortUid(marker.getCheckId(), dtProject.getWorkspaceProject());
        return uid == null ? null : uid.getCheckId();
    }
}
