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
 *     malikov-pro - port of the BSL Language Server diagnostic RewriteMethodParameter
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.FormalParam;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: параметр метода, переданный по значению (Знач), перезаписывается
 * до первого использования. Переданное значение теряется — вероятная ошибка.
 * <p>
 * Перенос диагностики BSL Language Server RewriteMethodParameter
 * (тип CODE_SMELL/SUSPICIOUS, серьёзность MAJOR). Упрощение относительно LS
 * (нет ReferenceIndex): маркер ставится на первое переприсваивание
 * {@code Параметр = …}, в правой части которого параметр не читается и до
 * которого параметр нигде не использовался; модификация
 * {@code Параметр = Параметр + 1} не является переписыванием. Параметры
 * без Знач (по ссылке) не проверяются — как в LS.
 *
 * @author malikov-pro
 */
public class RewriteMethodParameterCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "rewrite-method-parameter"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public RewriteMethodParameterCheck()
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
        builder.title(Messages.RewriteMethodParameterCheck_title)
            .description(Messages.RewriteMethodParameterCheck_description)
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

        Set<String> byValueParams = new HashSet<>();
        for (FormalParam formalParam : method.getFormalParams())
        {
            if (formalParam.isByValue())
            {
                byValueParams.add(formalParam.getName().toLowerCase(Locale.ROOT));
            }
        }
        if (byValueParams.isEmpty())
        {
            return;
        }

        // параметры, использованные (прочитанные) хотя бы раз, не проверяются
        Set<String> usedParams = new HashSet<>();
        Set<String> flaggedParams = new HashSet<>();

        for (SimpleStatement statement : EcoreUtil2.getAllContentsOfType(method, SimpleStatement.class))
        {
            if (!(statement.getLeft() instanceof StaticFeatureAccess left))
            {
                continue;
            }
            String name = left.getName();
            String lowerName = name.toLowerCase(Locale.ROOT);

            Set<String> readNames = statement.getRight() == null
                ? Set.of() : readNames(statement.getRight());
            boolean rightReadsParam = readNames.contains(lowerName);

            if (!rightReadsParam && byValueParams.contains(lowerName) && !usedParams.contains(lowerName)
                && !flaggedParams.contains(lowerName))
            {
                String message = MessageFormat.format(
                    Messages.RewriteMethodParameterCheck_Parameter_rewrite_without_use, name);
                resultAceptor.addIssue(message, statement);
                flaggedParams.add(lowerName);
            }

            usedParams.addAll(readNames);
        }
    }

    /**
     * Имена всех переменных/параметров, читаемых в выражении.
     *
     * @param expression выражение, не может быть {@code null}.
     * @return имена в нижнем регистре, не {@code null}.
     */
    private static Set<String> readNames(EObject expression)
    {
        Set<String> names = new HashSet<>();
        for (StaticFeatureAccess access : EcoreUtil2.getAllContentsOfType(expression, StaticFeatureAccess.class))
        {
            names.add(access.getName().toLowerCase(Locale.ROOT));
        }
        return names;
    }
}
