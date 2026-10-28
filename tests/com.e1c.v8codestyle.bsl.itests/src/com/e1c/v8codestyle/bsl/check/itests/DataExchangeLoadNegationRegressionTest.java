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
 *     malikov-pro - initial API and implementation
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com.e1c.v8codestyle.bsl.check.EventDataExchangeLoadCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** String mentions must not satisfy the DataExchange.Load requirement. */
public class DataExchangeLoadNegationRegressionTest
    extends AbstractSingleModuleTestBase
{
    public DataExchangeLoadNegationRegressionTest()
    {
        super(EventDataExchangeLoadCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "EventDataExchangeLoadCheck"; //$NON-NLS-1$
    }

    @Override
    protected String getModuleFileName()
    {
        return "/src/Catalogs/Services/ObjectModule.bsl"; //$NON-NLS-1$
    }

    @Override
    protected Module getModule()
    {
        return ((Catalog)getTopObjectByFqn("Catalog.Services", getProject())).getObjectModule(); //$NON-NLS-1$
    }

    /** @throws Exception if module validation fails */
    @Test
    public void testStringMentionsDoNotCountAsLoadChecks() throws Exception
    {
        ICheckSettings settings = checkRepository.getSettings(new CheckUid("data-exchange-load", //$NON-NLS-1$
            BslPlugin.PLUGIN_ID), getProject().getWorkspaceProject());
        String original = settings.getParameters().get("dataExchangeLoadFunctionList").getValue(); //$NON-NLS-1$
        try
        {
            updateModule(FOLDER_RESOURCE + "data-exchange-load-negation-regression.bsl"); //$NON-NLS-1$
            assertEquals(3, getModuleMarkers().size());
            settings.getParameters().get("dataExchangeLoadFunctionList").setValue("CustomLoadCheck"); //$NON-NLS-1$ //$NON-NLS-2$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
            updateModule(FOLDER_RESOURCE + "data-exchange-load-negation-regression.bsl"); //$NON-NLS-1$
            assertEquals(2, getModuleMarkers().size());
        }
        finally
        {
            settings.getParameters().get("dataExchangeLoadFunctionList").setValue(original); //$NON-NLS-1$
            checkRepository.applyChanges(List.of(settings), getProject().getWorkspaceProject());
        }
    }
}
