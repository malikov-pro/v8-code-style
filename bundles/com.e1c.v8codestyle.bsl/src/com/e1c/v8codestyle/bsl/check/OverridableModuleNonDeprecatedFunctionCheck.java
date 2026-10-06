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
import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.BslContextDef;
import com._1c.g5.v8.dt.bsl.model.BslContextDefMethod;
import com._1c.g5.v8.dt.bsl.model.Function;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
import com._1c.g5.v8.dt.bsl.model.RegionPreprocessor;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.bsl.ModuleStructureSection;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * В переопределяемом общем модуле не должно быть неустаревших функций
 * (подпункт 5 правила АПК_00460, статья 554 стандарта 1С).
 * <p>
 * Переопределяемый общий модуль (в имени содержится
 * «Переопределяемый»/«Override») содержит только переопределяющие процедуры;
 * функция в нём никогда не будет вызвана механизмом переопределения.
 * Устаревшие функции допустимы для обратной совместимости: функция считается
 * устаревшей, если помечена комментарием «Устарела.»/«Deprecated.» либо
 * размещена в области «УстаревшиеПроцедурыИФункции»/«Deprecated».
 * Каждая неустаревшая функция помечается отдельным замечанием.
 * <p>
 * Заимствованные (adopted) общие модули проектов-расширений не проверяются
 * — как в исходном алгоритме АПК.
 *
 * @author malikov-pro
 */
public class OverridableModuleNonDeprecatedFunctionCheck
    extends AbstractModuleStructureCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00460-non-deprecated-functions"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public OverridableModuleNonDeprecatedFunctionCheck()
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
        builder.title(Messages.OverridableModuleNonDeprecatedFunctionCheck_title)
            .description(Messages.OverridableModuleNonDeprecatedFunctionCheck_description)
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
        if (progressMonitor.isCanceled() || !(object instanceof Function))
        {
            return;
        }
        Function function = (Function)object;
        if (function.getName() == null)
        {
            return;
        }

        Module module = EcoreUtil2.getContainerOfType(function, Module.class);
        if (!OverridableModuleUtil.isOverridableCommonModule(module) || isDeprecated(module, function))
        {
            return;
        }

        resultAceptor.addIssue(MessageFormat.format(
            Messages.OverridableModuleNonDeprecatedFunctionCheck_Function_is_not_deprecated, function.getName(),
            OverridableModuleUtil.getOwnerName(module)), function, NAMED_ELEMENT__NAME);
    }

    /**
     * Функция устарела, если помечена комментарием «Устарела.»/«Deprecated.»
     * (признак в контексте модуля) либо размещена в области
     * «УстаревшиеПроцедурыИФункции»/«Deprecated».
     */
    private boolean isDeprecated(Module module, Function function)
    {
        if (isInDeprecatedRegion(function))
        {
            return true;
        }
        return module.getContextDef() instanceof BslContextDef contextDef && contextDef.allMethods()
            .stream()
            .filter(m -> function.getName().equals(m.getName()))
            .anyMatch(m -> m instanceof BslContextDefMethod defMethod && defMethod.isDeprecated());
    }

    private boolean isInDeprecatedRegion(Method function)
    {
        Optional<RegionPreprocessor> region = getFirstParentRegion(function);
        if (region.isEmpty() || region.get().getName() == null)
        {
            return false;
        }
        String regionName = region.get().getName();
        for (String deprecatedName : ModuleStructureSection.DEPRECATED_REGION.getNames())
        {
            if (deprecatedName.equalsIgnoreCase(regionName))
            {
                return true;
            }
        }
        return false;
    }
}
