/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: malikov-pro
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the APK check АПК_01205
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FEATURE_ACCESS__NAME;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: при использовании в конфигурации Библиотеки стандартных подсистем
 * код основного языка следует получать функцией {@code КодОсновногоЯзыка()}
 * общего модуля {@code ОбщегоНазначения}, а не обращением
 * {@code Метаданные.ОсновнойЯзык} (например,
 * {@code НСтр("ru = '...'", Метаданные.ОсновнойЯзык.КодЯзыка)}).
 * <p>
 * Перенос проверки АПК_01205 (раздел 1.3 стандарта «Обработчики обновления
 * данных информационной базы» и статьи 443). Предусловие АПК «в конфигурации
 * есть БСП» статически не воспроизводится и не проверяется.
 * <p>
 * Эвристика: начало цепочки — {@code StaticFeatureAccess} с именем
 * «Метаданные»/«Metadata» без получателя считается обращением к глобальному
 * свойству; помечается только сегмент «ОсновнойЯзык»/«MainLanguage» цепочки
 * ({@code Метаданные.ОсновнойЯзыК.КодЯзыка} даёт одно замечание).
 * Единственное исключение — функция {@code КодОсновногоЯзыка}
 * (англ. {@code MainLanguageCode}) общего модуля {@code ОбщегоНазначения}
 * (англ. {@code Common}) самого модуля БСП.
 *
 * @author malikov-pro
 */
public class ApkMainLanguageCodeCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-01205-main-language-code"; //$NON-NLS-1$

    private static final String METADATA_GLOBAL_RU = "метаданные"; //$NON-NLS-1$
    private static final String METADATA_GLOBAL_EN = "metadata"; //$NON-NLS-1$

    private static final String MAIN_LANGUAGE_RU = "основнойязык"; //$NON-NLS-1$
    private static final String MAIN_LANGUAGE_EN = "mainlanguage"; //$NON-NLS-1$

    //@formatter:off
    private static final Set<String> ALLOWED_MODULE_NAMES =
        Set.of("общегоназначения", "common"); //$NON-NLS-1$ //$NON-NLS-2$
    private static final Set<String> ALLOWED_METHOD_NAMES =
        Set.of("кодосновногоязыка", "mainlanguagecode"); //$NON-NLS-1$ //$NON-NLS-2$
    //@formatter:on

    /**
     * Instantiates a new check.
     */
    public ApkMainLanguageCodeCheck()
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
        builder.title(Messages.ApkMainLanguageCodeCheck_title)
            .description(Messages.ApkMainLanguageCodeCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Module module = (Module)object;
        for (DynamicFeatureAccess featureAccess : EcoreUtil2.eAllOfType(module, DynamicFeatureAccess.class))
        {
            if (progressMonitor.isCanceled())
            {
                return;
            }
            if (!(featureAccess.getSource() instanceof StaticFeatureAccess source))
            {
                continue;
            }
            String sourceName = source.getName().toLowerCase(Locale.ROOT);
            if (!METADATA_GLOBAL_RU.equals(sourceName) && !METADATA_GLOBAL_EN.equals(sourceName))
            {
                continue;
            }
            String featureName = featureAccess.getName().toLowerCase(Locale.ROOT);
            if (!MAIN_LANGUAGE_RU.equals(featureName) && !MAIN_LANGUAGE_EN.equals(featureName))
            {
                continue;
            }
            if (isAllowedInCodeOfMainLanguageFunction(module, featureAccess))
            {
                continue;
            }
            resultAceptor.addIssue(Messages.ApkMainLanguageCodeCheck_Use_MainLanguageCode_function, featureAccess,
                FEATURE_ACCESS__NAME);
        }
    }

    private boolean isAllowedInCodeOfMainLanguageFunction(Module module, DynamicFeatureAccess featureAccess)
    {
        // Получение основного языка через метаданные разрешено только в функции
        // КодОсновногоЯзыка общего модуля ОбщегоНазначения (англ. Common.MainLanguageCode).
        if (!(module.getOwner() instanceof CommonModule commonModule))
        {
            return false;
        }
        String moduleName = commonModule.getName();
        if (moduleName == null || !ALLOWED_MODULE_NAMES.contains(moduleName.toLowerCase(Locale.ROOT)))
        {
            return false;
        }
        Method method = EcoreUtil2.getContainerOfType(featureAccess, Method.class);
        if (method == null)
        {
            return false;
        }
        String methodName = method.getName();
        return methodName != null && ALLOWED_METHOD_NAMES.contains(methodName.toLowerCase(Locale.ROOT));
    }
}
