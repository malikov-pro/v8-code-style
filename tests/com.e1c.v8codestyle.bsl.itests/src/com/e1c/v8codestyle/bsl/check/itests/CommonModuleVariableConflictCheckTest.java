/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.CommonModuleVariableConflictCheck;

/** APK availability differs intentionally from native context resolution. */
public class CommonModuleVariableConflictCheckTest extends AbstractSingleModuleTestBase
{
    private String moduleFile = "/src/CommonModules/CommonModule/Module.bsl";

    public CommonModuleVariableConflictCheckTest()
    {
        super(CommonModuleVariableConflictCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "CommonModuleConflicts";
    }

    @Override
    protected String getModuleFileName()
    {
        return moduleFile;
    }

    @Override
    protected Module getModule()
    {
        if (!moduleFile.contains("/CommonModules/"))
        {
            var model = bmModelManager.getModel(getProject());
            return model.execute(new AbstractBmTask<Module>("Read actual module")
            {
                @Override
                public Module execute(IBmTransaction transaction, IProgressMonitor monitor)
                {
                    var uri = org.eclipse.emf.common.util.URI.createPlatformResourceURI(getModuleId(), true)
                        .appendFragment("/");
                    var object = transaction.getExternalObjectByUri(uri);
                    return object instanceof Module module ? module : null;
                }
            });
        }
        return ((CommonModule)getTopObjectByFqn("CommonModule.CommonModule", getProject())).getModule();
    }

    @Test
    public void testCommonModuleContextFlagsAndSyntaxCleanCorpus() throws Exception
    {
        try
        {
            updateModule(FOLDER_RESOURCE + "apk-01218-common-module-variable-conflict-clean.bsl");
            assertCount(0);
            updateModule(FOLDER_RESOURCE + "apk-01218-common-module-variable-conflict.bsl");
            assertCount(2);
            change("CommonModule", m -> m.setServerCall(true));
            assertCount(3);
            change("CommonModule", m -> { m.setServerCall(false); m.setServer(false); m.setClientManagedApplication(true); });
            assertCount(1);
        }
        finally
        {
            change("CommonModule", m -> { m.setServer(true); m.setClientManagedApplication(false); m.setServerCall(false); });
            updateModule(FOLDER_RESOURCE + "apk-01218-common-module-variable-conflict-clean.bsl");
        }
    }

    @Test
    public void testActualServerModuleOwners() throws Exception
    {
        String original = moduleFile;
        try
        {
            for (String file : java.util.List.of("/src/Catalogs/Products/ObjectModule.bsl",
                "/src/Catalogs/Products/ManagerModule.bsl", "/src/InformationRegisters/Prices/RecordSetModule.bsl",
                "/src/Constants/ProbeValue/ValueManagerModule.bsl", "/src/Configuration/SessionModule.bsl",
                "/src/Configuration/OrdinaryApplicationModule.bsl"))
            {
                moduleFile = file;
                updateModule(FOLDER_RESOURCE + "apk-01218-common-module-variable-conflict-clean.bsl");
                assertCount(0);
                updateModule(FOLDER_RESOURCE + "apk-01218-common-module-variable-conflict.bsl");
                assertCount(2);
                updateModule(FOLDER_RESOURCE + "apk-01218-common-module-variable-conflict-clean.bsl");
                assertCount(0);
            }
        }
        finally
        {
            moduleFile = original;
        }
    }

    @Test
    public void testProviderOnlyFlagsWithUnchangedFormalParameterConsumer() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "apk-01218-provider-consumer.bsl");
        var file = getProject().getWorkspaceProject().getFile(getModuleFileName());
        long stamp = file.getModificationStamp();
        try
        {
            assertCount(1);
            change("ServerProvider", m -> { m.setServer(false); m.setClientManagedApplication(true); });
            assertCount(0);
            assertEquals(stamp, file.getModificationStamp());
            change("ServerProvider", m -> { m.setServer(true); m.setClientManagedApplication(false); });
            assertCount(1);
            assertEquals(stamp, file.getModificationStamp());
        }
        finally
        {
            change("ServerProvider", m -> { m.setServer(true); m.setClientManagedApplication(false); });
        }
    }

    @Test
    public void testProviderOnlyRenameAndRemoval() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "apk-01218-provider-consumer.bsl");
        var file = getProject().getWorkspaceProject().getFile(getModuleFileName());
        long stamp = file.getModificationStamp();
        byte[] original;
        try (var input = file.getContents())
        {
            original = input.readAllBytes();
        }
        try
        {
            assertCount(1);
            renameProvider("ServerProvider", "RenamedProvider");
            assertCount(0);
            renameProvider("RenamedProvider", "ServerProvider");
            assertCount(1);
            bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Remove provider")
            {
                @Override
                public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
                {
                    var config = (com._1c.g5.v8.dt.metadata.mdclass.Configuration)
                        transaction.getTopObjectByFqn("Configuration");
                    var provider = (CommonModule)transaction.getTopObjectByFqn("CommonModule.ServerProvider");
                    config.getCommonModules().remove(provider);
                    transaction.detachTopObject((com._1c.g5.v8.bm.core.IBmObject)provider);
                    return null;
                }
            });
            waitForDD(getProject());
            assertCount(0);
        }
        finally
        {
            bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Restore provider")
            {
                @Override
                public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
                {
                    var config = (com._1c.g5.v8.dt.metadata.mdclass.Configuration)
                        transaction.getTopObjectByFqn("Configuration");
                    if (transaction.getTopObjectByFqn("CommonModule.ServerProvider") == null)
                    {
                        var provider = com._1c.g5.v8.dt.metadata.mdclass.MdClassFactory.eINSTANCE.createCommonModule();
                        provider.setName("ServerProvider");
                        provider.setServer(true);
                        transaction.attachTopObject((com._1c.g5.v8.bm.core.IBmObject)provider,
                            "CommonModule.ServerProvider");
                        config.getCommonModules().add(provider);
                    }
                    return null;
                }
            });
            waitForDD(getProject());
        }
        assertCount(1);
        assertEquals(stamp, file.getModificationStamp());
        try (var input = file.getContents())
        {
            org.junit.Assert.assertArrayEquals(original, input.readAllBytes());
        }
    }

    private void renameProvider(String from, String to)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Rename provider only")
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var config = (com._1c.g5.v8.dt.metadata.mdclass.Configuration)
                    transaction.getTopObjectByFqn("Configuration");
                var provider = (CommonModule)transaction.getTopObjectByFqn("CommonModule." + from);
                config.getCommonModules().remove(provider);
                provider.setName(to);
                transaction.updateTopObjectFqn((com._1c.g5.v8.bm.core.IBmObject)provider, "CommonModule." + to);
                config.getCommonModules().add(provider);
                return null;
            }
        });
        waitForDD(getProject());
    }

    @Test
    public void testRealFormCompilerDirectivesAndDefaultParameterExclusion() throws Exception
    {
        try
        {
            moduleFile = "/src/CommonForms/Probe/Module.bsl";
            updateModule(FOLDER_RESOURCE + "apk-01218-form-conflict.bsl");
            var form = (com._1c.g5.v8.dt.form.model.Form)getTopObjectByFqn("CommonForm.Probe.Form", getProject());
            var node = NodeModelUtils.getNode(form.getModule());
            for (var leaf : node.getLeafNodes())
            {
                assertNull(leaf.getSyntaxErrorMessage());
            }
            assertEquals(3, getModuleMarkers().size());
            updateModule(FOLDER_RESOURCE + "apk-01218-form-conflict-ru.bsl");
            assertEquals(3, getModuleMarkers().size());
            updateModule(FOLDER_RESOURCE + "apk-01218-form-conflict-clean.bsl");
            assertEquals(0, getModuleMarkers().size());
            updateModule(FOLDER_RESOURCE + "apk-01218-form-conflict.bsl");
            assertEquals(3, getModuleMarkers().size());
        }
        finally
        {
            moduleFile = "/src/CommonModules/CommonModule/Module.bsl";
        }
    }

    private void change(String name, java.util.function.Consumer<CommonModule> edit)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Change common module flags")
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                edit.accept((CommonModule)transaction.getTopObjectByFqn("CommonModule." + name));
                return null;
            }
        });
        waitForDD(getProject());
    }

    private void assertCount(int count)
    {
        var node = NodeModelUtils.getNode(getModule());
        for (var leaf : node.getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(getModuleMarkers().toString(), count, getModuleMarkers().size());
        for (var marker : getModuleMarkers())
        {
            int offset = (Integer)marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            int length = (Integer)marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            String name = node.getRootNode().getText().substring(offset, offset + length);
            org.junit.Assert.assertTrue(marker.getMessage(), marker.getMessage().contains(name));
        }
    }
}
