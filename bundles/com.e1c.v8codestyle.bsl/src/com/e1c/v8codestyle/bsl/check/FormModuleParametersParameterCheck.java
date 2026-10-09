/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: malikov-pro
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the upstream issue 1C-Company/v8-code-style#1353
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.NAMED_ELEMENT__NAME;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.FormalParam;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: параметр метода модуля формы не должен иметь имя «Параметры»
 * (англ. «Parameters», без учёта регистра). В модуле формы доступ к параметрам
 * формы осуществляется через системное свойство «Параметры», поэтому
 * одноимённый параметр метода перекрывает его и может привести к неявным
 * ошибкам в коде. Каждый такой параметр помечается одним замечанием.
 * <p>
 * Перенос предложения апстрима 1C-Company/v8-code-style#1353.
 *
 * @author malikov-pro
 */
public class FormModuleParametersParameterCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс up-N — номер issue апстрима). */
    public static final String CHECK_ID = "up-1353-form-parameters-param"; //$NON-NLS-1$

    private static final Set<String> FORBIDDEN_NAMES = Set.of("параметры", "parameters"); //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * Instantiates a new check.
     */
    public FormModuleParametersParameterCheck()
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
        builder.title(Messages.FormModuleParametersParameterCheck_title)
            .description(Messages.FormModuleParametersParameterCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .extension(ModuleTypeFilter.onlyTypes(ModuleType.FORM_MODULE))
            .module()
            .checkedObjectType(METHOD);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (monitor.isCanceled() || !(object instanceof Method method))
        {
            return;
        }

        Module module = EcoreUtil2.getContainerOfType(method, Module.class);
        if (module == null || module.getModuleType() != ModuleType.FORM_MODULE)
        {
            return;
        }

        for (FormalParam param : method.getFormalParams())
        {
            if (monitor.isCanceled())
            {
                return;
            }
            String name = param.getName();
            if (name != null && FORBIDDEN_NAMES.contains(name.toLowerCase(Locale.ROOT)))
            {
                resultAceptor.addIssue(MessageFormat.format(
                    Messages.FormModuleParametersParameterCheck_Parameter_named_Parameters, name), param,
                    NAMED_ELEMENT__NAME);
            }
        }
    }
}
