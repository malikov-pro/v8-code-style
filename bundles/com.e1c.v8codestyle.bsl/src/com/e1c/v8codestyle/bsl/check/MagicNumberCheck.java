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
 *     malikov-pro - port of the BSL Language Server diagnostic MagicNumber
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.FormalParam;
import com._1c.g5.v8.dt.bsl.model.IndexAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.NumberLiteral;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.WrongParameterException;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: магические числа в коде (кроме разрешённых списком и индексов).
 * <p>
 * Перенос диагностики BSL Language Server MagicNumber
 * (тип CODE_SMELL, серьёзность MINOR). Упрощения относительно LS: не
 * фиксируются разрешённые числа (параметр, по умолчанию «-1,0,1»), индексы
 * доступа «Объект[0]» (параметр allowMagicIndexes), значения по умолчанию
 * параметров методов и плоские компоненты вызова «Дата(2026, 1, 1)».
 *
 * @author malikov-pro
 */
public class MagicNumberCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "magic-number"; //$NON-NLS-1$

    private static final String DEFAULT_AUTHORIZED_NUMBERS = "-1,0,1"; //$NON-NLS-1$

    private static final String PARAM_AUTHORIZED_NUMBERS = "authorizedNumbers"; //$NON-NLS-1$

    private static final String PARAM_ALLOW_MAGIC_INDEXES = "allowMagicIndexes"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public MagicNumberCheck()
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
        builder.title(Messages.MagicNumberCheck_title)
            .description(Messages.MagicNumberCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(METHOD)
            .parameter(PARAM_AUTHORIZED_NUMBERS, String.class, DEFAULT_AUTHORIZED_NUMBERS,
                Messages.MagicNumberCheck_Authorized_numbers)
            .parameter(PARAM_ALLOW_MAGIC_INDEXES, Boolean.class, Boolean.TRUE.toString(),
                Messages.MagicNumberCheck_Allow_magic_indexes);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Method method = (Method)object;

        Set<String> authorized = authorizedNumbers(parameters);
        boolean allowIndexes = allowMagicIndexes(parameters);
        Set<EObject> defaults = new HashSet<>();
        for (FormalParam formalParam : method.getFormalParams())
        {
            if (formalParam.getDefaultValue() != null)
            {
                defaults.add(formalParam.getDefaultValue());
            }
        }

        for (NumberLiteral literal : EcoreUtil2.getAllContentsOfType(method, NumberLiteral.class))
        {
            String value = NodeText(literal);
            if (authorized.contains(value))
            {
                continue;
            }
            if (defaults.contains(literal))
            {
                continue;
            }
            if (allowIndexes && literal.eContainer() instanceof IndexAccess)
            {
                continue;
            }
            if (isDateComponent(literal))
            {
                continue;
            }
            String message = MessageFormat.format(Messages.MagicNumberCheck_Magic_number, value);
            resultAceptor.addIssue(message, literal);
        }
    }

    private static String NodeText(NumberLiteral literal)
    {
        return NodeModelUtils.findActualNodeFor(literal).getText().trim();
    }

    /**
     * Плоские числа — аргументы вызова «Дата(…)/Date(…)» — компоненты даты.
     */
    private static boolean isDateComponent(NumberLiteral literal)
    {
        EObject current = literal.eContainer();
        while (current != null && !(current instanceof Statement))
        {
            if (current instanceof Invocation invocation
                && invocation.getMethodAccess() instanceof StaticFeatureAccess access)
            {
                String name = access.getName().toLowerCase(Locale.ROOT);
                if ("дата".equals(name) || "date".equals(name)) //$NON-NLS-1$ //$NON-NLS-2$
                {
                    return isSingleNumberArg(invocation, literal);
                }
            }
            current = current.eContainer();
        }
        return false;
    }

    private static boolean isSingleNumberArg(Invocation invocation, NumberLiteral literal)
    {
        // компонентный вызов: все аргументы — плоские числа
        return invocation.getParams().stream()
            .allMatch(param -> param instanceof NumberLiteral
                || param instanceof com._1c.g5.v8.dt.bsl.model.EmptyExpression)
            && invocation.getParams().contains(literal);
    }

    private static Set<String> authorizedNumbers(ICheckParameters parameters)
    {
        String value;
        try
        {
            value = parameters.getString(PARAM_AUTHORIZED_NUMBERS);
        }
        catch (WrongParameterException e)
        {
            value = DEFAULT_AUTHORIZED_NUMBERS;
        }
        Set<String> numbers = new HashSet<>();
        if (value != null && !value.isBlank())
        {
            Arrays.stream(value.split(",")) //$NON-NLS-1$
                .map(String::trim)
                .forEach(numbers::add);
        }
        return numbers;
    }

    private static boolean allowMagicIndexes(ICheckParameters parameters)
    {
        try
        {
            return parameters.getBoolean(PARAM_ALLOW_MAGIC_INDEXES);
        }
        catch (WrongParameterException e)
        {
            return true;
        }
    }
}
