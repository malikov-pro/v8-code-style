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
 *     malikov-pro - port of the upstream issue #725 (АПК rule 1215)
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.md.check.MdExtendedListPresentationEqualsCheck;

/**
 * Tests for {@link MdExtendedListPresentationEqualsCheck} — the upstream
 * issue #725 (АПК rule 1215) port. An object whose extended list presentation
 * equals its list presentation (in some language, ignoring case and
 * leading/trailing spaces) is reported; objects with different presentations
 * or with an empty extended list presentation are clean.
 *
 * @author malikov-pro
 */
public class MdExtendedListPresentationEqualsCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String CHECK_ID = MdExtendedListPresentationEqualsCheck.CHECK_ID;

    private static final String PROJECT_NAME = "MdExtListPresentation"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * The extended list presentation that equals the list presentation
     * (including case-insensitive and trailing space match) is reported for
     * a catalog, a document, an information register and an accumulation
     * register.
     */
    @Test
    public void testEqualListPresentationIsReported()
    {
        assertSingleMarker("Catalog.BadListCatalog"); //$NON-NLS-1$
        assertSingleMarker("Document.BadListDoc"); //$NON-NLS-1$
        assertSingleMarker("InformationRegister.BadInfoReg"); //$NON-NLS-1$
        assertSingleMarker("AccumulationRegister.BadAccReg"); //$NON-NLS-1$
    }

    /**
     * The extended list presentation that differs from the list presentation
     * and the empty extended list presentation are clean.
     */
    @Test
    public void testDifferentOrEmptyExtendedListPresentationIsClean()
    {
        assertNull(getFirstMarker(CHECK_ID, getTopObjectIdByFqn("Catalog.OkListCatalog", getProject()), getProject())); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, getTopObjectIdByFqn("Catalog.EmptyExtCatalog", getProject()), getProject())); //$NON-NLS-1$
    }

    private void assertSingleMarker(String fqn)
    {
        Long id = getTopObjectIdByFqn(fqn, getProject());
        List<Marker> markers = getMarkersByCheckIds(Set.of(CHECK_ID), id, getProject());
        assertEquals(fqn, 1, markers.size());
    }
}
