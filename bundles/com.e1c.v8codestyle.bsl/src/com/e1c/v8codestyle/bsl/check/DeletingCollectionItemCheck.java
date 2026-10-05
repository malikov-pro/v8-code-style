/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: 1C-Soft LLC
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the BSL Language Server diagnostic DeletingCollectionItem
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FOR_EACH_STATEMENT;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.ForEachStatement;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: не следует удалять элементы коллекции при её обходе оператором
 * «Для каждого … Из … Цикл» — при удалении элемента сдвигается индекс
 * следующего элемента, часть элементов будет пропущена.
 * <p>
 * Перенос диагностики BSL Language Server DeletingCollectionItem
 * (тип ERROR, серьёзность MAJOR).
 *
 * @author malikov-pro
 */
public class DeletingCollectionItemCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "deleting-collection-item"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public DeletingCollectionItemCheck()
    {
        super();
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.DeletingCollectionItemCheck_title)
            .description(Messages.DeletingCollectionItemCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(FOR_EACH_STATEMENT);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        ForEachStatement forEach = (ForEachStatement)object;
        if (forEach.getCollection() == null)
        {
            return;
        }
        String collectionText = NodeModelUtils.findActualNodeFor(forEach.getCollection()).getText().trim();
        String collectionPrefix = collectionText.toLowerCase() + "."; //$NON-NLS-1$

        List<Invocation> invocations = EcoreUtil2.getAllContentsOfType(forEach, Invocation.class);
        for (Invocation invocation : invocations)
        {
            if (!(invocation.getMethodAccess() instanceof DynamicFeatureAccess))
            {
                continue;
            }
            DynamicFeatureAccess methodAccess = (DynamicFeatureAccess)invocation.getMethodAccess();
            String methodName = methodAccess.getName();
            if (!"Удалить".equalsIgnoreCase(methodName) && !"Delete".equalsIgnoreCase(methodName)) //$NON-NLS-1$ //$NON-NLS-2$
            {
                continue;
            }
            if (isSameCollection(methodAccess.getSource(), collectionText, collectionPrefix))
            {
                String message = MessageFormat.format(
                    Messages.DeletingCollectionItemCheck_Do_not_delete_collection_items_while_iterating,
                    collectionText);
                resultAceptor.addIssue(message, invocation);
            }
        }
    }

    private boolean isSameCollection(EObject source, String collectionText, String collectionPrefix)
    {
        if (source == null)
        {
            return false;
        }
        String sourceText = NodeModelUtils.findActualNodeFor(source).getText().trim();
        return sourceText.equalsIgnoreCase(collectionText)
            || sourceText.toLowerCase().startsWith(collectionPrefix);
    }
}
