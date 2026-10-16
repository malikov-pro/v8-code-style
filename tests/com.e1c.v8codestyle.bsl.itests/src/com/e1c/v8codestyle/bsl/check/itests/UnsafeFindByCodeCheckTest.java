/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.CatalogCodesSeries;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfAccounts;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfCharacteristicTypes;
import com._1c.g5.v8.dt.metadata.mdclass.CharOfAccountCodeSeries;
import com._1c.g5.v8.dt.metadata.mdclass.CharacteristicKindCodesSeries;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.UnsafeFindByCodeCheck;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

public class UnsafeFindByCodeCheckTest extends AbstractSingleModuleTestBase
{
    public UnsafeFindByCodeCheckTest()
    {
        super(UnsafeFindByCodeCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "SelfReferenceCheck"; //$NON-NLS-1$
    }

    @Override
    protected String getModuleFileName()
    {
        return "/src/CommonModules/MyCommonModule/Module.bsl"; //$NON-NLS-1$
    }

    @Override
    protected Module getModule()
    {
        return ((CommonModule)getTopObjectByFqn("CommonModule.MyCommonModule", getProject())).getModule(); //$NON-NLS-1$
    }

    @Test
    public void testBoundariesDefaultsAndSourceCycle() throws Exception
    {
        var settings = checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID),
            getProject().getWorkspaceProject());
        org.junit.Assert.assertTrue(settings.isEnabled());
        assertEquals(IssueSeverity.MAJOR, settings.getSeverity());
        assertEquals(0, settings.getParameters().size());
        Catalog catalog = (Catalog)getTopObjectByFqn("Catalog.Products", getProject()); //$NON-NLS-1$
        boolean unique = catalog.isCheckUnique();
        var series = catalog.getCodeSeries();
        try
        {
            change(false, CatalogCodesSeries.WHOLE_CATALOG);
            updateModule(FOLDER_RESOURCE + "unsafe-code-boundaries.bsl"); //$NON-NLS-1$
            assertLines(List.of(2, 7, 9, 10, 17));
            updateModule(FOLDER_RESOURCE + "unsafe-code-clean.bsl"); //$NON-NLS-1$
            assertLines(List.of());
            updateModule(FOLDER_RESOURCE + "unsafe-code-boundaries.bsl"); //$NON-NLS-1$
            assertLines(List.of(2, 7, 9, 10, 17));
            change(true, CatalogCodesSeries.WITHIN_OWNER_SUBORDINATION);
            assertLines(List.of(2, 7, 9, 10, 17));
            change(true, CatalogCodesSeries.WHOLE_CATALOG);
            assertLines(List.of());
        }
        finally
        {
            change(unique, series);
        }
    }

    @Test
    public void testBothChartsMetadataOnly() throws Exception
    {
        var accounts = (ChartOfAccounts)getTopObjectByFqn("ChartOfAccounts.Accounts", getProject()); //$NON-NLS-1$
        var kinds = (ChartOfCharacteristicTypes)getTopObjectByFqn("ChartOfCharacteristicTypes.Kinds", getProject()); //$NON-NLS-1$
        boolean accountUnique = accounts.isCheckUnique();
        boolean kindUnique = kinds.isCheckUnique();
        var accountSeries = accounts.getCodeSeries();
        var kindSeries = kinds.getCodeSeries();
        try
        {
            changeCharts(true, false);
            updateModule(FOLDER_RESOURCE + "unsafe-code-charts.bsl"); //$NON-NLS-1$
            assertLines(List.of());
            var consumer = getProject().getWorkspaceProject().getFile(getModuleFileName());
            long stamp = consumer.getModificationStamp();
            changeCharts(false, false);
            assertLines(List.of(2, 3, 4, 5));
            changeCharts(true, false);
            assertLines(List.of());
            changeCharts(true, true);
            assertLines(List.of(2, 3, 4, 5));
            changeCharts(true, false);
            assertLines(List.of());
            assertEquals(stamp, consumer.getModificationStamp());
        }
        finally
        {
            bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Restore charts") //$NON-NLS-1$
            {
                @Override
                public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
                {
                    var a = (ChartOfAccounts)transaction.getTopObjectByFqn("ChartOfAccounts.Accounts"); //$NON-NLS-1$
                    var k = (ChartOfCharacteristicTypes)transaction.getTopObjectByFqn("ChartOfCharacteristicTypes.Kinds"); //$NON-NLS-1$
                    a.setCheckUnique(accountUnique);
                    a.setCodeSeries(accountSeries);
                    k.setCheckUnique(kindUnique);
                    k.setCodeSeries(kindSeries);
                    return null;
                }
            });
            waitForDD(getProject());
        }
    }

    private void changeCharts(boolean unique, boolean subordinate)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Change chart contracts") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var a = (ChartOfAccounts)transaction.getTopObjectByFqn("ChartOfAccounts.Accounts"); //$NON-NLS-1$
                var k = (ChartOfCharacteristicTypes)transaction.getTopObjectByFqn("ChartOfCharacteristicTypes.Kinds"); //$NON-NLS-1$
                a.setCheckUnique(unique);
                a.setCodeSeries(subordinate ? CharOfAccountCodeSeries.WITHIN_SUBORDINATION
                    : CharOfAccountCodeSeries.WHOLE_CHART_OF_ACCOUNTS);
                k.setCheckUnique(unique);
                k.setCodeSeries(subordinate ? CharacteristicKindCodesSeries.WITHIN_SUBORDINATION
                    : CharacteristicKindCodesSeries.WHOLE_CHARACTERISTIC_KIND);
                return null;
            }
        });
        waitForDD(getProject());
    }

    @Test
    public void testInitiallyUnsafeCatalog() throws Exception
    {
        Catalog catalog = (Catalog)getTopObjectByFqn("Catalog.Products", getProject()); //$NON-NLS-1$
        boolean unique = catalog.isCheckUnique();
        CatalogCodesSeries series = catalog.getCodeSeries();
        try
        {
            change(false, CatalogCodesSeries.WHOLE_CATALOG);
            updateModule(FOLDER_RESOURCE + "unsafe-code-catalog.bsl"); //$NON-NLS-1$
            assertLines(List.of(2, 3));
        }
        finally
        {
            change(unique, series);
        }
    }

    @Test
    public void testMetadataOnlyUniqueAndSeriesCycle() throws Exception
    {
        Catalog catalog = (Catalog)getTopObjectByFqn("Catalog.Products", getProject()); //$NON-NLS-1$
        boolean unique = catalog.isCheckUnique();
        CatalogCodesSeries series = catalog.getCodeSeries();
        try
        {
            change(true, CatalogCodesSeries.WHOLE_CATALOG);
            updateModule(FOLDER_RESOURCE + "unsafe-code-catalog.bsl"); //$NON-NLS-1$
            assertLines(List.of());
            var consumer = getProject().getWorkspaceProject().getFile(getModuleFileName());
            long stamp = consumer.getModificationStamp();
            change(false, CatalogCodesSeries.WHOLE_CATALOG);
            assertLines(List.of(2, 3));
            change(true, CatalogCodesSeries.WHOLE_CATALOG);
            assertLines(List.of());
            change(true, CatalogCodesSeries.WITHIN_SUBORDINATION);
            assertLines(List.of(2, 3));
            change(true, CatalogCodesSeries.WHOLE_CATALOG);
            assertLines(List.of());
            assertEquals(stamp, consumer.getModificationStamp());
        }
        finally
        {
            change(unique, series);
        }
    }

    private void change(boolean unique, CatalogCodesSeries series)
    {
        bmModelManager.getModel(getProject()).execute(new AbstractBmTask<Void>("Change code contract") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                Catalog catalog = (Catalog)transaction.getTopObjectByFqn("Catalog.Products"); //$NON-NLS-1$
                catalog.setCheckUnique(unique);
                catalog.setCodeSeries(series);
                return null;
            }
        });
        waitForDD(getProject());
    }

    private void assertLines(List<Integer> expected)
    {
        for (var leaf : NodeModelUtils.findActualNodeFor(getModule()).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        assertEquals(expected, getModuleMarkers().stream().map(marker -> {
            Integer line = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
            return line;
        }).sorted().toList());
        var node = NodeModelUtils.findActualNodeFor(getModule());
        for (var marker : getModuleMarkers())
        {
            Integer offset = marker.getExtraInfo().get(StandardExtraInfo.TEXT_OFFSET);
            Integer length = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LENGTH);
            String name = node.getRootNode().getText().substring(offset, offset + length);
            org.junit.Assert.assertTrue(name, "НайтиПоКоду".equalsIgnoreCase(name) || "FindByCode".equalsIgnoreCase(name)); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}
