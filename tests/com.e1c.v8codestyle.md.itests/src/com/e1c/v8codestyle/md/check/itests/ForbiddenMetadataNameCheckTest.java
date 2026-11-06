/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import org.junit.Test;

import com.e1c.v8codestyle.md.check.ForbiddenMetadataNameCheck;

/** Reserved top/field names on real metadata, including objects without BSL. */
public class ForbiddenMetadataNameCheckTest extends MetadataNamesTestBase
{
    private static final String CHECK = ForbiddenMetadataNameCheck.CHECK_ID;

    @Test
    public void testAllSixteenTopKindsAndWholeNameMatching()
    {
        for (String fqn : OWNERS)
        {
            String type = fqn.substring(0, fqn.indexOf('.'));
            String original = fqn.substring(fqn.indexOf('.') + 1);
            int baseline = fqn.startsWith("DocumentJournal.") ? 1 : 0; //$NON-NLS-1$
            assertCount(CHECK, fqn, baseline);
            try
            {
                renameTop(fqn, "cAtAlOg"); //$NON-NLS-1$
                assertCount(CHECK, type + ".cAtAlOg", baseline + 1); //$NON-NLS-1$
                renameTop(type + ".cAtAlOg", "CatalogValue"); //$NON-NLS-1$ //$NON-NLS-2$
                assertCount(CHECK, type + ".CatalogValue", baseline); //$NON-NLS-1$
            }
            finally
            {
                // All assertions before restoration use real names, not stale FQN aliases.
                var bad = getTopObjectByFqn(type + ".cAtAlOg", project); //$NON-NLS-1$
                renameTop(type + (bad != null ? ".cAtAlOg" : ".CatalogValue"), original); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    @Test
    public void testAccountingFlagAdditionAndRemoval()
    {
        for (String feature : java.util.List.of("accountingFlags", "extDimensionAccountingFlags"))
        {
            try
            {
                change("ChartOfAccounts.Accounts", owner -> {
                    var reference = (org.eclipse.emf.ecore.EReference)owner.eClass().getEStructuralFeature(feature);
                    var flag = (com._1c.g5.v8.dt.metadata.mdclass.BasicFeature)
                        reference.getEReferenceType().getEPackage().getEFactoryInstance()
                            .create(reference.getEReferenceType());
                    flag.setName("Catalog");
                    var existing = (com._1c.g5.v8.dt.metadata.mdclass.BasicFeature)child(owner, feature, 0);
                    flag.setType(org.eclipse.emf.ecore.util.EcoreUtil.copy(existing.getType()));
                    @SuppressWarnings("unchecked")
                    var flags = (java.util.List<org.eclipse.emf.ecore.EObject>)owner.eGet(reference);
                    flags.add(flag);
                });
                assertCount(CHECK, "ChartOfAccounts.Accounts", 1);
            }
            finally
            {
                change("ChartOfAccounts.Accounts", owner -> {
                    var list = (java.util.List<?>)owner.eGet(owner.eClass().getEStructuralFeature(feature));
                    list.removeIf(f -> f instanceof com._1c.g5.v8.dt.metadata.mdclass.MdObject field
                        && "Catalog".equals(field.getName()));
                });
            }
            assertCount(CHECK, "ChartOfAccounts.Accounts", 0);
        }
    }

    @Test
    public void testAllSixtyFourNamesAndCleanSuffixes()
    {
        String names = "AccountingRegister|AccountingRegisters|AccumulationRegister|AccumulationRegisters"
            + "|BusinessProcess|BusinessProcesses|CalculationRegister|CalculationRegisters|Catalog|Catalogs"
            + "|ChartOfAccounts|ChartOfCalculationTypes|ChartOfCharacteristicTypes|ChartsOfAccounts"
            + "|ChartsOfCalculationTypes|ChartsOfCharacteristicTypes|Constant|Constants|Document|DocumentJournal"
            + "|DocumentJournals|Documents|Enum|Enums|ExchangePlan|ExchangePlans|FilterCriteria|FilterCriterion"
            + "|InformationRegister|InformationRegisters|Task|Tasks|БизнесПроцесс|БизнесПроцессы|Документ|Документы"
            + "|ЖурналДокументов|ЖурналыДокументов|Задача|Задачи|Константа|Константы|КритерииОтбора|КритерийОтбора"
            + "|Перечисление|Перечисления|ПланВидовРасчета|ПланВидовХарактеристик|ПланОбмена|ПланСчетов"
            + "|ПланыВидовРасчета|ПланыВидовХарактеристик|ПланыОбмена|ПланыСчетов|РегистрБухгалтерии"
            + "|РегистрНакопления|РегистрРасчета|РегистрСведений|РегистрыБухгалтерии|РегистрыНакопления"
            + "|РегистрыРасчета|РегистрыСведений|Справочник|Справочники";
        org.junit.Assert.assertEquals(64, names.split("\\|").length);
        try
        {
            for (String name : names.split("\\|"))
            {
                change("Catalog.Products", owner -> child(owner, "attributes", 0).setName(name));
                assertCount(CHECK, "Catalog.Products", 1);
                change("Catalog.Products", owner -> child(owner, "attributes", 0).setName(name + "Value"));
                assertCount(CHECK, "Catalog.Products", 0);
            }
        }
        finally
        {
            change("Catalog.Products", owner -> child(owner, "attributes", 0).setName("Products"));
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
                change("InformationRegister.Prices", owner -> child(owner, feature, 0).setName("Document"));
                assertCount(CHECK, "InformationRegister.Prices", 1);
                change("InformationRegister.Prices", owner -> child(owner, feature, 0).setName(original));
                assertCount(CHECK, "InformationRegister.Prices", 0);
            }
            finally
            {
                change("InformationRegister.Prices", owner -> child(owner, feature, 0).setName(original));
            }
        }
        for (String feature : java.util.List.of("accountingFlags", "extDimensionAccountingFlags"))
        {
            String original = feature.equals("accountingFlags") ? "Flag" : "ExtFlag";
            try
            {
                change("ChartOfAccounts.Accounts", owner -> child(owner, feature, 0).setName("Документ"));
                assertCount(CHECK, "ChartOfAccounts.Accounts", 1);
            }
            finally
            {
                change("ChartOfAccounts.Accounts", owner -> child(owner, feature, 0).setName(original));
            }
            assertCount(CHECK, "ChartOfAccounts.Accounts", 0);
        }
    }

    @Test
    public void testJournalRepeatedOwnerOnlyRenameKeepsColumnMarker()
    {
        for (int cycle = 0; cycle < 5; cycle++)
        {
            assertCount(CHECK, "DocumentJournal.Journal", 1); //$NON-NLS-1$
            try
            {
                renameTop("DocumentJournal.Journal", "Catalog"); //$NON-NLS-1$ //$NON-NLS-2$
                assertCount(CHECK, "DocumentJournal.Catalog", 2); //$NON-NLS-1$
            }
            finally
            {
                renameTop("DocumentJournal.Catalog", "Journal"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            assertCount(CHECK, "DocumentJournal.Journal", 1); //$NON-NLS-1$
        }
    }

    @Test
    public void testChildAndNestedFieldRenameCycles()
    {
        String fqn = "Catalog.Products"; //$NON-NLS-1$
        try
        {
            change(fqn, owner -> child(owner, "attributes", 0).setName("Документ")); //$NON-NLS-1$ //$NON-NLS-2$
            assertCount(CHECK, fqn, 1);
            change(fqn, owner -> child(owner, "attributes", 0).setName("DocumentValue")); //$NON-NLS-1$ //$NON-NLS-2$
            assertCount(CHECK, fqn, 0);
            change(fqn, owner -> child(owner, "tabularSections", 0).setName("Catalog")); //$NON-NLS-1$ //$NON-NLS-2$
            assertCount(CHECK, fqn, 1);
            change(fqn, owner -> child(child(owner, "tabularSections", 0), "attributes", 0).setName("Documents")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertCount(CHECK, fqn, 2);
        }
        finally
        {
            change(fqn, owner -> {
                child(owner, "attributes", 0).setName("Products"); //$NON-NLS-1$ //$NON-NLS-2$
                var section = child(owner, "tabularSections", 0); //$NON-NLS-1$
                section.setName("Lines"); //$NON-NLS-1$
                child(section, "attributes", 0).setName("Lines"); //$NON-NLS-1$ //$NON-NLS-2$
            });
        }
    }
}
