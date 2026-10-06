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
 *     malikov-pro - port of the APK check АПК_00460
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.NAMED_ELEMENT__NAME;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * В переопределяемом общем модуле не должно быть неэкспортных методов
 * (подпункт 6 правила АПК_00460, статья 554 стандарта 1С).
 * <p>
 * Переопределяемый общий модуль (в имени содержится
 * «Переопределяемый»/«Override») может содержать только экспортные методы,
 * переопределяющие методы библиотеки: неэкспортный метод недоступен
 * для вызывающего механизма и никогда не будет выполнен. Каждый
 * неэкспортный метод помечается отдельным замечанием.
 * <p>
 * Заимствованные (adopted) общие модули проектов-расширений не проверяются
 * — как в исходном алгоритме АПК.
 *
 * @author malikov-pro
 */
public class OverridableModuleNonExportMethodCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00460-non-export-methods"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public OverridableModuleNonExportMethodCheck()
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
        builder.title(Messages.OverridableModuleNonExportMethodCheck_title)
            .description(Messages.OverridableModuleNonExportMethodCheck_description)
            .issueType(IssueType.ERROR)
            .severity(IssueSeverity.MAJOR)
            .extension(new SkipAdoptedInExtensionModuleOwnerExtension())
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .extension(ModuleTypeFilter.onlyTypes(ModuleType.COMMON_MODULE))
            .module()
            .checkedObjectType(METHOD);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Method method = (Method)object;
        if (progressMonitor.isCanceled() || method.isExport() || method.getName() == null)
        {
            return;
        }

        Module module = EcoreUtil2.getContainerOfType(method, Module.class);
        if (!OverridableModuleUtil.isOverridableCommonModule(module))
        {
            return;
        }

        resultAceptor.addIssue(MessageFormat.format(Messages.OverridableModuleNonExportMethodCheck_Method_is_not_export,
            method.getName(), OverridableModuleUtil.getOwnerName(module)), method, NAMED_ELEMENT__NAME);
    }
}
