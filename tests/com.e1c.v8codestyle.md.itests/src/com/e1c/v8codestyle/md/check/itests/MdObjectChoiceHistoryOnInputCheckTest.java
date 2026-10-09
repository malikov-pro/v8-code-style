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
 *     malikov-pro - port of the АПК rule АПК_00616
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.md.check.MdObjectChoiceHistoryOnInputCheck;

/**
 * Tests for {@link MdObjectChoiceHistoryOnInputCheck} — the АПК_00616 rule port.
 * The "History choice on input" property must be "Don't use": for every document and
 * for every catalog/document whose manager module contains the data selection
 * processing handler.
 *
 * @author malikov-pro
 */
public class MdObjectChoiceHistoryOnInputCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String CHECK_ID = MdObjectChoiceHistoryOnInputCheck.CHECK_ID;

    private static final String PROJECT_NAME = "InputHistoryOnInput"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * Every document with the property other than "Don't use" is reported,
     * whether it has the manager module handler or not.
     */
    @Test
    public void testDocumentWithAutoHistoryIsReported()
    {
        assertSingleMarker("Document.ДокументАвто"); //$NON-NLS-1$
        assertSingleMarker("Document.ДокументСОбработчикомАвто"); //$NON-NLS-1$
    }

    /**
     * A document with the property "Don't use" is clean, with or without the handler.
     */
    @Test
    public void testDocumentWithDontUseHistoryIsClean()
    {
        assertNoMarkers("Document.ДокументПорядок"); //$NON-NLS-1$
        assertNoMarkers("Document.ДокументСОбработчикомПорядок"); //$NON-NLS-1$
    }

    /**
     * A catalog is reported only when its manager module contains the data selection
     * processing handler and the property is not "Don't use".
     */
    @Test
    public void testCatalogWithHandlerIsReportedOnlyWithoutDontUse()
    {
        assertSingleMarker("Catalog.КаталогСОбработчикомАвто"); //$NON-NLS-1$
        assertNoMarkers("Catalog.КаталогСОбработчикомПорядок"); //$NON-NLS-1$
    }

    /**
     * A catalog without the handler in the manager module is clean even with "Auto".
     */
    @Test
    public void testCatalogWithoutHandlerIsClean()
    {
        assertNoMarkers("Catalog.КаталогБезОбработчикаАвто"); //$NON-NLS-1$
    }

    private void assertSingleMarker(String fqn)
    {
        IDtProject dtProject = getProject();
        assertNotNull(dtProject);

        long id = getTopObjectIdByFqn(fqn, dtProject);
        List<Marker> markers = getMarkersByCheckIds(Set.of(CHECK_ID), id, dtProject);
        assertEquals(fqn, 1, markers.size());
    }

    private void assertNoMarkers(String fqn)
    {
        IDtProject dtProject = getProject();
        assertNotNull(dtProject);

        long id = getTopObjectIdByFqn(fqn, dtProject);
        assertNull(fqn, getFirstMarker(CHECK_ID, id, dtProject));
    }
}
