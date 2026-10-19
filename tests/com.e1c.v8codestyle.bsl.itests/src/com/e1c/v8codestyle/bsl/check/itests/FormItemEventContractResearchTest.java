/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.resource.BslEventsService;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.mcore.Event;
import com.e1c.v8codestyle.bsl.check.EventHandlerInvalidSignatureCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Research: discover actual bound platform contracts, not guessed event-name arities. */
public class FormItemEventContractResearchTest extends AbstractSingleModuleTestBase
{
    public FormItemEventContractResearchTest()
    {
        super(EventHandlerInvalidSignatureCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "SelfReferenceCheck"; //$NON-NLS-1$
    }

    @Override
    protected String getModuleFileName()
    {
        return "/src/CommonForms/ItemContracts/Module.bsl"; //$NON-NLS-1$
    }

    @Override
    protected Module getModule()
    {
        return ((Form)getTopObjectByFqn("CommonForm.ItemContracts.Form", getProject())).getModule(); //$NON-NLS-1$
    }

    @Test
    public void testBoundItemAndCommandContracts()
    {
        var events = BslPlugin.getDefault().getInjector().getInstance(BslEventsService.class);
        var handlers = events.getEventHandlers(getModule());
        for (var entry : handlers.entrySet())
        {
            for (var value : entry.getValue())
            {
                assertTrue(value instanceof Event);
                var event = (Event)value;
                var sets = event.getParamSet();
                assertEquals(1, sets.size());
                var set = sets.get(0);
                assertEquals(0, set.getMinParams());
                if (entry.getKey().getString().equals("FieldChange")) //$NON-NLS-1$
                {
                    assertEquals("OnChange", event.getName()); //$NON-NLS-1$
                    assertEquals(0, set.getMaxParams());
                    assertEquals(0, set.getParams().size());
                }
                else if (entry.getKey().getString().equals("FieldChoice")) //$NON-NLS-1$
                {
                    assertEquals("StartChoice", event.getName()); //$NON-NLS-1$
                    assertEquals(2, set.getMaxParams());
                    assertEquals(2, set.getParams().size());
                }
                else if (entry.getKey().getString().equals("CommandAction")) //$NON-NLS-1$
                {
                    assertEquals("Action", event.getName()); //$NON-NLS-1$
                    assertEquals(0, set.getMaxParams());
                    assertEquals(1, set.getParams().size());
                    assertFalse(set.getParams().get(0).isDefaultValue());
                }
            }
        }
        assertTrue(handlers.keySet().stream().anyMatch(key -> key.getString().equals("FieldChange"))); //$NON-NLS-1$
        assertTrue(handlers.keySet().stream().anyMatch(key -> key.getString().equals("FieldChoice"))); //$NON-NLS-1$
        assertTrue(handlers.keySet().stream().anyMatch(key -> key.getString().equals("CommandAction"))); //$NON-NLS-1$
        assertFalse(handlers.values().stream().flatMap(List::stream).anyMatch(value -> value.eIsProxy()));
        assertEquals(0, events.getAllModuleEvents(getModule()).size());
    }
}
