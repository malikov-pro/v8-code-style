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
 *     malikov-pro - port of the АПК rule АПК_00134
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.Test;

import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.metadata.mdclass.BasicFeature;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.StandardAttribute;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.md.check.MdAttributeHintMatchSynonymCheck;

/**
 * Tests for {@link MdAttributeHintMatchSynonymCheck} — the АПК_00134 rule
 * port. An attribute whose hint matches its synonym is reported; attributes
 * with an empty hint, attributes whose owner is not included in a subsystem
 * with the "Include in command interface" flag and the standard attribute
 * "Period" of a non-periodic information register are clean.
 *
 * @author malikov-pro
 */
public class MdAttributeHintMatchSynonymCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String CHECK_ID = MdAttributeHintMatchSynonymCheck.CHECK_ID;

    private static final String PROJECT_NAME = "MdAttributeHint"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * The hint that matches the synonym (including case-insensitive match)
     * is reported.
     */
    @Test
    public void testHintMatchesSynonymIsReported()
    {
        assertSingleMarker("MatchAttr", catalogAttribute("Catalog.HintCatalog", "MatchAttr")); //$NON-NLS-1$ //$NON-NLS-2$
        assertSingleMarker("CaseAttr", catalogAttribute("Catalog.HintCatalog", "CaseAttr")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * The hint that differs from the synonym is clean; the top object itself
     * has no hint and is clean.
     */
    @Test
    public void testHintDiffersFromSynonymIsClean()
    {
        assertNull(getFirstMarker(CHECK_ID, catalogAttribute("Catalog.HintCatalog", "DifferentHintAttr"), getProject())); //$NON-NLS-1$ //$NON-NLS-2$

        long topId = getTopObjectIdByFqn("Catalog.HintCatalog", getProject()); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, topId, getProject()));
    }

    /**
     * An attribute with an empty hint is not checked.
     */
    @Test
    public void testEmptyHintIsClean()
    {
        assertNull(getFirstMarker(CHECK_ID, catalogAttribute("Catalog.HintCatalog", "EmptyHintAttr"), getProject())); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * The attribute of the tabular section is checked via the owner object
     * of the tabular section.
     */
    @Test
    public void testTabularSectionAttributeIsChecked()
    {
        assertSingleMarker("MatchDetail", tabularAttribute("MatchDetail")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Of standard attributes only Code, Description, Parent, Owner and
     * Period are checked: the Code whose hint matches the synonym is
     * reported, the Description with a different hint is clean and the Ref
     * is out of the whitelist of standard attributes.
     */
    @Test
    public void testStandardAttributesAreChecked()
    {
        Catalog hintCatalog = catalog("Catalog.HintCatalog"); //$NON-NLS-1$

        assertSingleMarker("Code", standardAttribute(hintCatalog.getStandardAttributes(), "Code")); //$NON-NLS-1$ //$NON-NLS-2$

        assertNull(getFirstMarker(CHECK_ID, standardAttribute(hintCatalog.getStandardAttributes(), "Description"), //$NON-NLS-1$
            getProject()));
        assertNull(getFirstMarker(CHECK_ID, standardAttribute(hintCatalog.getStandardAttributes(), "Ref"), //$NON-NLS-1$
            getProject()));
    }

    /**
     * An attribute of an object included only in a subsystem with the
     * "Include in command interface" flag set to false is clean.
     */
    @Test
    public void testObjectWithoutCommandInterfaceSubsystemIsClean()
    {
        assertNull(getFirstMarker(CHECK_ID, catalogAttribute("Catalog.HiddenCatalog", "MatchHiddenAttr"), getProject())); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * The standard attribute "Period" of a periodic information register is
     * checked, of a non-periodic register is not checked.
     */
    @Test
    public void testPeriodStandardAttributeOfRegisters()
    {
        IBmObject periodic = getTopObjectByFqn("InformationRegister.PeriodicReg", getProject()); //$NON-NLS-1$
        assertSingleMarker("Period", //$NON-NLS-1$
            standardAttribute(((InformationRegister)periodic).getStandardAttributes(), "Period")); //$NON-NLS-1$

        IBmObject nonPeriodic = getTopObjectByFqn("InformationRegister.NonPeriodicReg", getProject()); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID,
            standardAttribute(((InformationRegister)nonPeriodic).getStandardAttributes(), "Period"), getProject())); //$NON-NLS-1$
    }

    private void assertSingleMarker(String name, EObject attribute)
    {
        List<?> markers = getMarkersByCheckIds(Set.of(CHECK_ID), attribute, getProject());
        assertEquals(name, 1, markers.size());
    }

    private Catalog catalog(String fqn)
    {
        IBmObject object = getTopObjectByFqn(fqn, getProject());
        if (!(object instanceof Catalog))
        {
            throw new IllegalStateException("Not a catalog: " + fqn); //$NON-NLS-1$
        }
        return (Catalog)object;
    }

    private BasicFeature catalogAttribute(String catalogFqn, String name)
    {
        return catalog(catalogFqn).getAttributes()
            .stream()
            .filter(attribute -> Objects.equals(name, attribute.getName()))
            .findFirst()
            .orElseThrow();
    }

    private BasicFeature tabularAttribute(String name)
    {
        return catalog("Catalog.HintCatalog").getTabularSections() //$NON-NLS-1$
            .stream()
            .flatMap(section -> section.getAttributes().stream())
            .filter(attribute -> Objects.equals(name, attribute.getName()))
            .findFirst()
            .orElseThrow();
    }

    private StandardAttribute standardAttribute(List<StandardAttribute> attributes, String name)
    {
        return attributes.stream()
            .filter(attribute -> Objects.equals(name, attribute.getName()))
            .findFirst()
            .orElseThrow();
    }
}
