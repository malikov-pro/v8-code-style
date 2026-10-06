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
 *     malikov-pro - port of the АПК rule АПК_00139
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Objects;

import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.metadata.mdclass.BasicFeature;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.md.check.DbObjectStringAllowedLengthCheck;

/**
 * Tests for {@link DbObjectStringAllowedLengthCheck} — the АПК_00139 rule port.
 * A string attribute with the fixed allowed length is reported; variable
 * length and unlimited length string attributes, number attributes and
 * attributes of tabular sections with variable length are clean.
 *
 * @author malikov-pro
 */
public class DbObjectStringAllowedLengthCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String CHECK_ID = "apk-00139-string-attr-length"; //$NON-NLS-1$

    private static final String PROJECT_NAME = "MdStringLength"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * The fixed length string attribute of the object is reported.
     */
    @Test
    public void testFixedStringAttributeReported()
    {
        BasicFeature attribute = objectAttribute("FixedString"); //$NON-NLS-1$
        Marker marker = getFirstMarker(CHECK_ID, attribute.getType(), getProject());
        assertNotNull(marker);
    }

    /**
     * The fixed length string attribute of the tabular section is reported.
     */
    @Test
    public void testFixedStringDetailOfTabularSectionReported()
    {
        BasicFeature attribute = tabularSectionAttribute("FixedDetail"); //$NON-NLS-1$
        Marker marker = getFirstMarker(CHECK_ID, attribute.getType(), getProject());
        assertNotNull(marker);
    }

    /**
     * The variable length string attribute is clean.
     */
    @Test
    public void testVariableStringAttributeIsClean()
    {
        BasicFeature attribute = objectAttribute("VariableString"); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, attribute.getType(), getProject()));
    }

    /**
     * The unlimited length string attribute is not in the scope of this check.
     */
    @Test
    public void testUnlimitedStringAttributeIsClean()
    {
        BasicFeature unlimited = objectAttribute("UnlimitedString"); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, unlimited.getType(), getProject()));

        BasicFeature unlimitedExplicit = objectAttribute("UnlimitedStringExplicit"); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, unlimitedExplicit.getType(), getProject()));
    }

    /**
     * The number attribute and the variable length attribute of the tabular
     * section are clean.
     */
    @Test
    public void testOtherAttributesAreClean()
    {
        BasicFeature number = objectAttribute("NumberAttribute"); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, number.getType(), getProject()));

        BasicFeature detail = tabularSectionAttribute("VariableDetail"); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, detail.getType(), getProject()));
    }

    private BasicFeature objectAttribute(String name)
    {
        IBmObject object = getTopObjectByFqn("Catalog.Test", getProject()); //$NON-NLS-1$
        assertTrue(object instanceof Catalog);

        return ((Catalog)object).getAttributes()
            .stream()
            .filter(attribute -> Objects.equals(name, attribute.getName()))
            .findFirst()
            .orElseThrow();
    }

    private BasicFeature tabularSectionAttribute(String name)
    {
        IBmObject object = getTopObjectByFqn("Catalog.Test", getProject()); //$NON-NLS-1$
        assertTrue(object instanceof Catalog);

        return ((Catalog)object).getTabularSections()
            .stream()
            .flatMap(section -> section.getAttributes().stream())
            .filter(attribute -> Objects.equals(name, attribute.getName()))
            .findFirst()
            .orElseThrow();
    }
}
