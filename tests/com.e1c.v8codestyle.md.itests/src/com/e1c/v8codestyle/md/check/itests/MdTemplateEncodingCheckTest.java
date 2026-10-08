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
 *     malikov-pro - port of the АПК rule АПК_00506
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.md.check.MdTemplateEncodingCheck;

/**
 * Tests for {@link MdTemplateEncodingCheck} — the АПК_00506 rule port.
 * A text or HTML template whose content declares an encoding other than
 * utf-8 is reported; utf-8 and templates without an encoding declaration
 * are clean.
 *
 * @author malikov-pro
 */
public class MdTemplateEncodingCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String CHECK_ID = MdTemplateEncodingCheck.CHECK_ID;

    private static final String PROJECT_NAME = "MdTemplateEncoding"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * A text template declaring windows-1251 is reported.
     */
    @Test
    public void testTextTemplateWithNonUtf8Encoding()
    {
        assertSingleMarker("CommonTemplate.МакетWindows1251"); //$NON-NLS-1$
    }

    /**
     * Text templates with the utf-8 declaration and without any encoding
     * declaration are clean.
     */
    @Test
    public void testCleanTextTemplates()
    {
        assertClean("CommonTemplate.МакетUtf8"); //$NON-NLS-1$
        assertClean("CommonTemplate.МакетБезКодировки"); //$NON-NLS-1$
    }

    /**
     * An HTML template declaring windows-1251 is reported; an HTML template
     * with the utf-8 declaration is clean.
     */
    @Test
    public void testHtmlTemplates()
    {
        assertSingleMarker("CommonTemplate.МакетHtml1251"); //$NON-NLS-1$
        assertClean("CommonTemplate.МакетHtmlUtf8"); //$NON-NLS-1$
    }

    /**
     * Template types other than text and HTML are out of the scope.
     */
    @Test
    public void testOtherTemplateTypesAreClean()
    {
        assertClean("CommonTemplate.МакетДвоичный"); //$NON-NLS-1$
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
}
