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
 *     malikov-pro - port of the BSL Language Server diagnostic TimeoutsInExternalResources
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.EmptyExpression;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.NumberLiteral;
import com._1c.g5.v8.dt.bsl.model.OperatorStyleCreator;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.WrongParameterException;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: не указан таймаут при работе с внешним ресурсом
 * (FTPСоединение, HTTPСоединение, WSОпределения, WSПрокси,
 * ИнтернетПочтовыйПрофиль). Отсутствие таймаута — риск зависания сеанса.
 * <p>
 * Перенос диагностики BSL Language Server TimeoutsInExternalResources
 * (тип ERROR, серьёзность CRITICAL). Таймаут считается заданным, если в
 * конструкторе заполнен параметр таймаута (индексы как в LS: WSОпределения —
 * 4, FTPСоединение — 6, остальные — 5) ИЛИ ниже по коду той же переменной
 * присвоено свойство «Таймаут» числом/переменной (упрощение: поиск ниже —
 * по всему методу, в LS — в том же блоке кода). Параметр
 * analyzeInternetMailProfileZeroTimeout включает анализ
 * ИнтернетПочтовыйПрофиль (по умолчанию включён). Quick fix не предусмотрен.
 *
 * @author malikov-pro
 */
public class TimeoutsInExternalResourcesCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "timeouts-in-external-resources"; //$NON-NLS-1$

    private static final Pattern PATTERN_RESOURCE_TYPE = Pattern
        .compile("^(FTPСоединение|FTPConnection|HTTPСоединение|HTTPConnection|WSОпределения|WSDefinitions|WSПрокси|WSProxy)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern PATTERN_RESOURCE_TYPE_WITH_MAIL = Pattern
        .compile("^(FTPСоединение|FTPConnection|HTTPСоединение|HTTPConnection|WSОпределения|WSDefinitions|WSПрокси|WSProxy|ИнтернетПочтовыйПрофиль|InternetMailProfile)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern PATTERN_TIMEOUT_PROPERTY = Pattern
        .compile("^(Таймаут|Timeout)$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final int TIMEOUT_INDEX_DEFAULT = 5;
    private static final int TIMEOUT_INDEX_FTP = 6;
    private static final int TIMEOUT_INDEX_WSD = 4;
    private static final int TIMEOUT_INDEX_MAIL = 5;

    private static final String PARAM_ANALYZE_MAIL = "analyzeInternetMailProfileZeroTimeout"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public TimeoutsInExternalResourcesCheck()
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
        builder.title(Messages.TimeoutsInExternalResourcesCheck_title)
            .description(Messages.TimeoutsInExternalResourcesCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(METHOD)
            .parameter(PARAM_ANALYZE_MAIL, Boolean.class, Boolean.TRUE.toString(),
                Messages.TimeoutsInExternalResourcesCheck_Analyze_mail);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Method method = (Method)object;

        boolean analyzeMail = analyzeMail(parameters);
        Pattern resourceTypes = analyzeMail ? PATTERN_RESOURCE_TYPE_WITH_MAIL : PATTERN_RESOURCE_TYPE;

        List<SimpleStatement> statements = EcoreUtil2.getAllContentsOfType(method, SimpleStatement.class);

        for (OperatorStyleCreator creator : EcoreUtil2.getAllContentsOfType(method, OperatorStyleCreator.class))
        {
            String typeName = typeName(creator);
            if (typeName == null || !resourceTypes.matcher(typeName).find())
            {
                continue;
            }
            int timeoutIndex = timeoutIndex(typeName);
            if (isTimeoutSetInConstructor(creator, timeoutIndex))
            {
                continue;
            }
            String variableName = variableNameOfStatement(creator);
            if (variableName != null && isTimeoutAssignedBelow(statements, creator, variableName))
            {
                continue;
            }
            resultAceptor.addIssue(Messages.TimeoutsInExternalResourcesCheck_Timeout_not_specified, creator);
        }
    }

    private static boolean analyzeMail(ICheckParameters parameters)
    {
        try
        {
            return parameters.getBoolean(PARAM_ANALYZE_MAIL);
        }
        catch (WrongParameterException e)
        {
            return true;
        }
    }

    /**
     * Имя создаваемого типа: русское (как в LS) либо английское.
     */
    private static String typeName(OperatorStyleCreator creator)
    {
        if (creator.getType() == null)
        {
            // тип мог не разрешиться — берём имя из текста «Новый <Тип>»
            String text = NodeModelUtils.findActualNodeFor(creator).getText();
            java.util.regex.Matcher matcher = Pattern
                .compile("(?iu)\\b(?:Новый|New)\\s+([А-Яа-яA-Za-z0-9_]+)").matcher(text);
            return matcher.find() ? matcher.group(1) : null;
        }
        String nameRu = creator.getType().getNameRu();
        return nameRu != null ? nameRu : creator.getType().getName();
    }

    private static int timeoutIndex(String typeName)
    {
        String lower = typeName.toLowerCase(Locale.ROOT);
        if (lower.startsWith("ws"))
        {
            return TIMEOUT_INDEX_WSD;
        }
        if (lower.startsWith("ftp"))
        {
            return TIMEOUT_INDEX_FTP;
        }
        if (lower.startsWith("интернетпочтовый") || lower.startsWith("internetmail"))
        {
            return TIMEOUT_INDEX_MAIL;
        }
        return TIMEOUT_INDEX_DEFAULT;
    }

    /**
     * Таймаут задан в конструкторе: параметр по индексу — число или
     * переменная (как isNumberOrVariable в LS; строка/булево/пусто — не задан).
     */
    private static boolean isTimeoutSetInConstructor(OperatorStyleCreator creator, int timeoutIndex)
    {
        List<Expression> params = creator.getParams();
        if (params.size() <= timeoutIndex)
        {
            return false;
        }
        Expression timeoutParam = params.get(timeoutIndex);
        if (timeoutParam instanceof EmptyExpression)
        {
            return false;
        }
        return timeoutParam instanceof NumberLiteral || timeoutParam instanceof StaticFeatureAccess;
    }

    /**
     * Имя переменной оператора, содержащего создание
     * {@code Соединение = Новый HTTPСоединение(...)}.
     */
    private static String variableNameOfStatement(OperatorStyleCreator creator)
    {
        EObject ancestor = creator.eContainer();
        while (ancestor != null && !(ancestor instanceof Statement))
        {
            ancestor = ancestor.eContainer();
        }
        if (ancestor instanceof SimpleStatement statement && statement.getLeft() instanceof StaticFeatureAccess left)
        {
            return left.getName();
        }
        return null;
    }

    /**
     * Ниже по коду метода той же переменной присваивается свойство
     * «Таймаут» числом или переменной
     * ({@code Соединение.Таймаут = 60;}).
     */
    private static boolean isTimeoutAssignedBelow(List<SimpleStatement> statements, OperatorStyleCreator creator,
        String variableName)
    {
        int creatorOffset = NodeModelUtils.findActualNodeFor(creator).getTotalOffset();
        for (SimpleStatement statement : statements)
        {
            if (!(statement.getLeft() instanceof DynamicFeatureAccess property))
            {
                continue;
            }
            INode node = NodeModelUtils.findActualNodeFor(statement);
            if (node == null || node.getTotalOffset() <= creatorOffset)
            {
                continue;
            }
            if (!(property.getSource() instanceof StaticFeatureAccess source)
                || !source.getName().equalsIgnoreCase(variableName))
            {
                continue;
            }
            if (!PATTERN_TIMEOUT_PROPERTY.matcher(property.getName()).find())
            {
                continue;
            }
            Expression right = statement.getRight();
            if (right instanceof NumberLiteral || right instanceof StaticFeatureAccess)
            {
                return true;
            }
        }
        return false;
    }
}
