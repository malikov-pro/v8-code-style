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
 *     malikov-pro - port of the BSL Language Server diagnostic UsingHardcodePath
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Module;
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
 * Проверка: хранение пути к файлу/каталогу в коде («C:\Program Files\»,
 * «/etc/…»). Путь зависит от машины, на которой выполняется код.
 * <p>
 * Перенос диагностики BSL Language Server UsingHardcodePath
 * (тип ERROR, серьёзность CRITICAL). Как и в LS: URL (ftp/http/https)
 * не проверяются; пути, начинающиеся с «/», проверяются только на стандартные
 * корневые каталоги Unix (параметр). Проверка не имеет quick fix: решение
 * о способе хранения пути — за пользователем (настройка/константа).
 *
 * @author malikov-pro
 */
public class UsingHardcodePathCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "using-hardcode-path"; //$NON-NLS-1$

    private static final String REGEX_PATH =
        "^(?=\\/).*|^(%.*%)(?=\\\\|\\/|\\/\\/)|^(~)(?=\\\\|\\/|\\/\\/)|(^([a-z]):" //$NON-NLS-1$
            + "(?=\\\\|\\/\\/(?![\0-\37<>:\"\\/\\\\|?*])|\\/(?![\0-\37<>:\"\\/\\\\|?*])|$)|^\\\\(?=[\\\\\\/]" //$NON-NLS-1$
            + "[^\0-\37<>:\"\\/\\\\|?*]+)|^(?=(\\\\|\\/|\\/\\/)$)^\\.(?=(\\\\|\\/|\\/\\/)[^\0-\37<>:\"\\/\\\\|?*]+))" //$NON-NLS-1$
            + "((\\\\|\\/|\\/\\/)[^\0-\37<>:\"\\/\\\\|?*]+|(\\\\|\\/|\\/\\/)$)*()$"; //$NON-NLS-1$

    private static final String DEFAULT_STD_PATHS_UNIX =
        "bin|boot|dev|etc|home|lib|lost\\+found|misc|mnt|media|opt|proc|root|run|sbin|tmp|usr|var"; //$NON-NLS-1$

    private static final Pattern PATTERN_PATH = compileCaseInsensitive(REGEX_PATH);

    private static final Pattern PATTERN_URL = compileCaseInsensitive("^(ftp|http|https):\\/\\/[^ \"].*"); //$NON-NLS-1$

    private static final String PARAM_STD_PATHS_UNIX = "searchWordsStdPathsUnix"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public UsingHardcodePathCheck()
    {
        super();
    }

    /**
     * Возвращает содержимое строкового литерала без кавычек.
     *
     * @param literal литерал, не может быть {@code null}.
     * @return содержимое литерала.
     */
    public static String literalContent(StringLiteral literal)
    {
        // узел литерала включает скрытый whitespace (пробел после «=») — обрезать
        String text = NodeModelUtils.findActualNodeFor(literal).getText().trim();
        if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            text = text.substring(1, text.length() - 1);
        }
        return text.replace("\"\"", "\""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Компилирует регулярное выражение без учёта регистра (аналог
     * CaseInsensitivePattern из BSL LS).
     *
     * @param regex выражение, не может быть {@code null}.
     * @return скомпилированный шаблон.
     */
    public static Pattern compileCaseInsensitive(String regex)
    {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.UsingHardcodePathCheck_title)
            .description(Messages.UsingHardcodePathCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE)
            .parameter(PARAM_STD_PATHS_UNIX, String.class, DEFAULT_STD_PATHS_UNIX,
                Messages.UsingHardcodePathCheck_Std_paths_unix);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Module module = (Module)object;

        Pattern stdPaths = stdPathsPattern(parameters);

        for (StringLiteral literal : EcoreUtil2.getAllContentsOfType(module, StringLiteral.class))
        {
            String content = literalContent(literal);
            // как и в LS: слишком короткие строки и URL не рассматриваются
            if (content.length() <= 3 || PATTERN_URL.matcher(content).find())
            {
                continue;
            }
            Matcher matcher = PATTERN_PATH.matcher(content);
            if (!matcher.find())
            {
                continue;
            }
            if (content.startsWith("/") && !stdPaths.matcher(content).find()) //$NON-NLS-1$
            {
                // Unix-путь вне стандартных корневых каталогов не фиксируется
                continue;
            }
            String message = MessageFormat.format(Messages.UsingHardcodePathCheck_Hardcode_path, content);
            resultAceptor.addIssue(message, literal);
        }
    }

    private Pattern stdPathsPattern(ICheckParameters parameters)
    {
        String value;
        try
        {
            value = parameters.getString(PARAM_STD_PATHS_UNIX);
        }
        catch (WrongParameterException e)
        {
            value = DEFAULT_STD_PATHS_UNIX;
        }
        if (value == null || value.isBlank())
        {
            value = DEFAULT_STD_PATHS_UNIX;
        }
        return compileCaseInsensitive("^\\/(" + value.trim() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
