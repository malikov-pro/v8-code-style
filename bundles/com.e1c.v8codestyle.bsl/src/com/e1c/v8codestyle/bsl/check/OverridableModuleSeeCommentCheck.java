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
 * У процедуры библиотеки, вызываемой из переопределяемой процедуры, есть
 * комментарий-отсылка «См. &lt;ИмяПереопределяемогоМодуля&gt;.&lt;ИмяПереопределяемойПроцедуры&gt;.»
 * (подпункт 4 правила АПК_00460, статья 554 стандарта 1С). Комментарий
 * размещается над объявлением вызываемой процедуры (между ними допускаются
 * пустые строки); при нормализации комментария (без «/», пробелов и
 * переводов строк) текст должен начинаться с «См.&lt;Модуль&gt;.&lt;Процедура&gt;»
 * — регистр значим, как в алгоритме АПК.
 * <p>
 * Проверяются вызовы, имя метода которых совпадает с именем переопределяемой
 * процедуры: прямой вызов {@code Модуль.Метод(...)}, неявный вызов
 * {@code ОбщегоНазначения.ОбщийМодуль("Модуль").Метод(...)} и вызов через
 * переменную. Код между комментариями «// _Демо начало примера» и
 * «// _Демо конец примера» не проверяется.
 * <p>
 * Отклонения от алгоритма АПК: замечание регистрируется на переопределяемой
 * процедуре (в АПК — на вызываемом модуле, доступ к чужому ресурсу из
 * проверки модуля не используется); проверка применяется, только если
 * существует «парный» метод — метод с тем же именем в другом общем модуле
 * конфигурации (глобальный поиск по IV8ProjectManager); фильтр подсистем
 * не воспроизводится. Проверка выключена по умолчанию (аудит-класс,
 * включается пользователем).
 *
 * @author malikov-pro
 */
public class OverridableModuleSeeCommentCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00460-see-comment"; //$NON-NLS-1$

    private final IV8ProjectManager v8ProjectManager;

    /**
     * Instantiates a new check.
     *
     * @param v8ProjectManager the V8 project manager service, cannot be {@code null}
     */
    @Inject
    public OverridableModuleSeeCommentCheck(IV8ProjectManager v8ProjectManager)
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
        builder.title(Messages.OverridableModuleSeeCommentCheck_title)
            .description(Messages.OverridableModuleSeeCommentCheck_description)
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
        String ownerName = OverridableModuleUtil.getOwnerName(module);

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
            if (call == null || call.methodName == null || !call.methodName.equalsIgnoreCase(method.getName()))
            {
                continue;
            }
            Module pairModule = OverridableModuleUtil.getModuleOf(call.module);
            Method pairMethod = OverridableModuleUtil.findMethodInModule(pairModule, call.methodName);
            if (pairMethod == null
                || OverridableModuleUtil.hasSeeComment(pairModule, pairMethod, ownerName, method.getName()))
            {
                continue;
            }
            resultAceptor.addIssue(MessageFormat.format(
                Messages.OverridableModuleSeeCommentCheck_Missing_see_comment, call.module.getName(),
                call.methodName, ownerName, method.getName()), method, NAMED_ELEMENT__NAME);
        }
    }
}
