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
 *     malikov-pro - port of the BSL Language Server diagnostic UnusedParameters
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;

import java.text.MessageFormat;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.FormalParam;
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
 * Проверка: неиспользуемый параметр метода. Параметр, не используемый в
 * теле метода, вводит в заблуждение — вызов обязателен, а смысла не имеет.
 * <p>
 * Перенос диагностики BSL Language Server UnusedParameters
 * (тип CODE_SMELL, серьёзность MAJOR). Упрощение относительно LS (там
 * учитываются ссылки через ReferenceIndex): параметр считается используемым,
 * если его имя встречается в теле метода как обращение; параметры со
 * значением по умолчанию проверяются так же. Quick fix не предусмотрен:
 * удаление параметра меняет сигнатуру и вызовы.
 *
 * @author malikov-pro
 */
public class UnusedParametersCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "unused-parameters"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public UnusedParametersCheck()
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
        builder.title(Messages.UnusedParametersCheck_title)
            .description(Messages.UnusedParametersCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
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
        if (method.getFormalParams().isEmpty())
        {
            return;
        }

        Set<String> usedNames = EcoreUtil2.getAllContentsOfType(method, StaticFeatureAccess.class).stream()
            .map(StaticFeatureAccess::getName)
            .map(name -> name.toLowerCase(java.util.Locale.ROOT))
            .collect(Collectors.toSet());

        for (FormalParam formalParam : method.getFormalParams())
        {
            if (!usedNames.contains(formalParam.getName().toLowerCase(java.util.Locale.ROOT)))
            {
                String message = MessageFormat.format(Messages.UnusedParametersCheck_Unused_parameter,
                    formalParam.getName());
                resultAceptor.addIssue(message, formalParam);
            }
        }
    }
}
