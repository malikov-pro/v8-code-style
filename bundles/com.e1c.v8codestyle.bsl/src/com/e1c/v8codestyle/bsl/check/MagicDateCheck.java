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
 *     malikov-pro - port of the BSL Language Server diagnostic MagicDate
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.DateLiteral;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.ReturnStatement;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.WrongParameterException;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: магические даты в коде (литералы вида '20260101').
 * <p>
 * Перенос диагностики BSL Language Server MagicDate
 * (тип CODE_SMELL, серьёзность MINOR). Как и в LS: не фиксируются пустая
 * дата и разрешённые даты (параметр authorizedDates), значения по умолчанию
 * параметров методов, правая часть простого присваивания и выражение
 * «Возврат». Упрощение: ключи структур/соответствий не анализируются.
 *
 * @author malikov-pro
 */
public class MagicDateCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "magic-date"; //$NON-NLS-1$

    private static final String DEFAULT_AUTHORIZED_DATES = "00010101,00010101000000,000101010000"; //$NON-NLS-1$

    private static final String PARAM_AUTHORIZED_DATES = "authorizedDates"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public MagicDateCheck()
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
        builder.title(Messages.MagicDateCheck_title)
            .description(Messages.MagicDateCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(METHOD)
            .parameter(PARAM_AUTHORIZED_DATES, String.class, DEFAULT_AUTHORIZED_DATES,
                Messages.MagicDateCheck_Authorized_dates);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Method method = (Method)object;

        Set<String> authorized = authorizedDates(parameters);
        Set<com._1c.g5.v8.dt.bsl.model.Literal> defaults = new HashSet<>();
        for (com._1c.g5.v8.dt.bsl.model.FormalParam formalParam : method.getFormalParams())
        {
            if (formalParam.getDefaultValue() != null)
            {
                defaults.add(formalParam.getDefaultValue());
            }
        }

        for (DateLiteral literal : EcoreUtil2.getAllContentsOfType(method, DateLiteral.class))
        {
            String value = literal.getValue() == null ? "" : literal.getValue().replace("'", ""); //$NON-NLS-1$ //$NON-NLS-2$
            if (defaults.contains(literal) || authorized.contains(value))
            {
                continue;
            }
            EObject container = literal.eContainer();
            if (container instanceof SimpleStatement statement && statement.getRight() == literal)
            {
                // правая часть простого присваивания — как в LS, не фиксируется
                continue;
            }
            if (hasAncestor(literal, ReturnStatement.class))
            {
                continue;
            }
            String message = MessageFormat.format(Messages.MagicDateCheck_Magic_date, value);
            resultAceptor.addIssue(message, literal);
        }
    }

    private static boolean hasAncestor(DateLiteral literal, Class<? extends EObject> type)
    {
        EObject current = literal.eContainer();
        while (current != null)
        {
            if (type.isInstance(current))
            {
                return true;
            }
            current = current.eContainer();
        }
        return false;
    }

    private static Set<String> authorizedDates(ICheckParameters parameters)
    {
        String value;
        try
        {
            value = parameters.getString(PARAM_AUTHORIZED_DATES);
        }
        catch (WrongParameterException e)
        {
            value = DEFAULT_AUTHORIZED_DATES;
        }
        Set<String> dates = new HashSet<>();
        if (value != null && !value.isBlank())
        {
            Arrays.stream(value.split(",")) //$NON-NLS-1$
                .map(String::trim)
                .forEach(dates::add);
        }
        return dates;
    }
}
