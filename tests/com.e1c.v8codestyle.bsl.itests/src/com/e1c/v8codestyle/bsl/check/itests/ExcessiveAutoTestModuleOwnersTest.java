/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegister;
import com.e1c.v8codestyle.bsl.check.ExcessiveAutoTestCheck;

/** Real model-owner dispatch and incremental source edits, not synthetic type mutation. */
public class ExcessiveAutoTestModuleOwnersTest extends AbstractSingleModuleTestBase
{
    private String moduleFile = "/src/CommonModules/CommonModule/Module.bsl"; //$NON-NLS-1$

    public ExcessiveAutoTestModuleOwnersTest()
    {
        super(ExcessiveAutoTestCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "AutoTestModules"; //$NON-NLS-1$
    }

    @Override
    protected String getModuleFileName()
    {
        return moduleFile;
    }

    @Test
    public void testActualOwnersAndManagerExclusion() throws Exception
    {
        var catalog = (Catalog)getTopObjectByFqn("Catalog.Products", getProject()); //$NON-NLS-1$
        var register = (InformationRegister)getTopObjectByFqn("InformationRegister.Prices", getProject()); //$NON-NLS-1$
        var form = (Form)getTopObjectByFqn("CommonForm.Probe.Form", getProject()); //$NON-NLS-1$
        List<Module> owners = List.of(getModule(), catalog.getObjectModule(), register.getRecordSetModule(),
            form.getModule(), catalog.getManagerModule());
        List<ModuleType> types = List.of(ModuleType.COMMON_MODULE, ModuleType.OBJECT_MODULE,
            ModuleType.RECORDSET_MODULE, ModuleType.FORM_MODULE, ModuleType.MANAGER_MODULE);
        List<String> files = List.of("/src/CommonModules/CommonModule/Module.bsl", //$NON-NLS-1$
            "/src/Catalogs/Products/ObjectModule.bsl", "/src/InformationRegisters/Prices/RecordSetModule.bsl", //$NON-NLS-1$ //$NON-NLS-2$
            "/src/CommonForms/Probe/Module.bsl", "/src/Catalogs/Products/ManagerModule.bsl"); //$NON-NLS-1$ //$NON-NLS-2$
        try
        {
            for (int i = 0; i < owners.size(); i++)
            {
                assertNotNull(owners.get(i));
                assertEquals(types.get(i), owners.get(i).getModuleType());
                moduleFile = files.get(i);
                int count = i == 4 ? 0 : 11;
                updateModule(FOLDER_RESOURCE + "excessive-auto-test-check.bsl"); //$NON-NLS-1$
                assertEquals(moduleFile, count, getModuleMarkers().size());
                updateModule(FOLDER_RESOURCE + "excessive-auto-test-check-clean.bsl"); //$NON-NLS-1$
                assertEquals(moduleFile, 0, getModuleMarkers().size());
                updateModule(FOLDER_RESOURCE + "excessive-auto-test-check.bsl"); //$NON-NLS-1$
                assertEquals(moduleFile, count, getModuleMarkers().size());
            }
        }
        finally
        {
            setCheckEnable(false);
        }
    }
}
