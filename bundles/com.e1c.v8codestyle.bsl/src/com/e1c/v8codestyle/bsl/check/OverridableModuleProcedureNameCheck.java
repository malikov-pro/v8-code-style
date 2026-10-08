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

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.OptInCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;
import com.google.inject.Inject;

/**
 * Имя процедуры, вызываемой из переопределяемой процедуры, совпадает
 * с именем переопределяемой процедуры (подпункт 2 правила АПК_00460,
 * статья 554 стандарта 1С): переопределяющая процедура
 * {@code <Модуль>.<Процедура>} сводится к вызову
 * {@code <Библиотека>.<Та жеПроцедура>(...)}. Вызов метода с другим именем
 * помечается отдельным замечанием.
 * <p>
 * Распознаются формы вызова из алгоритма АПК: прямой вызов
 * {@code Модуль.Метод(...)}, неявный вызов
 * {@code ОбщегоНазначения.ОбщийМодуль("Модуль").Метод(...)} и вызов через
 * переменную, которой присвоен {@code ОбщегоНазначения.ОбщийМодуль("Модуль")}.
 * Код между комментариями «// _Демо начало примера» и
 * «// _Демо конец примера» не проверяется.
 * <p>
 * Упрощение против алгоритма АПК: проверка применяется, только если
 * существует «парный» метод — метод с тем же именем в другом общем модуле
 * конфигурации (глобальный поиск по IV8ProjectManager); фильтр подсистем
 * не воспроизводится. Проверка выключена по умолчанию (аудит-класс,
 * включается пользователем).
 *
 * @author malikov-pro
 */
public class OverridableModuleProcedureNameCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00460-procedure-name-match"; //$NON-NLS-1$

    private final IV8ProjectManager v8ProjectManager;

    /**
     * Instantiates a new check.
     *
     * @param v8ProjectManager the V8 project manager service, cannot be {@code null}
     */
    @Inject
    public OverridableModuleProcedureNameCheck(IV8ProjectManager v8ProjectManager)
    {
        super();
        this.v8ProjectManager = v8ProjectManager;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.OverridableModuleProcedureNameCheck_title)
            .description(Messages.OverridableModuleProcedureNameCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.ERROR)
            .extension(new SkipAdoptedInExtensionModuleOwnerExtension())
            .extension(new OptInCheckExtension())
            .extension(ModuleTypeFilter.onlyTypes(ModuleType.COMMON_MODULE))
            .module()
            .checkedObjectType(METHOD);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        if (progressMonitor.isCanceled() || !(object instanceof Method method))
        {
            return;
        }
        Module module = EcoreUtil2.getContainerOfType(method, Module.class);
        if (!OverridableModuleUtil.isOverridableCommonModule(module)
            || !OverridableModuleUtil.isForwardingProcedureCandidate(method)
            || OverridableModuleUtil.isDeprecatedMethod(module, method))
        {
            return;
        }
        Configuration configuration = OverridableModuleUtil.getConfiguration(v8ProjectManager, method);
        if (configuration == null
            || !OverridableModuleUtil.hasPairMethod(configuration, module, method.getName()))
        {
            return;
        }

        List<int[]> demoRanges = OverridableModuleUtil.findDemoExampleRanges(module);
        for (Statement statement : method.getStatements())
        {
            if (OverridableModuleUtil.isInDemoExample(statement, demoRanges))
            {
                continue;
            }
            Invocation invocation = OverridableModuleUtil.getCallExpression(statement);
            if (invocation == null)
            {
                continue;
            }
            OverridableModuleUtil.ForwardingCall call =
                OverridableModuleUtil.resolveForwardingCall(invocation, configuration, method);
            if (call == null || call.methodName == null
                || call.methodName.equalsIgnoreCase(method.getName()))
            {
                continue;
            }
            resultAceptor.addIssue(MessageFormat.format(
                Messages.OverridableModuleProcedureNameCheck_Method_name_does_not_match, call.methodName,
                method.getName(), OverridableModuleUtil.getOwnerName(module)), statement);
        }
    }
}
