/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.testing.check.CheckTestBase;
import com.e1c.v8codestyle.internal.md.CorePlugin;
import com.e1c.v8codestyle.md.check.DenyIncompleteValuesCheck;

/** Same metadata and profiles as the live RED, rather than the minimal synthetic fixture. */
public class DenyIncompleteValuesLiveFixtureTest extends CheckTestBase
{
    @Test
    public void testLiveProfileCycles() throws Exception
    {
        var project = openProjectAndWaitForValidationFinish("DimensionRegionSmoke"); //$NON-NLS-1$
        var resource = project.getWorkspaceProject();
        var uid = new CheckUid(DenyIncompleteValuesCheck.CHECK_ID, CorePlugin.PLUGIN_ID);
        var settings = checkRepository.getSettings(uid, resource);
        settings.setEnabled(true);
        checkRepository.applyChanges(List.of(settings), resource);
        waitForDD(project);
        for (int cycle = 0; cycle < 20; cycle++)
        {
            checkRepository.setActiveSettingsProfile("DimensionRegionSmokeAlternate", List.of(), resource); //$NON-NLS-1$
            waitForDD(project);
            for (String fqn : List.of("InformationRegister.Information", "AccumulationRegister.Accumulation", //$NON-NLS-1$ //$NON-NLS-2$
                "AccountingRegister.Accounting", "CalculationRegister.Calculation")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                var owner = getTopObjectByFqn(fqn, project);
                assertEquals(fqn, 0, getMarkersByCheckIds(Set.of(DenyIncompleteValuesCheck.CHECK_ID), null, project,
                    markerManager.getNestedMarkers(resource, owner.bmGetId())).size());
            }
            checkRepository.setActiveSettingsProfile("Default", List.of(), resource); //$NON-NLS-1$
            waitForDD(project);
            for (String fqn : List.of("InformationRegister.Information", "AccumulationRegister.Accumulation", //$NON-NLS-1$ //$NON-NLS-2$
                "AccountingRegister.Accounting", "CalculationRegister.Calculation")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                var owner = getTopObjectByFqn(fqn, project);
                assertEquals("cycle=" + cycle + " " + fqn, 1, //$NON-NLS-1$ //$NON-NLS-2$
                    getMarkersByCheckIds(Set.of(DenyIncompleteValuesCheck.CHECK_ID), null, project,
                        markerManager.getNestedMarkers(resource, owner.bmGetId())).size());
            }
        }
    }
}
