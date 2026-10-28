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
 *     malikov-pro - port of the upstream issue #710 (АПК rule 1136)
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
import com.e1c.v8codestyle.md.check.MdToolTipEmptyCheck;

/**
 * Tests for {@link MdToolTipEmptyCheck} — the upstream issue #710 (АПК rule
 * 1136) port. An attribute with a filled synonym and an empty popup hint is
 * reported; attributes with a hint, attributes without a synonym, attributes
 * whose owner is not included in a subsystem with the "Include in command
 * interface" flag and the standard attribute "Period" of a non-periodic
 * information register are clean.
 *
 * @author malikov-pro
 */
public class MdToolTipEmptyCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String CHECK_ID = MdToolTipEmptyCheck.CHECK_ID;

    private static final String PROJECT_NAME = "MdToolTipEmpty"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * An attribute with a synonym and an empty hint, and the standard
     * attribute "Code" with a synonym and an empty hint are reported.
     */
    @Test
    public void testEmptyHintWithSynonymIsReported()
    {
        assertSingleMarker("NoToolTipAttr", catalogAttribute("Catalog.TooltipCatalog", "NoToolTipAttr")); //$NON-NLS-1$ //$NON-NLS-2$

        Catalog catalog = catalog("Catalog.TooltipCatalog"); //$NON-NLS-1$
        assertSingleMarker("Code", standardAttribute(catalog.getStandardAttributes(), "Code")); //$NON-NLS-1$
    }

    /**
     * The attribute of the tabular section is checked via the owner object of
     * the tabular section.
     */
    @Test
    public void testTabularSectionAttributeIsChecked()
    {
        assertSingleMarker("TsNoToolTipAttr", tabularAttribute("TsNoToolTipAttr")); //$NON-NLS-1$
    }

    /**
     * The dimension of the register and the standard attribute "Period" of
     * a periodic register are reported.
     */
    @Test
    public void testRegisterDimensionAndPeriodicPeriodAreReported()
    {
        IBmObject periodic = getTopObjectByFqn("InformationRegister.PeriodicReg", getProject()); //$NON-NLS-1$
        assertSingleMarker("RegDimension",
            ((InformationRegister)periodic).getDimensions()
                .stream()
                .filter(dimension -> Objects.equals("RegDimension", dimension.getName())) //$NON-NLS-1$
                .findFirst()
                .orElseThrow());

        assertSingleMarker("Period",
            standardAttribute(((InformationRegister)periodic).getStandardAttributes(), "Period")); //$NON-NLS-1$
    }

    /**
     * An attribute with a hint, an attribute without a synonym, the standard
     * attribute out of the whitelist and the "Period" of a non-periodic
     * register are clean.
     */
    @Test
    public void testAttributesWithHintOrWithoutSynonymAreClean()
    {
        assertNull(getFirstMarker(CHECK_ID, catalogAttribute("Catalog.TooltipCatalog", "WithToolTipAttr"), getProject())); //$NON-NLS-1$ //$NON-NLS-2$
        assertNull(getFirstMarker(CHECK_ID, catalogAttribute("Catalog.TooltipCatalog", "NoSynonymAttr"), getProject())); //$NON-NLS-1$ //$NON-NLS-2$

        Catalog catalog = catalog("Catalog.TooltipCatalog"); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, standardAttribute(catalog.getStandardAttributes(), "Description"), getProject())); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, standardAttribute(catalog.getStandardAttributes(), "Ref"), getProject())); //$NON-NLS-1$

        IBmObject nonPeriodic = getTopObjectByFqn("InformationRegister.NonPeriodicReg", getProject()); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID,
            standardAttribute(((InformationRegister)nonPeriodic).getStandardAttributes(), "Period"), getProject())); //$NON-NLS-1$
    }

    /**
     * An attribute of an object included only in a subsystem with the
     * "Include in command interface" flag set to false is clean.
     */
    @Test
    public void testObjectWithoutCommandInterfaceSubsystemIsClean()
    {
        assertNull(getFirstMarker(CHECK_ID, catalogAttribute("Catalog.HiddenCatalog", "HiddenNoToolTipAttr"), getProject())); //$NON-NLS-1$ //$NON-NLS-2$
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
        return catalog("Catalog.TooltipCatalog").getTabularSections() //$NON-NLS-1$
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
