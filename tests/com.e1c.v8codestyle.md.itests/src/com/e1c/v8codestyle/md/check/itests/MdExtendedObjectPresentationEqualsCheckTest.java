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
 *     malikov-pro - port of the upstream issue #722 (АПК rule 1211)
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.md.check.MdExtendedObjectPresentationEqualsCheck;

/**
 * Tests for {@link MdExtendedObjectPresentationEqualsCheck} — the upstream
 * issue #722 (АПК rule 1211) port. An object whose extended object
 * presentation equals its object presentation, or whose extended record
 * presentation equals its record presentation (in some language, ignoring
 * case and leading/trailing spaces), is reported; objects with different
 * presentations or with an empty extended presentation are clean.
 *
 * @author malikov-pro
 */
public class MdExtendedObjectPresentationEqualsCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String CHECK_ID = MdExtendedObjectPresentationEqualsCheck.CHECK_ID;

    private static final String PROJECT_NAME = "MdExtObjectPresentation"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * The extended object presentation that equals the object presentation
     * (including case-insensitive and trailing space match) and the extended
     * record presentation that equals the record presentation are reported.
     */
    @Test
    public void testEqualPresentationsAreReported()
    {
        assertSingleMarker("Catalog.BadObjectCatalog"); //$NON-NLS-1$
        assertSingleMarker("InformationRegister.BadRecordReg"); //$NON-NLS-1$
    }

    /**
     * The extended presentations that differ from the plain presentations and
     * the empty extended object presentation are clean.
     */
    @Test
    public void testDifferentOrEmptyExtendedPresentationIsClean()
    {
        assertNull(getFirstMarker(CHECK_ID, getTopObjectIdByFqn("Catalog.OkObjectCatalog", getProject()), getProject())); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID,
            getTopObjectIdByFqn("Catalog.EmptyExtObjectCatalog", getProject()), getProject())); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, getTopObjectIdByFqn("InformationRegister.OkRecordReg", getProject()), getProject())); //$NON-NLS-1$
    }

    private void assertSingleMarker(String fqn)
    {
        Long id = getTopObjectIdByFqn(fqn, getProject());
        List<Marker> markers = getMarkersByCheckIds(Set.of(CHECK_ID), id, getProject());
        assertEquals(fqn, 1, markers.size());
    }
}
