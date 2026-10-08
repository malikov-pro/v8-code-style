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
 *     malikov-pro - port of the АПК rule АПК_01214
 *******************************************************************************/
package com.e1c.v8codestyle.form.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormAttribute;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.form.check.ApkDsFieldAliasCheck;

/**
 * Tests for {@link ApkDsFieldAliasCheck} — the АПК_01214 rule port.
 * An alias after the КАК/AS keyword in the query text of a form dynamic
 * list attribute that matches a metadata object class name is reported.
 *
 * @author malikov-pro
 */
public class ApkDsFieldAliasCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String CHECK_ID = "apk-01214-ds-field-alias"; //$NON-NLS-1$

    private static final String PROJECT_NAME = "DsFieldAlias"; //$NON-NLS-1$

    private static final String FQN_FORM = "CommonForm.DsAliasForm.Form"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * The Russian keyword КАК with an alias named after a metadata class is reported once.
     */
    @Test
    public void testBadListReported()
    {
        FormAttribute attribute = attributeByName("BadList"); //$NON-NLS-1$
        List<Marker> markers = markers(attribute);
        assertEquals(1, markers.size());
    }

    /**
     * The English keyword AS with an alias matching a metadata class name
     * case-insensitively is reported once.
     */
    @Test
    public void testMixedListReported()
    {
        FormAttribute attribute = attributeByName("MixedList"); //$NON-NLS-1$
        List<Marker> markers = markers(attribute);
        assertEquals(1, markers.size());
    }

    /**
     * A clean alias is not reported; a keyword with an invalid alias in a query
     * text comment is not reported.
     */
    @Test
    public void testOkListClean()
    {
        FormAttribute attribute = attributeByName("OkList"); //$NON-NLS-1$
        assertEquals(0, markers(attribute).size());
    }

    /**
     * A dynamic list without a query text is not reported.
     */
    @Test
    public void testEmptyListClean()
    {
        FormAttribute attribute = attributeByName("EmptyList"); //$NON-NLS-1$
        assertEquals(0, markers(attribute).size());
    }

    /**
     * An attribute that is not a dynamic list is not reported.
     */
    @Test
    public void testPlainAttributeClean()
    {
        FormAttribute attribute = attributeByName("PlainAttribute"); //$NON-NLS-1$
        assertEquals(0, markers(attribute).size());
    }

    private List<Marker> markers(FormAttribute attribute)
    {
        return getMarkersByCheckIds(Set.of(CHECK_ID), attribute, getProject());
    }

    private FormAttribute attributeByName(String name)
    {
        IBmObject object = getTopObjectByFqn(FQN_FORM, getProject());
        assertTrue(object instanceof Form);
        return ((Form)object).getAttributes()
            .stream()
            .filter(attribute -> Objects.equals(name, attribute.getName()))
            .findFirst()
            .orElseThrow();
    }
}
