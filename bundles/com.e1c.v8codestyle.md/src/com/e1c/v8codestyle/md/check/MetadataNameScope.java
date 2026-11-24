/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;

/** Explicit storage-field scope of the pinned LS metadata naming rules. */
final class MetadataNameScope
{
    private static final MdClassPackage MD = MdClassPackage.eINSTANCE;

    static final List<EClass> SAME_TYPES = List.of(MD.getAccountingRegister(), MD.getAccumulationRegister(),
        MD.getBusinessProcess(), MD.getCalculationRegister(), MD.getCatalog(), MD.getChartOfAccounts(),
        MD.getChartOfCalculationTypes(), MD.getChartOfCharacteristicTypes(), MD.getDocument(),
        MD.getExchangePlan(), MD.getInformationRegister(), MD.getTask());

    static final List<EClass> FORBIDDEN_TYPES;

    static
    {
        List<EClass> types = new ArrayList<>(SAME_TYPES);
        types.addAll(List.of(MD.getConstant(), MD.getDocumentJournal(), MD.getEnum(), MD.getFilterCriterion()));
        FORBIDDEN_TYPES = List.copyOf(types);
    }

    private MetadataNameScope()
    {
    }

    static List<MdObject> attributes(EObject owner)
    {
        List<MdObject> fields = new ArrayList<>();
        for (String feature : List.of("attributes", "resources", "dimensions", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "accountingFlags", "extDimensionAccountingFlags", "columns")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        {
            fields.addAll(children(owner, feature));
        }
        return fields;
    }

    static List<MdObject> sections(EObject owner)
    {
        return children(owner, "tabularSections"); //$NON-NLS-1$
    }

    private static List<MdObject> children(EObject owner, String name)
    {
        var feature = owner.eClass().getEStructuralFeature(name);
        if (!(feature instanceof EReference reference) || !reference.isContainment() || !reference.isMany())
        {
            return List.of();
        }
        List<MdObject> children = new ArrayList<>();
        for (Object child : (List<?>)owner.eGet(feature))
        {
            if (child instanceof MdObject named)
            {
                children.add(named);
            }
        }
        return children;
    }
}
