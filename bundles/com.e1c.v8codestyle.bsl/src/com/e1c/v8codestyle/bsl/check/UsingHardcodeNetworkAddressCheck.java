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
 *     malikov-pro - port of the BSL Language Server diagnostic UsingHardcodeNetworkAddress
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.Statement;
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
 * Проверка: хранение IP-адреса (IPv4/IPv6) в коде. Адрес зависит от
 * окружения, на котором выполняется код.
 * <p>
 * Перенос диагностики BSL Language Server UsingHardcodeNetworkAddress
 * (тип VULNERABILITY, серьёзность CRITICAL). Как и в LS: URL не проверяются;
 * значения, похожие на версии (параметр popularVersionExclusion), и
 * выражения со словами-исключениями (параметр searchWordsExclusion:
 * Верси/Version/Драйвер/Driver/…) не фиксируются. Quick fix не предусмотрен:
 * способ хранения адреса — решение пользователя (настройка/константа).
 *
 * @author malikov-pro
 */
public class UsingHardcodeNetworkAddressCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "using-hardcode-network-address"; //$NON-NLS-1$

    private static final String REGEX_NETWORK_ADDRESS =
        "(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:" //$NON-NLS-1$
            + "|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}" //$NON-NLS-1$
            + "|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}" //$NON-NLS-1$
            + "|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})" //$NON-NLS-1$
            + "|(?<![g-zа-яА-ЯёЁ]):((:[0-9a-fA-F]{1,4}){1,7}|\\s:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}" //$NON-NLS-1$
            + "|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]" //$NON-NLS-1$
            + "|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]" //$NON-NLS-1$
            + "|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))" //$NON-NLS-1$
            + "|((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])"; //$NON-NLS-1$

    private static final int DOTS_IN_IPV4 = 3;

    private static final Pattern PATTERN_NETWORK_ADDRESS =
        UsingHardcodePathCheck.compileCaseInsensitive(REGEX_NETWORK_ADDRESS);

    private static final Pattern PATTERN_URL =
        UsingHardcodePathCheck.compileCaseInsensitive("^(ftp|http|https):\\/\\/[^ \"].*"); //$NON-NLS-1$

    private static final Pattern PATTERN_ALPHABET = UsingHardcodePathCheck.compileCaseInsensitive("[A-zА-я]"); //$NON-NLS-1$

    private static final String DEFAULT_EXCLUSION_WORDS =
        "Верси|Version|ЗапуститьПриложение|RunApp|Пространств|Namespace|Драйвер|Driver"; //$NON-NLS-1$

    private static final String DEFAULT_POPULAR_VERSION = "^(1|2|3|8\\.3|11)\\."; //$NON-NLS-1$

    private static final String PARAM_EXCLUSION_WORDS = "searchWordsExclusion"; //$NON-NLS-1$

    private static final String PARAM_POPULAR_VERSION = "searchPopularVersionExclusion"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public UsingHardcodeNetworkAddressCheck()
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
        builder.title(Messages.UsingHardcodeNetworkAddressCheck_title)
            .description(Messages.UsingHardcodeNetworkAddressCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.SECURITY)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE)
            .parameter(PARAM_EXCLUSION_WORDS, String.class, DEFAULT_EXCLUSION_WORDS,
                Messages.UsingHardcodeNetworkAddressCheck_Exclusion_words)
            .parameter(PARAM_POPULAR_VERSION, String.class, DEFAULT_POPULAR_VERSION,
                Messages.UsingHardcodeNetworkAddressCheck_Popular_version_exclusion);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Module module = (Module)object;

        Pattern exclusionWords = parameterPattern(parameters, PARAM_EXCLUSION_WORDS, DEFAULT_EXCLUSION_WORDS);
        Pattern popularVersion = parameterPattern(parameters, PARAM_POPULAR_VERSION, DEFAULT_POPULAR_VERSION);

        for (StringLiteral literal : EcoreUtil2.getAllContentsOfType(module, StringLiteral.class))
        {
            String content = UsingHardcodePathCheck.literalContent(literal);
            if (content.length() <= 2 || PATTERN_URL.matcher(content).find())
            {
                continue;
            }
            Matcher matcher = PATTERN_NETWORK_ADDRESS.matcher(content);
            if (!matcher.find())
            {
                continue;
            }
            String firstValue = matcher.group(0);
            long dotsInValue = firstValue.chars().filter(c -> c == '.').count();
            long dotsInContent = content.chars().filter(c -> c == '.').count();
            boolean alphabetInValue = PATTERN_ALPHABET.matcher(firstValue).find();

            // «192.168.0.1.2» или значение с буквами — не IPv4 (версия и т.п.)
            if (dotsInValue > 0 && (dotsInContent > DOTS_IN_IPV4 || alphabetInValue))
            {
                continue;
            }
            if (statementContains(literal, exclusionWords) || popularVersion.matcher(content).find())
            {
                continue;
            }
            String message = MessageFormat.format(Messages.UsingHardcodeNetworkAddressCheck_Hardcode_ip_address,
                content);
            resultAceptor.addIssue(message, literal);
        }
    }

    /**
     * Содержит ли объемлющий оператор слова-исключения (аналог skipStatement
     * в LS): текст оператора проверяется шаблоном исключений.
     *
     * @param literal литерал, не может быть {@code null}.
     * @param exclusionWords шаблон исключений, не может быть {@code null}.
     * @return {@code true}, если оператор содержит слово-исключение.
     */
    private static boolean statementContains(StringLiteral literal, Pattern exclusionWords)
    {
        EObject ancestor = literal.eContainer();
        while (ancestor != null && !(ancestor instanceof Statement))
        {
            ancestor = ancestor.eContainer();
        }
        if (ancestor == null)
        {
            return false;
        }
        String statementText = NodeModelUtils.findActualNodeFor(ancestor).getText();
        return exclusionWords.matcher(statementText).find();
    }

    private static Pattern parameterPattern(ICheckParameters parameters, String paramName, String defaultValue)
    {
        String value;
        try
        {
            value = parameters.getString(paramName);
        }
        catch (WrongParameterException e)
        {
            value = defaultValue;
        }
        if (value == null || value.isBlank())
        {
            value = defaultValue;
        }
        return UsingHardcodePathCheck.compileCaseInsensitive(value.trim());
    }
}
