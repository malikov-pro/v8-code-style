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
 *     malikov-pro - implementation of the upstream issue 1C-Company/v8-code-style#757
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.LockWithoutLockCallCheck;

/**
 * Tests for {@link LockWithoutLockCallCheck} check.
 *
 * @author malikov-pro
 */
public class LockWithoutLockCallCheckTest
    extends AbstractSingleModuleTestBase
{

    /**
     * Test {@link LockWithoutLockCallCheck}.
     */
    public LockWithoutLockCallCheckTest()
    {
        super(LockWithoutLockCallCheck.class);
    }

    /**
     * Lock created and filled with items, but the Lock() method is never called:
     * one issue per each violating lock variable.
     */
    @Test
    public void testLockWithoutLockCall() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "up-757-lock-without-lock-call.bsl");

        List<Marker> markers = getModuleMarkers();
        assertEquals(2, markers.size());
        for (Marker marker : markers)
        {
            assertEquals(Messages.LockWithoutLockCall_Lock_method_is_not_called, marker.getMessage());
        }
    }

    /**
     * Lock() is called, or no items are added, or the variable is not a data lock: no issues.
     */
    @Test
    public void testLockWithoutLockCallClean() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "up-757-lock-without-lock-call-clean.bsl");

        List<Marker> markers = getModuleMarkers();
        assertEquals(0, markers.size());
    }
}
