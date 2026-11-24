/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import org.junit.Test;

import com.e1c.v8codestyle.md.check.SameMetadataObjectAndChildNamesCheck;

/** Owner-only and field-only changes, with stable field identities. */
public class SameMetadataObjectAndChildNamesCheckTest extends MetadataNamesTestBase
{
    private static final String CHECK = SameMetadataObjectAndChildNamesCheck.CHECK_ID;

    @Test
    public void testTwelveOwnerKindsAndFourExclusions()
    {
        for (int i = 0; i < OWNERS.size(); i++)
        {
            String fqn = OWNERS.get(i);
            int expected = i < 12 ? (i == 0 ? 2 : 1) : 0;
            assertCount(CHECK, fqn, expected);
            if (i >= 12)
            {
                continue;
            }
            String name = fqn.substring(fqn.indexOf('.') + 1);
            try
            {
                change(fqn, owner -> child(owner, "attributes", 0).setName("DifferentField")); //$NON-NLS-1$ //$NON-NLS-2$
                assertCount(CHECK, fqn, expected - 1);
                change(fqn, owner -> child(owner, "attributes", 0).setName(name.toUpperCase(java.util.Locale.ROOT))); //$NON-NLS-1$
                assertCount(CHECK, fqn, expected);
            }
            finally
            {
                change(fqn, owner -> child(owner, "attributes", 0).setName(name)); //$NON-NLS-1$
            }
        }
    }

    @Test
    public void testDimensionsResourcesAndAccountingFlags()
    {
        for (String feature : java.util.List.of("dimensions", "resources"))
        {
            String original = feature.equals("dimensions") ? "PriceKey" : "PriceValue";
            try
            {
                change("InformationRegister.Prices", owner -> child(owner, feature, 0).setName("PRICES"));
                assertCount(CHECK, "InformationRegister.Prices", 2);
            }
            finally
            {
                change("InformationRegister.Prices", owner -> child(owner, feature, 0).setName(original));
            }
            assertCount(CHECK, "InformationRegister.Prices", 1);
        }
        for (String feature : java.util.List.of("accountingFlags", "extDimensionAccountingFlags"))
        {
            String original = feature.equals("accountingFlags") ? "Flag" : "ExtFlag";
            try
            {
                change("ChartOfAccounts.Accounts", owner -> child(owner, feature, 0).setName("ACCOUNTS"));
                assertCount(CHECK, "ChartOfAccounts.Accounts", 2);
            }
            finally
            {
                change("ChartOfAccounts.Accounts", owner -> child(owner, feature, 0).setName(original));
            }
            assertCount(CHECK, "ChartOfAccounts.Accounts", 1);
        }
    }

    @Test
    public void testOwnerOnlyAndSectionOnlyRename()
    {
        try
        {
            assertCount(CHECK, "Catalog.Products", 2); //$NON-NLS-1$
            renameTop("Catalog.Products", "Goods"); //$NON-NLS-1$ //$NON-NLS-2$
            assertCount(CHECK, "Catalog.Goods", 1); //$NON-NLS-1$
            change("Catalog.Goods", owner -> child(owner, "tabularSections", 0).setName("Goods")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertCount(CHECK, "Catalog.Goods", 0); //$NON-NLS-1$
            change("Catalog.Goods", owner -> child(owner, "tabularSections", 0).setName("LINES")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertCount(CHECK, "Catalog.Goods", 1); //$NON-NLS-1$
        }
        finally
        {
            if (getTopObjectByFqn("Catalog.Goods", project) != null) //$NON-NLS-1$
            {
                renameTop("Catalog.Goods", "Products"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            change("Catalog.Products", owner -> child(owner, "tabularSections", 0).setName("Lines")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        assertCount(CHECK, "Catalog.Products", 2); //$NON-NLS-1$
    }
}
