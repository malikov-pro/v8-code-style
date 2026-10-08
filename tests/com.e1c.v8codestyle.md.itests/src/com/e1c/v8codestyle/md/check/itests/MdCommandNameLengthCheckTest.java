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
 *     malikov-pro - port of the АПК rule АПК_00528
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.Test;

import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.metadata.mdclass.BasicCommand;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.CommonCommand;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.md.check.MdCommandNameLengthCheck;

/**
 * Tests for {@link MdCommandNameLengthCheck} — the АПК_00528 rule port.
 * A command (or common command) with a synonym (or name, when the synonym
 * is empty) longer than 38 characters that is available in the command
 * interface is reported. Commands of subsystems with the "Include in
 * command interface" flag not set in some subsystem of the chain up to the
 * root, commands of objects not included in any subsystem and commands of
 * form command groups are clean.
 *
 * @author malikov-pro
 */
public class MdCommandNameLengthCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String CHECK_ID = MdCommandNameLengthCheck.CHECK_ID;

    private static final String PROJECT_NAME = "CommandNameLength"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * A command of an object included in a subsystem with the "Include in
     * command interface" flag set is reported by the long synonym and by
     * the long name when the synonym is empty.
     */
    @Test
    public void testLongCommandOfObjectIsReported()
    {
        assertSingleMarker("LongCommand", command("Catalog.CiCatalog", "LongCommand")); //$NON-NLS-1$ //$NON-NLS-2$
        assertSingleMarker("WithoutSynonym", command("Catalog.CiCatalog", //$NON-NLS-1$ //$NON-NLS-2$
            "CommandWithoutSynonymButWithAVeryLongName")); //$NON-NLS-1$
    }

    /**
     * A common command included in a subsystem with the "Include in command
     * interface" flag set is reported by the long synonym and by the long
     * name when the synonym is empty.
     */
    @Test
    public void testLongCommonCommandIsReported()
    {
        assertSingleMarker("LongCommonCommand", commonCommand("CommonCommand.LongCommonCommand")); //$NON-NLS-1$ //$NON-NLS-2$
        assertSingleMarker("WithoutSynonym", //$NON-NLS-1$
            commonCommand("CommonCommand.CommonCommandWithAVeryLongNameWithoutSynonym")); //$NON-NLS-1$
    }

    /**
     * A command of an object included in a nested subsystem is reported when
     * the whole chain of subsystems up to the root has the "Include in
     * command interface" flag set.
     */
    @Test
    public void testCommandOfNestedSubsystemIsReported()
    {
        assertSingleMarker("LongNestedCiCommand", command("Catalog.NestedCiCatalog", "LongNestedCiCommand")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * A short command, a command without a synonym and with a short name and
     * a command whose group is a form command group are clean.
     */
    @Test
    public void testShortAndFormGroupCommandsAreClean()
    {
        assertClean(command("Catalog.CiCatalog", "ShortCommand")); //$NON-NLS-1$
        assertClean(command("Catalog.CiCatalog", "ShortNoSynonym")); //$NON-NLS-1$
        assertClean(command("Catalog.CiCatalog", "FormGroupLongCommand")); //$NON-NLS-1$
    }

    /**
     * A long command is clean when its object is included in a subsystem
     * with the "Include in command interface" flag not set: in the root
     * subsystem, in a nested subsystem or in no subsystem at all.
     */
    @Test
    public void testCommandsOutsideCommandInterfaceAreClean()
    {
        assertClean(command("Catalog.NoCiCatalog", "LongNoCiCommand")); //$NON-NLS-1$
        assertClean(command("Catalog.NestedNoCiCatalog", "LongNestedNoCiCommand")); //$NON-NLS-1$
        assertClean(command("Catalog.RootNoCiCatalog", "LongRootNoCiCommand")); //$NON-NLS-1$
        assertClean(command("Catalog.LonelyCatalog", "LongLonelyCommand")); //$NON-NLS-1$
    }

    /**
     * A long common command included only in a subsystem with the "Include
     * in command interface" flag not set is clean.
     */
    @Test
    public void testCommonCommandOutsideCommandInterfaceIsClean()
    {
        assertClean(commonCommand("CommonCommand.NoCiCommonCommand")); //$NON-NLS-1$
    }

    private void assertSingleMarker(String name, EObject command)
    {
        List<?> markers = getMarkersByCheckIds(Set.of(CHECK_ID), command, getProject());
        assertEquals(name, 1, markers.size());
    }

    private void assertClean(EObject command)
    {
        assertNull(((BasicCommand)command).getName(), getFirstMarker(CHECK_ID, command, getProject()));
    }

    private BasicCommand command(String catalogFqn, String name)
    {
        IBmObject object = getTopObjectByFqn(catalogFqn, getProject());
        assertNotNull(catalogFqn, object);
        return ((Catalog)object).getCommands()
            .stream()
            .filter(command -> Objects.equals(name, command.getName()))
            .findFirst()
            .orElseThrow();
    }

    private CommonCommand commonCommand(String fqn)
    {
        IBmObject object = getTopObjectByFqn(fqn, getProject());
        if (!(object instanceof CommonCommand))
        {
            throw new IllegalStateException("Not a common command: " + fqn); //$NON-NLS-1$
        }
        return (CommonCommand)object;
    }
}
