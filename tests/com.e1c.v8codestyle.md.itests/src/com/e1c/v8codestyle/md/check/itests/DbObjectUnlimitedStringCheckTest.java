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
 *     malikov-pro - port of the АПК rule АПК_00141
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Objects;

import org.junit.Before;
import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmObject;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com._1c.g5.v8.dt.metadata.mdclass.BasicFeature;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.internal.md.CorePlugin;
import com.e1c.v8codestyle.md.check.DbObjectUnlimitedStringCheck;

/**
 * Tests for {@link DbObjectUnlimitedStringCheck} — the АПК_00141 rule port.
 * The check is an audit and disabled by default, so the test enables it
 * explicitly. An unlimited length string attribute of an object or of a
 * tabular section is reported; limited string attributes and number
 * attributes are clean.
 *
 * @author malikov-pro
 */
public class DbObjectUnlimitedStringCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String CHECK_ID = "apk-00141-unlimited-string"; //$NON-NLS-1$

    private static final String PROJECT_NAME = "MdStringLengthUnlimited"; //$NON-NLS-1$

    /**
     * The check is disabled by default (opt-in audit), enable it for the test.
     */
    @Before
    public void enableCheck()
    {
        CheckUid checkUid = new CheckUid(CHECK_ID, CorePlugin.PLUGIN_ID);
        ICheckSettings settings =
            checkRepository.getSettings(checkUid, getProject().getWorkspaceProject());
        if (!settings.isEnabled())
        {
            settings.setEnabled(true);
            checkRepository.applyChanges(Collections.singleton(settings), getProject().getWorkspaceProject());
            waitForDD(getProject());
        }
    }

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * The unlimited length string attribute of the object is reported.
     */
    @Test
    public void testUnlimitedStringAttributeReported()
    {
        BasicFeature attribute = objectAttribute("UnlimitedString"); //$NON-NLS-1$
        Marker marker = getFirstMarker(CHECK_ID, attribute.getType(), getProject());
        assertNotNull(marker);
    }

    /**
     * The unlimited length string attribute with explicit zero length is
     * reported.
     */
    @Test
    public void testUnlimitedStringExplicitAttributeReported()
    {
        BasicFeature attribute = objectAttribute("UnlimitedStringExplicit"); //$NON-NLS-1$
        Marker marker = getFirstMarker(CHECK_ID, attribute.getType(), getProject());
        assertNotNull(marker);
    }

    /**
     * The unlimited length string attribute of the tabular section is reported.
     */
    @Test
    public void testUnlimitedStringDetailOfTabularSectionReported()
    {
        BasicFeature attribute = tabularSectionAttribute("UnlimitedDetail"); //$NON-NLS-1$
        Marker marker = getFirstMarker(CHECK_ID, attribute.getType(), getProject());
        assertNotNull(marker);
    }

    /**
     * The limited length string attribute is clean.
     */
    @Test
    public void testLimitedStringAttributeIsClean()
    {
        BasicFeature fixed = objectAttribute("FixedString"); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, fixed.getType(), getProject()));

        BasicFeature variable = objectAttribute("VariableString"); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, variable.getType(), getProject()));

        BasicFeature detail = tabularSectionAttribute("VariableDetail"); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, detail.getType(), getProject()));
    }

    /**
     * The number attribute is clean.
     */
    @Test
    public void testNumberAttributeIsClean()
    {
        BasicFeature number = objectAttribute("NumberAttribute"); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, number.getType(), getProject()));
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
