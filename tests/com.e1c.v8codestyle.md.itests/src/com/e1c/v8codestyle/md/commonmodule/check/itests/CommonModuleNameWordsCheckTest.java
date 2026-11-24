/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.md.commonmodule.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.junit.Before;
import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.testing.check.CheckTestBase;
import com.e1c.v8codestyle.internal.md.CorePlugin;
import com.e1c.v8codestyle.md.commonmodule.check.CommonModuleNameWordsCheck;

/** Metadata-only delivery, exact name anchors and source-independent parameter updates. */
public class CommonModuleNameWordsCheckTest extends CheckTestBase
{
    private static final String ORIGINAL_NAME = "CommonModuleName"; //$NON-NLS-1$
    private static final CheckUid UID = new CheckUid(CommonModuleNameWordsCheck.CHECK_ID, CorePlugin.PLUGIN_ID);
    private IDtProject project;
    private String currentName;

    @Before
    public void setUp() throws Exception
    {
        project = openProjectAndWaitForValidationFinish("CommonModuleName"); //$NON-NLS-1$
        currentName = ORIGINAL_NAME;
    }

    @Test
    public void testDefaultsAndEveryAlternativeWithoutBslText()
    {
        var defaults = checkRepository.getDefaultSettings(UID, project.getWorkspaceProject());
        assertTrue(defaults.isEnabled());
        assertEquals(IssueSeverity.TRIVIAL, defaults.getSeverity());
        assertEquals(1, defaults.getParameters().size());
        assertEquals(CommonModuleNameWordsCheck.DEFAULT_WORDS,
            defaults.getParameters().get(CommonModuleNameWordsCheck.WORDS).getValue());
        assertFalse(project.getWorkspaceProject().getFile(
            "src/CommonModules/CommonModuleName/Module.bsl").exists()); //$NON-NLS-1$
        try
        {
            for (String word : CommonModuleNameWordsCheck.DEFAULT_WORDS.split("\\|")) //$NON-NLS-1$
            {
                rename("Prefix" + word.toUpperCase(java.util.Locale.ROOT) + "Suffix"); //$NON-NLS-1$ //$NON-NLS-2$
                assertCount(1);
                rename("ApplicationServices"); //$NON-NLS-1$
                assertCount(0);
            }
        }
        finally
        {
            rename(ORIGINAL_NAME);
        }
    }

    @Test
    public void testCustomEmptyAndInvalidRegexWithoutSourceChanges()
    {
        List<IStatus> warnings = new CopyOnWriteArrayList<>();
        ILogListener listener = (status, plugin) -> {
            if (CorePlugin.PLUGIN_ID.equals(plugin) && status.getMessage().contains(CommonModuleNameWordsCheck.CHECK_ID))
            {
                warnings.add(status);
            }
        };
        CorePlugin.getDefault().getLog().addLogListener(listener);
        try
        {
            words("^Services$"); //$NON-NLS-1$
            assertCount(0);
            words("name$"); //$NON-NLS-1$
            assertCount(1);
            words("^Services$"); //$NON-NLS-1$
            assertCount(0);
            words(""); //$NON-NLS-1$
            assertCount(1);
            words("["); //$NON-NLS-1$
            assertCount(0);
            assertFalse("Invalid regex must log a warning", warnings.isEmpty()); //$NON-NLS-1$
            assertTrue(warnings.toString(), warnings.stream().allMatch(s -> s.getSeverity() == IStatus.WARNING));
            words(CommonModuleNameWordsCheck.DEFAULT_WORDS);
            assertCount(1);
        }
        finally
        {
            CorePlugin.getDefault().getLog().removeLogListener(listener);
            words(CommonModuleNameWordsCheck.DEFAULT_WORDS);
            rename(ORIGINAL_NAME);
        }
    }

    @Test
    public void testParameterAfterRenameKeepsConfigurationReference()
    {
        try
        {
            rename("ApplicationServices"); //$NON-NLS-1$
            assertCount(0);
            bmModelManager.getModel(project).execute(new AbstractBmTask<Void>("Check configuration reference") //$NON-NLS-1$
            {
                @Override
                public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
                {
                    var configuration = (Configuration)transaction.getTopObjectByFqn("Configuration"); //$NON-NLS-1$
                    assertTrue(configuration.getCommonModules().toString(), configuration.getCommonModules().stream()
                        .anyMatch(module -> !module.eIsProxy() && currentName.equals(module.getName())));
                    return null;
                }
            });
            words("services$"); //$NON-NLS-1$
            assertCount(1);
            words(CommonModuleNameWordsCheck.DEFAULT_WORDS);
            assertCount(0);
        }
        finally
        {
            words(CommonModuleNameWordsCheck.DEFAULT_WORDS);
            rename(ORIGINAL_NAME);
        }
    }

    @Test
    public void testOffOnAndManualSeverity()
    {
        try
        {
            assertCount(1);
            for (int cycle = 0; cycle < 3; cycle++)
            {
                enabled(false);
                assertCount(0);
                enabled(true);
                assertCount(1);
            }
            var settings = checkRepository.getSettings(UID, project.getWorkspaceProject());
            settings.setSeverity(IssueSeverity.MAJOR);
            checkRepository.applyChanges(List.of(settings), project.getWorkspaceProject());
            waitForDD(project);
            rename("UtilityFunctions"); //$NON-NLS-1$
            assertCount(1);
            assertEquals(IssueSeverity.MAJOR, checkRepository.getSettings(UID,
                project.getWorkspaceProject()).getSeverity());
        }
        finally
        {
            var settings = checkRepository.getSettings(UID, project.getWorkspaceProject());
            settings.setSeverity(IssueSeverity.TRIVIAL);
            settings.setEnabled(true);
            checkRepository.applyChanges(List.of(settings), project.getWorkspaceProject());
            waitForDD(project);
            rename(ORIGINAL_NAME);
        }
    }

    @Test
    public void testProfileSwitchWithoutSourceChanges() throws Exception
    {
        var workspaceProject = project.getWorkspaceProject();
        String original = checkRepository.getActiveSettingsProfile(workspaceProject);
        String alternate = "CommonModuleWordsOff"; //$NON-NLS-1$
        try
        {
            assertCount(1);
            checkRepository.duplicateSettingsProfile(original, alternate, workspaceProject);
            checkRepository.setActiveSettingsProfile(alternate, List.of(), workspaceProject);
            waitForDD(project);
            enabled(false);
            assertCount(0);
            for (int cycle = 0; cycle < 3; cycle++)
            {
                checkRepository.setActiveSettingsProfile(original, List.of(), workspaceProject);
                waitForDD(project);
                assertCount(1);
                checkRepository.setActiveSettingsProfile(alternate, List.of(), workspaceProject);
                waitForDD(project);
                assertCount(0);
            }
        }
        finally
        {
            checkRepository.setActiveSettingsProfile(original, List.of(), workspaceProject);
            checkRepository.deleteSettingsProfile(alternate, workspaceProject);
            waitForDD(project);
        }
    }

    private void words(String value)
    {
        var settings = checkRepository.getSettings(UID, project.getWorkspaceProject());
        settings.getParameters().get(CommonModuleNameWordsCheck.WORDS).setValue(value);
        checkRepository.applyChanges(List.of(settings), project.getWorkspaceProject());
        waitForDD(project);
    }

    private void enabled(boolean value)
    {
        var settings = checkRepository.getSettings(UID, project.getWorkspaceProject());
        settings.setEnabled(value);
        checkRepository.applyChanges(List.of(settings), project.getWorkspaceProject());
        waitForDD(project);
    }

    private void rename(String name)
    {
        bmModelManager.getModel(project).execute(new AbstractBmTask<Void>("Rename common module") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var module = (CommonModule)transaction.getTopObjectByFqn("CommonModule." + currentName); //$NON-NLS-1$
                var configuration = (Configuration)transaction.getTopObjectByFqn("Configuration"); //$NON-NLS-1$
                // A raw FQN update does not refactor the Configuration's URI reference.
                // Keep the fixture valid, as EDT's real Rename refactoring does.
                configuration.getCommonModules().remove(module);
                module.setName(name);
                transaction.updateTopObjectFqn((IBmObject)module, "CommonModule." + name); //$NON-NLS-1$
                configuration.getCommonModules().add(module);
                return null;
            }
        });
        currentName = name;
        waitForDD(project);
    }

    private void assertCount(int expected)
    {
        long id = getTopObjectIdByFqn("CommonModule." + currentName, project); //$NON-NLS-1$
        var markers = getMarkersByCheckIds(Set.of(CommonModuleNameWordsCheck.CHECK_ID), null, project,
            markerManager.getNestedMarkers(project.getWorkspaceProject(), id));
        assertEquals(currentName + ": " + markers, expected, markers.size()); //$NON-NLS-1$
        for (var marker : markers)
        {
            assertEquals(id, marker.getMarkerObjectId());
            assertEquals(MdClassPackage.MD_OBJECT__NAME, marker.getFeatureId());
            assertTrue(marker.getMessage(), marker.getMessage().contains(currentName));
        }
    }
}
