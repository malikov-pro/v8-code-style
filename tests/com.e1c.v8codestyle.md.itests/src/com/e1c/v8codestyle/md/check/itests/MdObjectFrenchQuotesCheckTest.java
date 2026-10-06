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
 *     malikov-pro - port of the АПК rule АПК_01196
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Objects;

import org.junit.Test;

import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com._1c.g5.v8.dt.metadata.mdclass.StandardAttribute;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.md.check.MdObjectFrenchQuotesCheck;

/**
 * Tests for {@link MdObjectFrenchQuotesCheck} — the АПК_01196 rule port.
 * French quotes (guillemets) in the synonym, comment or tooltip of metadata
 * objects are reported; straight double quotes are clean.
 *
 * @author malikov-pro
 */
public class MdObjectFrenchQuotesCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String CHECK_ID = "apk-01196-french-quotes-md"; //$NON-NLS-1$

    private static final String PROJECT_NAME = "MdFrenchQuotes"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * The catalog synonym containing French quotes is reported.
     */
    @Test
    public void testCatalogSynonymReported()
    {
        Catalog object = (Catalog)getTopObjectByFqn("Catalog.SynonymQuotes", getProject()); //$NON-NLS-1$
        assertNotNull(object);

        Marker marker = getFirstMarker(CHECK_ID, object, getProject());
        assertNotNull(marker);
    }

    /**
     * The catalog comment containing French quotes is reported.
     */
    @Test
    public void testCatalogCommentReported()
    {
        Catalog object = (Catalog)getTopObjectByFqn("Catalog.CommentQuotes", getProject()); //$NON-NLS-1$
        assertNotNull(object);

        Marker marker = getFirstMarker(CHECK_ID, object, getProject());
        assertNotNull(marker);
    }

    /**
     * The attribute synonym containing French quotes is reported, the clean attribute is not.
     */
    @Test
    public void testAttributeSynonymReported()
    {
        Catalog catalog = (Catalog)getTopObjectByFqn("Catalog.AttrQuotes", getProject()); //$NON-NLS-1$
        assertNotNull(catalog);

        MdObject bad = attributeByName(catalog, "BadAttr"); //$NON-NLS-1$
        Marker marker = getFirstMarker(CHECK_ID, bad, getProject());
        assertNotNull(marker);

        MdObject ok = attributeByName(catalog, "OkAttr"); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, ok, getProject()));
    }

    /**
     * The synonym of an attribute of the tabular section containing French quotes is reported.
     */
    @Test
    public void testTabularSectionAttributeSynonymReported()
    {
        Catalog catalog = (Catalog)getTopObjectByFqn("Catalog.DetailQuotes", getProject()); //$NON-NLS-1$
        assertNotNull(catalog);

        MdObject bad = catalog.getTabularSections()
            .get(0)
            .getAttributes()
            .stream()
            .filter(attribute -> Objects.equals("BadPart", attribute.getName())) //$NON-NLS-1$
            .findFirst()
            .orElseThrow();
        Marker marker = getFirstMarker(CHECK_ID, bad, getProject());
        assertNotNull(marker);
    }

    /**
     * The tooltip of the catalog command containing French quotes is reported.
     */
    @Test
    public void testCommandTooltipReported()
    {
        Catalog catalog = (Catalog)getTopObjectByFqn("Catalog.CommandQuotes", getProject()); //$NON-NLS-1$
        assertNotNull(catalog);

        MdObject command = catalog.getCommands()
            .stream()
            .filter(cmd -> Objects.equals("BadCmd", cmd.getName())) //$NON-NLS-1$
            .findFirst()
            .orElseThrow();
        Marker marker = getFirstMarker(CHECK_ID, command, getProject());
        assertNotNull(marker);
    }

    /**
     * The synonym of the standard attribute containing French quotes is reported.
     */
    @Test
    public void testStandardAttributeSynonymReported()
    {
        Catalog catalog = (Catalog)getTopObjectByFqn("Catalog.StdAttrQuotes", getProject()); //$NON-NLS-1$
        assertNotNull(catalog);

        StandardAttribute attribute = catalog.getStandardAttributes()
            .stream()
            .filter(std -> Objects.equals("Description", std.getName())) //$NON-NLS-1$
            .findFirst()
            .orElseThrow();
        Marker marker = getFirstMarker(CHECK_ID, attribute, getProject());
        assertNotNull(marker);
    }

    /**
     * The catalog with straight double quotes only is clean.
     */
    @Test
    public void testStraightQuotesAreClean()
    {
        Catalog catalog = (Catalog)getTopObjectByFqn("Catalog.OkQuotes", getProject()); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, catalog, getProject()));
        for (MdObject attribute : catalog.getAttributes())
        {
            assertNull(getFirstMarker(CHECK_ID, attribute, getProject()));
        }
    }

    private MdObject attributeByName(Catalog catalog, String name)
    {
        return catalog.getAttributes()
            .stream()
            .filter(attribute -> Objects.equals(name, attribute.getName()))
            .findFirst()
            .orElseThrow();
    }
}
