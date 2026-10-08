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
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

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
import com.google.inject.Inject;

/**
 * В процедурах переопределяемого общего модуля используется только вызов
 * переопределяемого метода библиотеки (подпункт 1 правила АПК_00460,
 * статья 554 стандарта 1С): каждый оператор процедуры имеет вид
 * {@code <ИмяМодуля>.<ИмяПроцедуры>(...)}. Любой другой код — присваивания,
 * условия, циклы, локальные вызовы — является лишним: переопределяемая
 * процедура должна сводиться к вызову оригинала. Регистрируется одно
 * замечание на процедуру — по первому лишнему оператору.
 * <p>
 * Распознаются формы вызова из алгоритма АПК: прямой вызов
 * {@code Модуль.Метод(...)}, неявный вызов
 * {@code ОбщегоНазначения.ОбщийМодуль("Модуль").Метод(...)} и вызов через
 * переменную, которой присвоен {@code ОбщегоНазначения.ОбщийМодуль("Модуль")}
 * (сам оператор присваивания при этом считается лишним кодом, как в АПК).
 * Код между комментариями «// _Демо начало примера» и
 * «// _Демо конец примера» не проверяется.
 * <p>
 * Упрощение против алгоритма АПК: проверка применяется, только если
 * существует «парный» метод — метод с тем же именем в другом общем модуле
 * конфигурации; фильтр подсистем не воспроизводится. Проверка выключена
 * по умолчанию (аудит-класс, включается пользователем).
 *
 * @author malikov-pro
 */
public class OverridableModuleRedundantCodeCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00460-redundant-code"; //$NON-NLS-1$

    private final IV8ProjectManager v8ProjectManager;

    /**
     * Instantiates a new check.
     *
     * @param v8ProjectManager the V8 project manager service, cannot be {@code null}
     */
    @Inject
    public OverridableModuleRedundantCodeCheck(IV8ProjectManager v8ProjectManager)
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
        builder.title(Messages.OverridableModuleRedundantCodeCheck_title)
            .description(Messages.OverridableModuleRedundantCodeCheck_description)
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
            if (invocation != null
                && OverridableModuleUtil.resolveForwardingCall(invocation, configuration, method) != null)
            {
                continue;
            }
            resultAceptor.addIssue(MessageFormat.format(
                Messages.OverridableModuleRedundantCodeCheck_Redundant_code, method.getName(),
                OverridableModuleUtil.getOwnerName(module), getStatementText(statement)), statement);
            break;
        }
    }

    private String getStatementText(Statement statement)
    {
        INode node = NodeModelUtils.findActualNodeFor(statement);
        if (node == null)
        {
            return ""; //$NON-NLS-1$
        }
        String[] lines = node.getText().split("\n"); //$NON-NLS-1$
        for (String line : lines)
        {
            String stripped = line.replace("\r", "").strip(); //$NON-NLS-1$ //$NON-NLS-2$
            if (stripped.isEmpty() || stripped.startsWith("//")) //$NON-NLS-1$
            {
                continue;
            }
            return stripped;
        }
        return ""; //$NON-NLS-1$
    }
}
