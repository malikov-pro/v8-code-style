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
 *     malikov-pro - port of the BSL Language Server diagnostic OSUsersMethod
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
 * Проверка: использование метода «ПользователиОС» — потенциально опасно
 * (получение данных пользователей операционной системы), требуется
 * обоснование и проверка прав.
 * <p>
 * Перенос диагностики BSL Language Server OSUsersMethod
 * (тип SECURITY_HOTSPOT, серьёзность CRITICAL). Методы объекта не
 * проверяются — как в LS. Quick fix не предусмотрен.
 *
 * @author malikov-pro
 */
public class OSUsersMethodCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "o-s-users-method"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public OSUsersMethodCheck()
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
        builder.title(Messages.OSUsersMethodCheck_title)
            .description(Messages.OSUsersMethodCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.SECURITY)
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
        if ("пользователиос".equals(name) || "osusers".equals(name)) //$NON-NLS-1$ //$NON-NLS-2$
        {
            resultAceptor.addIssue(Messages.OSUsersMethodCheck_Check_potentially_malicious_use_of_OS_users_method,
                methodAccess);
        }
    }
}
