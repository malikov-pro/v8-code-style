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
 *     malikov-pro - port of the АПК rule АПК_00458
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Set;

import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.md.check.MdSubsystemMembershipCheck;

/**
 * Tests for {@link MdSubsystemMembershipCheck} — the АПК_00458 rule port.
 * Every checked metadata object included in no subsystem of the
 * configuration subsystem tree is reported. Objects included in a root
 * subsystem, in a nested subsystem or in a subsystem without the "Include
 * in command interface" flag are clean; service objects that cannot be
 * included in a subsystem composition are not checked.
 *
 * @author malikov-pro
 */
public class MdSubsystemMembershipCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String CHECK_ID = MdSubsystemMembershipCheck.CHECK_ID;

    private static final String PROJECT_NAME = "SubsystemMembership"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * An object of any checked kind included in the composition of a root
     * subsystem is clean.
     */
    @Test
    public void testObjectsInSubsystemAreClean()
    {
        assertClean("Catalog.InSubsystemCatalog"); //$NON-NLS-1$
        assertClean("Document.InSubsystemDocument"); //$NON-NLS-1$
        assertClean("CommonModule.InSubsystemModule"); //$NON-NLS-1$
        assertClean("CommonCommand.InSubsystemCommand"); //$NON-NLS-1$
        assertClean("Report.InSubsystemReport"); //$NON-NLS-1$
    }

    /**
     * An object included in the composition of a nested subsystem and an
     * object included in a subsystem without the "Include in command
     * interface" flag are clean: any subsystem counts.
     */
    @Test
    public void testObjectsInNestedOrNoCiSubsystemAreClean()
    {
        assertClean("Catalog.InNestedCatalog"); //$NON-NLS-1$
        assertClean("Catalog.InNoCiCatalog"); //$NON-NLS-1$
    }

    /**
     * An object of a checked kind included in no subsystem is reported.
     */
    @Test
    public void testObjectsOutsideSubsystemsAreReported()
    {
        assertSingleMarker("Catalog.LonelyCatalog", "Catalog.LonelyCatalog"); //$NON-NLS-1$ //$NON-NLS-2$
        assertSingleMarker("Document.LonelyDocument", "Document.LonelyDocument"); //$NON-NLS-1$ //$NON-NLS-2$
        assertSingleMarker("CommonModule.LonelyModule", "CommonModule.LonelyModule"); //$NON-NLS-1$ //$NON-NLS-2$
        assertSingleMarker("CommonCommand.LonelyCommonCommand", "CommonCommand.LonelyCommonCommand"); //$NON-NLS-1$ //$NON-NLS-2$
        assertSingleMarker("Report.LonelyReport", "Report.LonelyReport"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Service objects that cannot be included in a subsystem composition are
     * not checked.
     */
    @Test
    public void testServiceObjectsAreNotChecked()
    {
        assertClean("EventSubscription.LonelySubscription"); //$NON-NLS-1$
        assertClean("CommonPicture.LonelyPicture"); //$NON-NLS-1$
        assertClean("SessionParameter.LonelyParameter"); //$NON-NLS-1$
        assertClean("XDTOPackage.LonelyPackage"); //$NON-NLS-1$
        assertClean("CommandGroup.LonelyGroup"); //$NON-NLS-1$
    }

    private void assertSingleMarker(String name, String fqn)
    {
        MdObject object = object(fqn);
        assertEquals(name, 1, getMarkersByCheckIds(Set.of(CHECK_ID), object, getProject()).size());
    }

    private void assertClean(String fqn)
    {
        MdObject object = object(fqn);
        assertNull(fqn, getFirstMarker(CHECK_ID, object, getProject()));
    }

    private MdObject object(String fqn)
    {
        IBmObject object = getTopObjectByFqn(fqn, getProject());
        assertNotNull(fqn, object);
        if (!(object instanceof MdObject))
        {
            throw new IllegalStateException("Not an MdObject: " + fqn); //$NON-NLS-1$
        }
        return (MdObject)object;
    }
}
