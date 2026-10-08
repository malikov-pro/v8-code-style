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
 *     malikov-pro - port of the АПК rule АПК_00505
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.metadata.mdclass.BasicTemplate;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.md.check.MdTemplateLangPostfixCheck;

/**
 * Tests for {@link MdTemplateLangPostfixCheck} — the АПК_00505 rule port.
 * The configuration default language code is "ru" in the test project.
 * Binary data and HTML templates must have the "_ru" name postfix; text and
 * other template types are clean; templates of applied objects are checked
 * as well.
 *
 * @author malikov-pro
 */
public class MdTemplateLangPostfixCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String CHECK_ID = MdTemplateLangPostfixCheck.CHECK_ID;

    private static final String PROJECT_NAME = "MdTemplatePostfix"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * A binary data template without the default language postfix is reported;
     * a template with the "_ru" postfix is clean.
     */
    @Test
    public void testBinaryDataTemplates()
    {
        assertSingleMarker("CommonTemplate.БинарныйМакет"); //$NON-NLS-1$
        assertClean("CommonTemplate.БинарныйМакет_ru"); //$NON-NLS-1$
    }

    /**
     * An HTML template without the default language postfix is reported;
     * a template with the "_ru" postfix is clean.
     */
    @Test
    public void testHtmlTemplates()
    {
        assertSingleMarker("CommonTemplate.HtmlМакет"); //$NON-NLS-1$
        assertClean("CommonTemplate.HtmlМакет_ru"); //$NON-NLS-1$
    }

    /**
     * Template types that do not require separate preparation for translation
     * are clean without the postfix.
     */
    @Test
    public void testOtherTemplateTypesAreClean()
    {
        assertClean("CommonTemplate.ТекстовыйМакет"); //$NON-NLS-1$
    }

    /**
     * Templates of an applied object are checked as well.
     */
    @Test
    public void testTemplatesOfAppliedObject()
    {
        Catalog catalog = (Catalog)getTopObjectByFqn("Catalog.ВладелецМакетов", getProject()); //$NON-NLS-1$
        assertNotNull(catalog);

        BasicTemplate bad = template(catalog, "МакетКаталога"); //$NON-NLS-1$
        List<Marker> markers = getMarkersByCheckIds(Set.of(CHECK_ID), bad, getProject());
        assertEquals(1, markers.size());

        BasicTemplate good = template(catalog, "МакетКаталога_ru"); //$NON-NLS-1$
        markers = getMarkersByCheckIds(Set.of(CHECK_ID), good, getProject());
        assertEquals(0, markers.size());
    }

    private void assertSingleMarker(String fqn)
    {
        long id = getTopObjectIdByFqn(fqn, getProject());
        List<Marker> markers = getMarkersByCheckIds(Set.of(CHECK_ID), id, getProject());
        assertEquals(fqn, 1, markers.size());
    }

    private void assertClean(String fqn)
    {
        long id = getTopObjectIdByFqn(fqn, getProject());
        List<Marker> markers = getMarkersByCheckIds(Set.of(CHECK_ID), id, getProject());
        assertEquals(fqn, 0, markers.size());
    }

    private BasicTemplate template(Catalog catalog, String name)
    {
        return catalog.getTemplates()
            .stream()
            .filter(item -> name.equals(item.getName()))
            .findFirst()
            .orElseThrow();
    }
}
