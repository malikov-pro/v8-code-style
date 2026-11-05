/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.junit.Test;
import org.junit.Before;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com._1c.g5.v8.dt.metadata.mdclass.RegisterDimension;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.testing.check.CheckTestBase;
import com._1c.g5.v8.dt.core.platform.IDtProject;
import com.e1c.v8codestyle.internal.md.CorePlugin;
import com.e1c.v8codestyle.md.check.DenyIncompleteValuesCheck;

/** Real metadata owners, precise dimension anchors and repeated incremental model/file updates. */
public class DenyIncompleteValuesCheckTest extends CheckTestBase
{
    private IDtProject project;
    private static final List<String> OWNERS = List.of("InformationRegister.Information", //$NON-NLS-1$
        "AccumulationRegister.Accumulation", "AccountingRegister.Accounting", "CalculationRegister.Calculation"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    @Before
    public void setUp() throws Exception
    {
        project = openProjectAndWaitForValidationFinish("DenyIncompleteValues"); //$NON-NLS-1$
    }

    private IDtProject getProject()
    {
        return project;
    }

    @Test
    public void testDefaultsAndFourModelCycles() throws Exception
    {
        var uid = new CheckUid(DenyIncompleteValuesCheck.CHECK_ID, CorePlugin.PLUGIN_ID);
        var defaults = checkRepository.getDefaultSettings(uid, getProject().getWorkspaceProject());
        assertFalse(defaults.isEnabled());
        assertEquals(IssueSeverity.MAJOR, defaults.getSeverity());
        assertEquals(0, defaults.getParameters().size());
        try
        {
            enable(false);
            assertCounts(0);
            enable(true);
            assertCounts(1);
            for (String fqn : OWNERS)
            {
                for (int cycle = 0; cycle < 2; cycle++)
                {
                    changeFlag(fqn, true);
                    assertCount(fqn, 0);
                    changeFlag(fqn, false);
                    assertCount(fqn, 1);
                }
            }
            enable(false);
            assertCounts(0);
            enable(true);
            assertCounts(1);
        }
        finally
        {
            for (String fqn : OWNERS)
            {
                changeFlag(fqn, false);
            }
            enable(false);
        }
    }

    @Test
    public void testFourFileCycles() throws Exception
    {
        try
        {
            enable(true);
            for (String fqn : OWNERS)
            {
                String[] parts = fqn.split("\\."); //$NON-NLS-1$
                var file = getProject().getWorkspaceProject().getFile("src/" + parts[0] + "s/" //$NON-NLS-1$ //$NON-NLS-2$
                    + parts[1] + "/" + parts[1] + ".mdo"); //$NON-NLS-1$ //$NON-NLS-2$
                byte[] original;
                try (var in = file.getContents())
                {
                    original = in.readAllBytes();
                }
                String text = new String(original, StandardCharsets.UTF_8);
                String fixed = text.replace("<denyIncompleteValues>false</denyIncompleteValues>", //$NON-NLS-1$
                    "<denyIncompleteValues>true</denyIncompleteValues>"); //$NON-NLS-1$
                assertFalse(text.equals(fixed));
                try
                {
                    assertCount(fqn, 1);
                    file.setContents(new ByteArrayInputStream(fixed.getBytes(StandardCharsets.UTF_8)), true, true,
                        new NullProgressMonitor());
                    ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
                    waitForDD(getProject());
                    assertCount(fqn, 0);
                    file.setContents(new ByteArrayInputStream(original), true, true, new NullProgressMonitor());
                    ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
                    waitForDD(getProject());
                    assertCount(fqn, 1);
                }
                finally
                {
                    file.setContents(new ByteArrayInputStream(original), true, true, new NullProgressMonitor());
                    ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
                    waitForDD(getProject());
                }
            }
        }
        finally
        {
            enable(false);
        }
    }

    private void assertCounts(int expected)
    {
        for (String fqn : OWNERS)
        {
            assertCount(fqn, expected);
        }
    }

    @Test
    public void testProfileSwitch() throws Exception
    {
        var project = getProject().getWorkspaceProject();
        String original = checkRepository.getActiveSettingsProfile(project);
        String alternate = "DimensionAuditOn"; //$NON-NLS-1$
        try
        {
            enable(false);
            checkRepository.duplicateSettingsProfile(original, alternate, project);
            checkRepository.setActiveSettingsProfile(alternate, List.of(), project);
            waitForDD(getProject());
            enable(true);
            assertCounts(1);
            checkRepository.setActiveSettingsProfile(original, List.of(), project);
            waitForDD(getProject());
            assertCounts(0);
            checkRepository.setActiveSettingsProfile(alternate, List.of(), project);
            waitForDD(getProject());
            assertCounts(1);
        }
        finally
        {
            checkRepository.setActiveSettingsProfile(original, List.of(), project);
            checkRepository.deleteSettingsProfile(alternate, project);
            enable(false);
        }
    }

    private void assertCount(String fqn, int expected)
    {
        var owner = getTopObjectByFqn(fqn, getProject());
        var markers = getMarkersByCheckIds(Set.of(DenyIncompleteValuesCheck.CHECK_ID), null, getProject(),
            markerManager.getNestedMarkers(getProject().getWorkspaceProject(), owner.bmGetId()));
        assertEquals(fqn, expected, markers.size());
        for (var marker : markers)
        {
            assertEquals(MdClassPackage.REGISTER_DIMENSION__DENY_INCOMPLETE_VALUES, marker.getFeatureId());
            assertEquals(((IBmObject)dimensions(owner).get(0)).bmGetId(), marker.getMarkerObjectId());
            assertTrue(marker.getMessage(), marker.getMessage().contains("Optional")); //$NON-NLS-1$
        }
    }

    @Test
    public void testRepeatedSettingsAndProfileAfterAllFlagChanges() throws Exception
    {
        var project = getProject().getWorkspaceProject();
        String original = checkRepository.getActiveSettingsProfile(project);
        String alternate = "DimensionAuditOffRepeat"; //$NON-NLS-1$
        try
        {
            enable(true);
            for (String fqn : OWNERS)
            {
                changeFlag(fqn, true);
            }
            assertCounts(0);
            for (String fqn : OWNERS)
            {
                changeFlag(fqn, false);
            }
            assertCounts(1);
            for (int cycle = 0; cycle < 10; cycle++)
            {
                enable(false);
                assertCounts(0);
                enable(true);
                assertCounts(1);
            }
            checkRepository.duplicateSettingsProfile(original, alternate, project);
            checkRepository.setActiveSettingsProfile(alternate, List.of(), project);
            waitForDD(getProject());
            enable(false);
            assertCounts(0);
            for (int cycle = 0; cycle < 10; cycle++)
            {
                checkRepository.setActiveSettingsProfile(original, List.of(), project);
                waitForDD(getProject());
                assertCounts(1);
                checkRepository.setActiveSettingsProfile(alternate, List.of(), project);
                waitForDD(getProject());
                assertCounts(0);
            }
        }
        finally
        {
            checkRepository.setActiveSettingsProfile(original, List.of(), project);
            if (checkRepository.getAvailableSettingsProfiles(project).contains(alternate))
            {
                checkRepository.deleteSettingsProfile(alternate, project);
            }
            enable(false);
        }
    }

    private void enable(boolean enabled)
    {
        var settings = checkRepository.getSettings(new CheckUid(DenyIncompleteValuesCheck.CHECK_ID,
            CorePlugin.PLUGIN_ID), getProject().getWorkspaceProject());
        settings.setEnabled(enabled);
        checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
        waitForDD(getProject());
    }

    private static List<RegisterDimension> dimensions(EObject owner)
    {
        return owner.eContents().stream().filter(RegisterDimension.class::isInstance)
            .map(RegisterDimension.class::cast).toList();
    }

    private void changeFlag(String fqn, boolean value)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Change dimension flag") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                dimensions(transaction.getTopObjectByFqn(fqn)).get(0).setDenyIncompleteValues(value);
                return null;
            }
        });
        waitForDD(getProject());
    }
}
