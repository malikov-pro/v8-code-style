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
 *     malikov-pro - port of the BSL Language Server diagnostic UsingHardcodeSecretInformation
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.IndexAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.OperatorStyleCreator;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.StringLiteral;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.WrongParameterException;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: хранение конфиденциальной информации (паролей) в коде:
 * <ul>
 * <li>{@code Пароль = "12345";}</li>
 * <li>{@code Объект["Пароль"] = "12345";}</li>
 * <li>{@code Объект.Пароль = "12345";}</li>
 * <li>{@code Объект.Вставить("Пароль", "12345");}</li>
 * <li>{@code Новый Структура("Пароль", "12345");}</li>
 * <li>{@code Новый FTPСоединение(…, "12345", …);} — пароль соединения.</li>
 * </ul>
 * <p>
 * Перенос диагностики BSL Language Server UsingHardcodeSecretInformation
 * (тип VULNERABILITY, серьёзность CRITICAL). Ключевые слова — параметр
 * (по умолчанию «Пароль|Password»); замаскированные значения («***»)
 * не фиксируются. Quick fix не предусмотрен: хранение пароля — решение
 * пользователя (БезопасноеХранение/настройка).
 *
 * @author malikov-pro
 */
public class UsingHardcodeSecretInformationCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "using-hardcode-secret-information"; //$NON-NLS-1$

    private static final String DEFAULT_SEARCH_WORDS = "Пароль|Password"; //$NON-NLS-1$

    private static final String PARAM_SEARCH_WORDS = "searchWords"; //$NON-NLS-1$

    private static final String INSERT_RU = "вставить"; //$NON-NLS-1$
    private static final String INSERT_EN = "insert"; //$NON-NLS-1$

    private static final String STRUCTURE_RU = "структура"; //$NON-NLS-1$
    private static final String STRUCTURE_EN = "structure"; //$NON-NLS-1$
    private static final String MAP_RU = "соответствие"; //$NON-NLS-1$
    private static final String MAP_EN = "map"; //$NON-NLS-1$

    private static final String CONNECTION_RU = "соединение"; //$NON-NLS-1$

    private static final int CONNECTION_PASSWORD_PARAM_INDEX = 3;

    /**
     * Instantiates a new check.
     */
    public UsingHardcodeSecretInformationCheck()
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
        builder.title(Messages.UsingHardcodeSecretInformationCheck_title)
            .description(Messages.UsingHardcodeSecretInformationCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.SECURITY)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(METHOD)
            .parameter(PARAM_SEARCH_WORDS, String.class, DEFAULT_SEARCH_WORDS,
                Messages.UsingHardcodeSecretInformationCheck_Search_words);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Method method = (Method)object;

        Pattern searchWords = searchWordsPattern(parameters);

        for (SimpleStatement statement : EcoreUtil2.getAllContentsOfType(method, SimpleStatement.class))
        {
            checkStatement(statement, searchWords, resultAceptor);
        }
        for (Invocation invocation : EcoreUtil2.getAllContentsOfType(method, Invocation.class))
        {
            checkInsertInvocation(invocation, searchWords, resultAceptor);
        }
        for (OperatorStyleCreator creator : EcoreUtil2.getAllContentsOfType(method, OperatorStyleCreator.class))
        {
            checkCreator(creator, searchWords, resultAceptor);
        }
    }

    private void checkStatement(SimpleStatement statement, Pattern searchWords, ResultAcceptor resultAceptor)
    {
        if (!(statement.getRight() instanceof StringLiteral value) || !isSecretValue(value))
        {
            return;
        }
        String message;
        if (statement.getLeft() instanceof StaticFeatureAccess variable
            && searchWords.matcher(variable.getName().toLowerCase(Locale.ROOT)).matches())
        {
            message = MessageFormat.format(Messages.UsingHardcodeSecretInformationCheck_Hardcode_secret,
                UsingHardcodePathCheck.literalContent(value));
            resultAceptor.addIssue(message, statement);
        }
        else if (statement.getLeft() instanceof IndexAccess indexAccess
            && indexAccess.getIndex() instanceof StringLiteral key
            && searchWords.matcher(UsingHardcodePathCheck.literalContent(key).toLowerCase(Locale.ROOT)).matches())
        {
            message = MessageFormat.format(Messages.UsingHardcodeSecretInformationCheck_Hardcode_secret,
                UsingHardcodePathCheck.literalContent(key));
            resultAceptor.addIssue(message, statement);
        }
        else if (statement.getLeft() instanceof DynamicFeatureAccess property
            && searchWords.matcher(property.getName().toLowerCase(Locale.ROOT)).matches())
        {
            message = MessageFormat.format(Messages.UsingHardcodeSecretInformationCheck_Hardcode_secret,
                property.getName());
            resultAceptor.addIssue(message, statement);
        }
    }

    private void checkInsertInvocation(Invocation invocation, Pattern searchWords, ResultAcceptor resultAceptor)
    {
        if (!(invocation.getMethodAccess() instanceof DynamicFeatureAccess methodAccess))
        {
            return;
        }
        String methodName = methodAccess.getName().toLowerCase(Locale.ROOT);
        if (!INSERT_RU.equals(methodName) && !INSERT_EN.equals(methodName))
        {
            return;
        }
        List<Expression> params = invocation.getParams();
        if (params.size() < 2 || !(params.get(0) instanceof StringLiteral key)
            || !(params.get(1) instanceof StringLiteral value) || !isSecretValue(value))
        {
            return;
        }
        if (searchWords.matcher(UsingHardcodePathCheck.literalContent(key).toLowerCase(Locale.ROOT)).matches())
        {
            String message = MessageFormat.format(Messages.UsingHardcodeSecretInformationCheck_Hardcode_secret,
                UsingHardcodePathCheck.literalContent(key));
            resultAceptor.addIssue(message, invocation);
        }
    }

    private void checkCreator(OperatorStyleCreator creator, Pattern searchWords, ResultAcceptor resultAceptor)
    {
        String typeName = typeName(creator);
        if (typeName == null)
        {
            return;
        }
        List<Expression> params = creator.getParams();
        if (isConnectionType(typeName))
        {
            if (params.size() > CONNECTION_PASSWORD_PARAM_INDEX
                && params.get(CONNECTION_PASSWORD_PARAM_INDEX) instanceof StringLiteral password
                && isSecretValue(password))
            {
                String message = MessageFormat.format(
                    Messages.UsingHardcodeSecretInformationCheck_Hardcode_secret_in_connection, typeName);
                resultAceptor.addIssue(message, creator);
            }
            return;
        }
        if (!isKeyType(typeName) || params.size() < 2 || !(params.get(0) instanceof StringLiteral key)
            || !(params.get(1) instanceof StringLiteral value) || !isSecretValue(value))
        {
            return;
        }
        if (searchWords.matcher(UsingHardcodePathCheck.literalContent(key).toLowerCase(Locale.ROOT)).matches())
        {
            String message = MessageFormat.format(Messages.UsingHardcodeSecretInformationCheck_Hardcode_secret,
                UsingHardcodePathCheck.literalContent(key));
            resultAceptor.addIssue(message, creator);
        }
    }

    private static final String REGEX_TYPE_FROM_TEXT = "(?iu)\\b(?:Новый|New)\\s+([А-Яа-яA-Za-z0-9_]+)"; //$NON-NLS-1$

    private static String typeName(OperatorStyleCreator creator)
    {
        if (creator.getType() != null && creator.getType().getName() != null)
        {
            return creator.getType().getName();
        }
        // тип мог не разрешиться в типовой системе — берём имя из текста «Новый <Тип>»
        String text = NodeModelUtils.findActualNodeFor(creator).getText();
        Pattern typeFromText = UsingHardcodePathCheck.compileCaseInsensitive(REGEX_TYPE_FROM_TEXT);
        Matcher matcher = typeFromText.matcher(text);
        if (matcher.find())
        {
            return matcher.group(1);
        }
        return null;
    }

    private static boolean isKeyType(String typeName)
    {
        String lower = typeName.toLowerCase(Locale.ROOT);
        return STRUCTURE_RU.equals(lower) || STRUCTURE_EN.equals(lower) || MAP_RU.equals(lower)
            || MAP_EN.equals(lower);
    }

    private static boolean isConnectionType(String typeName)
    {
        String lower = typeName.toLowerCase(Locale.ROOT);
        return lower.endsWith(CONNECTION_RU) || lower.endsWith("connection"); //$NON-NLS-1$
    }

    /**
     * Значение — непустая строка, не состоящая только из маскирующих «*».
     *
     * @param literal литерал, не может быть {@code null}.
     * @return {@code true}, если значение похоже на секрет.
     */
    private static boolean isSecretValue(StringLiteral literal)
    {
        String content = UsingHardcodePathCheck.literalContent(literal).trim();
        return !content.isEmpty() && !content.chars().allMatch(c -> c == '*');
    }

    private Pattern searchWordsPattern(ICheckParameters parameters)
    {
        String value;
        try
        {
            value = parameters.getString(PARAM_SEARCH_WORDS);
        }
        catch (WrongParameterException e)
        {
            value = DEFAULT_SEARCH_WORDS;
        }
        if (value == null || value.isBlank())
        {
            value = DEFAULT_SEARCH_WORDS;
        }
        return UsingHardcodePathCheck.compileCaseInsensitive("^(" + value.trim() + ")$"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
