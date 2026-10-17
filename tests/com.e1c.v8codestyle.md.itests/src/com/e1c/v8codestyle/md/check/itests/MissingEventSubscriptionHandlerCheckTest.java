/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com._1c.g5.v8.dt.metadata.mdclass.EventSubscription;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.internal.md.CorePlugin;
import com.e1c.v8codestyle.md.check.MissingEventSubscriptionHandlerCheck;

public class MissingEventSubscriptionHandlerCheckTest extends SingleProjectReadOnlyCheckTestBase
{
    @Override
    protected String getTestConfigurationName()
    {
        return "SubsystemMembership"; //$NON-NLS-1$
    }

    @Test
    public void testReferenceBoundariesAndDefaults() throws Exception
    {
        var settings = checkRepository.getSettings(new CheckUid(MissingEventSubscriptionHandlerCheck.CHECK_ID,
            CorePlugin.PLUGIN_ID), getProject().getWorkspaceProject());
        assertTrue(settings.isEnabled());
        assertEquals(IssueSeverity.CRITICAL, settings.getSeverity());
        assertEquals(0, settings.getParameters().size());
        var module = (CommonModule)getTopObjectByFqn("CommonModule.InSubsystemModule", getProject()); //$NON-NLS-1$
        boolean original = module.isServer();
        var subscription = (EventSubscription)getTopObjectByFqn("EventSubscription.LonelySubscription", getProject()); //$NON-NLS-1$
        String handler = subscription.getHandler();
        try
        {
            changeServer(false);
            for (String reference : List.of("CommonModule.InSubsystemModule.Handle", //$NON-NLS-1$
                "cOmMoNmOdUlE.iNsUbSyStEmMoDuLe.hAnDlE", "ОбщийМодуль.InSubsystemModule.Handle")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                changeReference(reference);
                assertCount(1);
                changeServer(true);
                assertCount(0);
                changeServer(false);
                assertCount(1);
            }
            for (String reference : List.of("", "CommonModule.InSubsystemModule", //$NON-NLS-1$ //$NON-NLS-2$
                "CommonModule.InSubsystemModule.", "CommonModule.Missing.Handle", //$NON-NLS-1$ //$NON-NLS-2$
                "Catalog.InSubsystemModule.Handle", "CommonModule.InSubsystemModule.Handle.Extra")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                changeReference(reference);
                assertCount(0);
            }
            changeReference(null);
            assertCount(0);
            // A different existing server module is clean even when this provider remains non-server.
            changeReference("CommonModule.LonelyModule.Handle"); //$NON-NLS-1$
            assertCount(0);
        }
        finally
        {
            changeServer(original);
            changeReference(handler);
        }
    }

    @Test
    public void testProviderFileOnlyServerCycle() throws Exception
    {
        var file = getProject().getWorkspaceProject()
            .getFile("/src/CommonModules/InSubsystemModule/InSubsystemModule.mdo"); //$NON-NLS-1$
        byte[] original;
        try (var input = file.getContents())
        {
            original = input.readAllBytes();
        }
        String text = new String(original, StandardCharsets.UTF_8);
        String changed = text.replace("<server>true</server>", "<server>false</server>"); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(!text.equals(changed));
        var consumer = getProject().getWorkspaceProject()
            .getFile("/src/EventSubscriptions/LonelySubscription/LonelySubscription.mdo"); //$NON-NLS-1$
        long stamp = consumer.getModificationStamp();
        byte[] before;
        try (var input = consumer.getContents())
        {
            before = input.readAllBytes();
        }
        try
        {
            assertCount(0);
            writeFile(file, changed.getBytes(StandardCharsets.UTF_8));
            assertCount(1);
            writeFile(file, original);
            assertCount(0);
            assertEquals(stamp, consumer.getModificationStamp());
            try (var input = consumer.getContents())
            {
                assertArrayEquals(before, input.readAllBytes());
            }
        }
        finally
        {
            writeFile(file, original);
        }
    }

    @Test
    public void testSubscriptionFileReferenceCycle() throws Exception
    {
        var file = getProject().getWorkspaceProject()
            .getFile("/src/EventSubscriptions/LonelySubscription/LonelySubscription.mdo"); //$NON-NLS-1$
        byte[] original;
        try (var input = file.getContents())
        {
            original = input.readAllBytes();
        }
        String text = new String(original, StandardCharsets.UTF_8);
        String changed = text.replace("CommonModule.InSubsystemModule.Handle", "CommonModule.LonelyModule.Handle"); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(!text.equals(changed));
        var module = (CommonModule)getTopObjectByFqn("CommonModule.InSubsystemModule", getProject()); //$NON-NLS-1$
        boolean server = module.isServer();
        try
        {
            changeServer(false);
            assertCount(1);
            writeFile(file, changed.getBytes(StandardCharsets.UTF_8));
            assertCount(0);
            writeFile(file, original);
            assertCount(1);
        }
        finally
        {
            changeServer(server);
            writeFile(file, original);
        }
    }

    private void writeFile(IFile file, byte[] bytes) throws Exception
    {
        file.setContents(new ByteArrayInputStream(bytes), true, true, new NullProgressMonitor());
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
        waitForDD(getProject());
    }

    private void changeReference(String reference)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Change subscription reference") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                ((EventSubscription)transaction.getTopObjectByFqn("EventSubscription.LonelySubscription")) //$NON-NLS-1$
                    .setHandler(reference);
                return null;
            }
        });
        waitForDD(getProject());
    }

    @Test
    public void testInitiallyNonServerProvider() throws Exception
    {
        var module = (CommonModule)getTopObjectByFqn("CommonModule.InSubsystemModule", getProject()); //$NON-NLS-1$
        boolean original = module.isServer();
        try
        {
            changeServer(false);
            bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Change subscription reference") //$NON-NLS-1$
            {
                @Override
                public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
                {
                    var subscription = (EventSubscription)
                        transaction.getTopObjectByFqn("EventSubscription.LonelySubscription"); //$NON-NLS-1$
                    subscription.setHandler("CommonModule.InSubsystemModule.OtherHandle"); //$NON-NLS-1$
                    return null;
                }
            });
            waitForDD(getProject());
            assertCount(1);
        }
        finally
        {
            changeServer(original);
            bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Restore subscription reference") //$NON-NLS-1$
            {
                @Override
                public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
                {
                    ((EventSubscription)
                        transaction.getTopObjectByFqn("EventSubscription.LonelySubscription")) //$NON-NLS-1$
                        .setHandler("CommonModule.InSubsystemModule.Handle"); //$NON-NLS-1$
                    return null;
                }
            });
            waitForDD(getProject());
        }
    }

    @Test
    public void testProviderOnlyServerFlagCycle() throws Exception
    {
        var module = (CommonModule)getTopObjectByFqn("CommonModule.InSubsystemModule", getProject()); //$NON-NLS-1$
        boolean original = module.isServer();
        var file = getProject().getWorkspaceProject()
            .getFile("/src/EventSubscriptions/LonelySubscription/LonelySubscription.mdo"); //$NON-NLS-1$
        long stamp = file.getModificationStamp();
        try
        {
            changeServer(true);
            assertCount(0);
            changeServer(false);
            assertCount(1);
            changeServer(true);
            assertCount(0);
            assertEquals(stamp, file.getModificationStamp());
        }
        finally
        {
            changeServer(original);
        }
    }

    private void changeServer(boolean server)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Change subscription provider flag") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var module = (CommonModule)transaction.getTopObjectByFqn("CommonModule.InSubsystemModule"); //$NON-NLS-1$
                module.setServer(server);
                return null;
            }
        });
        waitForDD(getProject());
    }

    private void assertCount(int expected)
    {
        var subscription = getTopObjectByFqn("EventSubscription.LonelySubscription", getProject()); //$NON-NLS-1$
        var markers = getMarkersByCheckIds(Set.of(MissingEventSubscriptionHandlerCheck.CHECK_ID),
            subscription, getProject());
        assertEquals(expected, markers.size());
        for (var marker : markers)
        {
            assertEquals(MdClassPackage.EVENT_SUBSCRIPTION__HANDLER, marker.getFeatureId());
            assertTrue(marker.getMessage(), !marker.getMessage().isBlank());
        }
    }
}
