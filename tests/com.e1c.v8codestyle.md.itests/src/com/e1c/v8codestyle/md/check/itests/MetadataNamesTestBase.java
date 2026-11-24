/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.junit.Before;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com.e1c.g5.v8.dt.testing.check.CheckTestBase;

/** Shared fixture operations, keeping root references valid across top-object renames. */
public abstract class MetadataNamesTestBase extends CheckTestBase
{
    protected static final List<String> OWNERS = List.of("Catalog.Products", "Document.Invoice", //$NON-NLS-1$ //$NON-NLS-2$
        "InformationRegister.Prices", "AccumulationRegister.Stock", "AccountingRegister.Balances", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        "CalculationRegister.Payroll", "BusinessProcess.Approval", "ChartOfAccounts.Accounts", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        "ChartOfCalculationTypes.Accruals", "ChartOfCharacteristicTypes.Properties", "ExchangePlan.Nodes", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        "Task.Work", "Constant.Setting", "DocumentJournal.Journal", "Enum.States", "FilterCriterion.Selection"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

    protected IDtProject project;

    @Before
    public void setUp() throws Exception
    {
        project = openProjectAndWaitForValidationFinish("MetadataNames"); //$NON-NLS-1$
    }

    protected void change(String fqn, Consumer<MdObject> edit)
    {
        bmModelManager.getModel(project).execute(new AbstractBmTask<Void>("Change metadata name") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var owner = (MdObject)transaction.getTopObjectByFqn(fqn);
                assertNotNull(fqn, owner);
                edit.accept(owner);
                return null;
            }
        });
        waitForDD(project);
    }

    protected void renameTop(String fqn, String name)
    {
        bmModelManager.getModel(project).execute(new AbstractBmTask<Void>("Rename metadata owner") //$NON-NLS-1$
        {
            @Override
            @SuppressWarnings("unchecked")
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                var object = (MdObject)transaction.getTopObjectByFqn(fqn);
                var configuration = transaction.getTopObjectByFqn("Configuration"); //$NON-NLS-1$
                var reference = configuration.eClass().getEAllReferences().stream()
                    .filter(r -> !r.isContainment() && r.isMany() && r.getEReferenceType().isSuperTypeOf(object.eClass()))
                    .filter(r -> ((List<?>)configuration.eGet(r)).contains(object)).findFirst().orElseThrow();
                var list = (List<EObject>)configuration.eGet(reference);
                int index = list.indexOf(object);
                list.remove(index);
                object.setName(name);
                transaction.updateTopObjectFqn((IBmObject)object, object.eClass().getName() + "." + name); //$NON-NLS-1$
                list.add(index, object);
                return null;
            }
        });
        waitForDD(project);
    }

    protected static MdObject child(MdObject owner, String feature, int index)
    {
        return (MdObject)((List<?>)owner.eGet(owner.eClass().getEStructuralFeature(feature))).get(index);
    }

    protected void assertCount(String check, String fqn, int count)
    {
        var owner = getTopObjectByFqn(fqn, project);
        assertNotNull(fqn, owner);
        var markers = getMarkersByCheckIds(Set.of(check), null, project,
            markerManager.getNestedMarkers(project.getWorkspaceProject(), owner.bmGetId()));
        assertEquals(fqn + " " + markers, count, markers.size()); //$NON-NLS-1$
        for (var marker : markers)
        {
            bmModelManager.getModel(project).execute(new AbstractBmTask<Void>("Verify metadata marker anchor") //$NON-NLS-1$
            {
                @Override
                public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
                {
                    var target = transaction.getObjectById(((Number)marker.getMarkerObjectId()).longValue());
                    assertNotNull(target);
                    assertEquals(target.eClass().getFeatureID(target.eClass().getEStructuralFeature("name")), //$NON-NLS-1$
                        marker.getFeatureId());
                    return null;
                }
            });
        }
    }
}
