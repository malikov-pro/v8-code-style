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
 *     malikov-pro - port of the BSL Language Server diagnostic DeprecatedFind
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;

import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;

import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: использование устаревшего глобального метода «Найти» —
 * следует использовать «СтрНайти».
 * <p>
 * Перенос диагностики BSL Language Server DeprecatedFind
 * (тип CODE_SMELL, серьёзность MINOR). Замещает часть проверки
 * use-non-recommended-method (методы Найти/Find убраны из её дефолтного
 * списка): эта проверка целевая — даёт рекомендацию замены и ловит вызовы,
 * не разрешённые в модели типов. Методы объекта (Объект.Найти) не
 * проверяются — как в LS. Quick fix не предусмотрен: замена Найти → СтрНайти
 * меняет порядок аргументов.
 *
 * @author malikov-pro
 */
public class DeprecatedFindCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "deprecated-find"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public DeprecatedFindCheck()
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
        builder.title(Messages.DeprecatedFindCheck_title)
            .description(Messages.DeprecatedFindCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(INVOCATION);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Invocation invocation = (Invocation)object;
        if (!(invocation.getMethodAccess() instanceof StaticFeatureAccess methodAccess))
        {
            // методы объекта (Объект.Найти) не проверяются — как в LS
            return;
        }
        String name = methodAccess.getName().toLowerCase(Locale.ROOT);
        if ("найти".equals(name) || "find".equals(name)) //$NON-NLS-1$ //$NON-NLS-2$
        {
            resultAceptor.addIssue(Messages.DeprecatedFindCheck_Use_StrFind_instead_of_Find, methodAccess);
        }
    }
}
