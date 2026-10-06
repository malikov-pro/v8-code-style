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
 *     malikov-pro - port of the BSL Language Server diagnostic DeprecatedCurrentDate
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
 * Проверка: использование устаревшего метода «ТекущаяДата» — время может
 * отличаться на клиенте и сервере (непредсказуемое поведение); следует
 * использовать «ТекущаяДатаСеанса».
 * <p>
 * Перенос диагностики BSL Language Server DeprecatedCurrentDate
 * (тип ERROR, серьёзность MAJOR). Замещает часть проверки
 * use-non-recommended-method (методы ТекущаяДата/CurrentDate убраны из её
 * дефолтного списка): эта проверка целевая — серьёзность выше и конкретная
 * рекомендация замены. Quick fix не предусмотрен: семантика времени
 * меняется (сеанс vs клиент/сервер) — решение пользователя.
 *
 * @author malikov-pro
 */
public class DeprecatedCurrentDateCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "deprecated-current-date"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public DeprecatedCurrentDateCheck()
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
        builder.title(Messages.DeprecatedCurrentDateCheck_title)
            .description(Messages.DeprecatedCurrentDateCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.ERROR)
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
            return;
        }
        String name = methodAccess.getName().toLowerCase(Locale.ROOT);
        if ("текущаядата".equals(name) || "currentdate".equals(name)) //$NON-NLS-1$ //$NON-NLS-2$
        {
            resultAceptor.addIssue(Messages.DeprecatedCurrentDateCheck_Use_CurrentSessionDate_instead_of_CurrentDate,
                methodAccess);
        }
    }
}
