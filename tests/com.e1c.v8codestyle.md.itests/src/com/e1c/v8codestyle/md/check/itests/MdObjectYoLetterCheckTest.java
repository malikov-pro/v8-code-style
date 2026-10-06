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
 *     malikov-pro - port of the АПК rule АПК_00126
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.metadata.mdclass.BasicFeature;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.md.check.MdObjectYoLetterCheck;

/**
 * Tests for {@link MdObjectYoLetterCheck} — the АПК_00126 rule port.
 * The letter "ё" (in any case) in the name, synonym (in every language)
 * or comment of a metadata object is reported; attributes are checked as well.
 *
 * @author malikov-pro
 */
public class MdObjectYoLetterCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String CHECK_ID = MdObjectYoLetterCheck.CHECK_ID;

    private static final String PROJECT_NAME = "MdYoLetter"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * The object name with the letter "ё" is reported.
     */
    @Test
    public void testNameContainsYoLetter()
    {
        assertSingleMarker("Catalog.СправочникСЁвИмени"); //$NON-NLS-1$

        BasicFeature attribute = attribute("РеквизитСЁвИмени"); //$NON-NLS-1$
        assertSingleMarker(attribute);
    }

    /**
     * The synonym with the letter "ё" in any case and in any language
     * is reported.
     */
    @Test
    public void testSynonymContainsYoLetter()
    {
        assertSingleMarker("Catalog.СправочникДляСинонима"); //$NON-NLS-1$
        assertSingleMarker("Catalog.СправочникДляПрописной"); //$NON-NLS-1$
        assertSingleMarker("Catalog.CatalogYoInEnglish"); //$NON-NLS-1$

        BasicFeature attribute = attribute("РеквизитДляСинонима"); //$NON-NLS-1$
        assertSingleMarker(attribute);
    }

    /**
     * The comment with the letter "ё" is reported.
     */
    @Test
    public void testCommentContainsYoLetter()
    {
        assertSingleMarker("Catalog.СправочникДляКомментария"); //$NON-NLS-1$
    }

    /**
     * Objects without the letter "ё" are clean.
     */
    @Test
    public void testCleanObjects()
    {
        IDtProject dtProject = getProject();
        assertNotNull(dtProject);

        long id = getTopObjectIdByFqn("Catalog.ОбычныйСправочник", dtProject); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, id, dtProject));

        BasicFeature attribute = attribute("ОбычныйРеквизит"); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, attribute, dtProject));
    }

    private void assertSingleMarker(String fqn)
    {
        long id = getTopObjectIdByFqn(fqn, getProject());
        List<Marker> markers = getMarkersByCheckIds(Set.of(CHECK_ID), id, getProject());
        assertEquals(fqn, 1, markers.size());
    }

    private void assertSingleMarker(EObject object)
    {
        List<Marker> markers = getMarkersByCheckIds(Set.of(CHECK_ID), object, getProject());
        assertEquals(1, markers.size());
    }

    private BasicFeature attribute(String name)
    {
        Catalog catalog = (Catalog)getTopObjectByFqn("Catalog.ОбычныйСправочник", getProject()); //$NON-NLS-1$
        return catalog.getAttributes()
            .stream()
            .filter(item -> name.equals(item.getName()))
            .findFirst()
            .orElseThrow();
    }
}
