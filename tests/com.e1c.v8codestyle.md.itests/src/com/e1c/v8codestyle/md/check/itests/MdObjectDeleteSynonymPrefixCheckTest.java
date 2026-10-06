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
 *     malikov-pro - port of the АПК rule АПК_00156
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
import com.e1c.v8codestyle.md.check.MdObjectDeleteSynonymPrefixCheck;

/**
 * Tests for {@link MdObjectDeleteSynonymPrefixCheck} — the АПК_00156 rule port.
 * An object whose name starts with "Удалить"/"Delete" must have a synonym that
 * starts with "(не используется)", and vice versa. Objects with an empty
 * synonym are clean.
 *
 * @author malikov-pro
 */
public class MdObjectDeleteSynonymPrefixCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String CHECK_ID = MdObjectDeleteSynonymPrefixCheck.CHECK_ID;

    private static final String PROJECT_NAME = "MdDeleteSynonym"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * The name with the "Удалить"/"Delete" prefix and the synonym without the
     * "(не используется)" prefix is reported.
     */
    @Test
    public void testDeleteNameWithoutSynonymPrefix()
    {
        assertSingleMarker("Catalog.DeleteOldCatalog");
        assertSingleMarker("Catalog.DeleteEnglishCatalog");
    }

    /**
     * The synonym with the "(не используется)" prefix and the name without the
     * "Удалить" prefix is reported.
     */
    @Test
    public void testSynonymPrefixWithoutDeleteName()
    {
        assertSingleMarker("Catalog.SynonymPrefixOnlyCatalog");
    }

    /**
     * The pair of prefixes in the name and in the synonym is clean, an object
     * with an empty synonym is clean.
     */
    @Test
    public void testPairedPrefixesAndEmptySynonymAreClean()
    {
        for (String fqn : List.of("Catalog.УдалитьАрхивныйСправочник", "Catalog.EmptySynonymCatalog")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            long id = getTopObjectIdByFqn(fqn, getProject());
            assertNull(fqn, getFirstMarker(CHECK_ID, id, getProject()));
        }
    }

    /**
     * Attributes of the object and of the tabular section are checked as well;
     * a proper attribute is clean.
     */
    @Test
    public void testAttributesAreChecked()
    {
        Catalog catalog = (Catalog)getTopObjectByFqn("Catalog.OkPlainCatalog", getProject()); //$NON-NLS-1$

        BasicFeature badAttribute = attribute(catalog, "УдалитьСтарыйРеквизит"); //$NON-NLS-1$
        assertSingleObjectMarker(badAttribute);

        BasicFeature badDetail = tabularSectionAttribute(catalog, "УдалитьСтараяДеталь"); //$NON-NLS-1$
        assertSingleObjectMarker(badDetail);

        BasicFeature okAttribute = attribute(catalog, "ХорошийРеквизит"); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, okAttribute, getProject()));

        long topId = getTopObjectIdByFqn("Catalog.OkPlainCatalog", getProject()); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, topId, getProject()));
    }

    private void assertSingleMarker(String fqn)
    {
        long id = getTopObjectIdByFqn(fqn, getProject());
        List<Marker> markers = getMarkersByCheckIds(Set.of(CHECK_ID), id, getProject());
        assertEquals(fqn, 1, markers.size());
    }

    private void assertSingleObjectMarker(EObject object)
    {
        List<Marker> markers = getMarkersByCheckIds(Set.of(CHECK_ID), object, getProject());
        assertEquals(1, markers.size());
    }

    private BasicFeature attribute(Catalog catalog, String name)
    {
        return catalog.getAttributes()
            .stream()
            .filter(item -> name.equals(item.getName()))
            .findFirst()
            .orElseThrow();
    }

    private BasicFeature tabularSectionAttribute(Catalog catalog, String name)
    {
        return catalog.getTabularSections()
            .stream()
            .flatMap(section -> section.getAttributes().stream())
            .filter(item -> name.equals(item.getName()))
            .findFirst()
            .orElseThrow();
    }
}
