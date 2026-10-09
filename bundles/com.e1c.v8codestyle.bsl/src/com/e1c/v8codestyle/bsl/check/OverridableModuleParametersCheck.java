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
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.FormalParam;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.OptInCheckExtension;
import com.google.inject.Inject;

/**
 * Параметры, передаваемые в вызываемую процедуру из переопределяемой,
 * совпадают по составу и порядку с параметрами переопределяемой процедуры
 * (подпункт 3 правила АПК_00460, статья 554 стандарта 1С): вызов
 * {@code <Библиотека>.<Процедура>(...)} передаёт все параметры исходной
 * процедуры в том же порядке. Сравниваются имена и количество параметров
 * по позициям (типы в 1С не декларируются); вызов с несовпадающим составом
 * или порядком помечается отдельным замечанием.
 * <p>
 * Проверяются вызовы, имя метода которых совпадает с именем переопределяемой
 * процедуры: прямой вызов {@code Модуль.Метод(...)}, неявный вызов
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
public class OverridableModuleParametersCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00460-parameters-match"; //$NON-NLS-1$

    private final IV8ProjectManager v8ProjectManager;

    /**
     * Instantiates a new check.
     *
     * @param v8ProjectManager the V8 project manager service, cannot be {@code null}
     */
    @Inject
    public OverridableModuleParametersCheck(IV8ProjectManager v8ProjectManager)
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
        builder.title(Messages.OverridableModuleParametersCheck_title)
            .description(Messages.OverridableModuleParametersCheck_description)
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
        List<FormalParam> formalParams = method.getFormalParams();
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
            checkCallParameters(invocation, formalParams, method, resultAceptor);
        }
    }

    private void checkCallParameters(Invocation invocation, List<FormalParam> formalParams, Method method,
        ResultAcceptor resultAceptor)
    {
        List<Expression> callParams = invocation.getParams();
        if (callParams.size() != formalParams.size())
        {
            resultAceptor.addIssue(MessageFormat.format(
                Messages.OverridableModuleParametersCheck_Parameters_count_does_not_match, callParams.size(),
                formalParams.size(), method.getName()), invocation);
            return;
        }
        for (int i = 0; i < formalParams.size(); i++)
        {
            String formalName = formalParams.get(i).getName();
            String actualName = getCallParamName(callParams.get(i));
            if (formalName == null || formalName.equalsIgnoreCase(actualName))
            {
                continue;
            }
            resultAceptor.addIssue(MessageFormat.format(
                Messages.OverridableModuleParametersCheck_Parameters_order_does_not_match, i + 1, actualName,
                formalName, method.getName()), invocation);
            return;
        }
    }

    /**
     * Возвращает имя параметра вызова: для простого идентификатора — имя
     * переменной, для прочих выражений — их текст (сравнение без учёта
     * регистра, как в АПК).
     */
    private String getCallParamName(Expression expression)
    {
        if (expression instanceof StaticFeatureAccess featureAccess && featureAccess.getName() != null)
        {
            return featureAccess.getName();
        }
        INode node = NodeModelUtils.findActualNodeFor(expression);
        if (node == null)
        {
            return null;
        }
        return node.getText().strip().toUpperCase(Locale.ROOT);
    }
}
