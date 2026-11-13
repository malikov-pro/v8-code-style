/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.e1c.v8codestyle.bsl.check.ServerCallsInFormEventsCheck;

/** Server calls in OnActivateRow; the no-context call is allowed. */
public class ServerCallsInFormEventsCheckTest
    extends AbstractSingleModuleTestBase
{
    public ServerCallsInFormEventsCheckTest()
    {
        super(ServerCallsInFormEventsCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "CommonForm"; //$NON-NLS-1$
    }

    @Override
    protected String getModuleFileName()
    {
        return "/src/CommonForms/Form/Module.bsl"; //$NON-NLS-1$
    }

    @Test
    public void testCorpus() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "server-calls-in-form-events.bsl"); //$NON-NLS-1$
        assertEquals(1, getModuleMarkers().size());
    }

    @Test
    public void testIncrementalUpdates() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "server-calls-in-form-events-clean.bsl"); //$NON-NLS-1$
        assertEquals(0, getModuleMarkers().size());
        updateModule(FOLDER_RESOURCE + "server-calls-in-form-events.bsl"); //$NON-NLS-1$
        assertEquals(1, getModuleMarkers().size());
        updateModule(FOLDER_RESOURCE + "server-calls-in-form-events-clean.bsl"); //$NON-NLS-1$
        assertEquals(0, getModuleMarkers().size());
    }
}
