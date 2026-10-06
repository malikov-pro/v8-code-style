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
 *     malikov-pro - port of the BSL Language Server diagnostic MissingTempStorageDeletion
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;

import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: получение данных из временного хранилища без последующего
 * удаления. Данные остаются в серверной памяти до завершения сеанса.
 * <p>
 * Перенос диагностики BSL Language Server MissingTempStorageDeletion
 * (тип CODE_SMELL, серьёзность CRITICAL). Упрощение относительно LS
 * (там анализируются операторы позже по строкам в том же блоке): если в
 * методе есть «ПолучитьИзВременногоХранилища», но ни одного
 * «УдалитьИзВременногоХранилища», каждое получение фиксируется.
 * Quick fix не предусмотрен: момент удаления определяется логикой метода.
 *
 * @author malikov-pro
 */
public class MissingTempStorageDeletionCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "missing-temp-storage-deletion"; //$NON-NLS-1$

    private static final String GET_RU = "получитьизвременногохранилища"; //$NON-NLS-1$
    private static final String GET_EN = "getfromtempstorage"; //$NON-NLS-1$
    private static final String DELETE_RU = "удалитьизвременногохранилища"; //$NON-NLS-1$
    private static final String DELETE_EN = "deletefromtempstorage"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public MissingTempStorageDeletionCheck()
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
        builder.title(Messages.MissingTempStorageDeletionCheck_title)
            .description(Messages.MissingTempStorageDeletionCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(METHOD);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Method method = (Method)object;

        boolean hasDeletion = false;
        int getCount = 0;
        Invocation firstGet = null;
        for (Invocation invocation : EcoreUtil2.getAllContentsOfType(method, Invocation.class))
        {
            if (!(invocation.getMethodAccess() instanceof StaticFeatureAccess methodAccess))
            {
                continue;
            }
            String name = methodAccess.getName().toLowerCase(Locale.ROOT);
            if (GET_RU.equals(name) || GET_EN.equals(name))
            {
                getCount++;
                if (firstGet == null)
                {
                    firstGet = invocation;
                }
            }
            else if (DELETE_RU.equals(name) || DELETE_EN.equals(name))
            {
                hasDeletion = true;
            }
        }
        if (getCount == 0 || hasDeletion || firstGet == null)
        {
            return;
        }
        for (Invocation invocation : EcoreUtil2.getAllContentsOfType(method, Invocation.class))
        {
            if (invocation.getMethodAccess() instanceof StaticFeatureAccess methodAccess)
            {
                String name = methodAccess.getName().toLowerCase(Locale.ROOT);
                if (GET_RU.equals(name) || GET_EN.equals(name))
                {
                    resultAceptor.addIssue(
                        Messages.MissingTempStorageDeletionCheck_Missing_temp_storage_deletion, invocation);
                }
            }
        }
    }
}
