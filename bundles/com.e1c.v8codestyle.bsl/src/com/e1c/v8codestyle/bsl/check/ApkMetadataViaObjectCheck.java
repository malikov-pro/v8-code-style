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
 *     malikov-pro - port of the APK check АПК_00306
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FEATURE_ACCESS__NAME;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: метаданные объекта следует получать методом {@code Метаданные()}
 * самого объекта, а не обращением к свойству глобального контекста
 * {@code Метаданные} (например, {@code Метаданные.Справочники[Имя]},
 * {@code Метаданные.Документы.НайтиПоКоду(...)}) — второй способ существенно
 * медленнее. Исключение — метод {@code НайтиПоТипу} для случаев, когда тип
 * объекта метаданных заранее неизвестен: {@code Метаданные.НайтиПоТипу(...)}
 * не помечается.
 * <p>
 * Перенос проверки АПК_00306 (статья 445 стандарта 1С). Эвристика: начало
 * цепочки — {@code StaticFeatureAccess} с именем «Метаданные»/«Metadata» без
 * получателя считается обращением к глобальному свойству; вызов метода
 * {@code Х.Метаданные()} моделируется как {@code Invocation} с
 * {@code DynamicFeatureAccess} у получателя и не помечается.
 *
 * @author malikov-pro
 */
public class ApkMetadataViaObjectCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00306-metadata-via-object"; //$NON-NLS-1$

    private static final String METADATA_GLOBAL_RU = "метаданные"; //$NON-NLS-1$
    private static final String METADATA_GLOBAL_EN = "metadata"; //$NON-NLS-1$

    private static final String FIND_BY_TYPE_RU = "найтипотипу"; //$NON-NLS-1$
    private static final String FIND_BY_TYPE_EN = "findbytype"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public ApkMetadataViaObjectCheck()
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
        builder.title(Messages.ApkMetadataViaObjectCheck_title)
            .description(Messages.ApkMetadataViaObjectCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.PERFORMANCE)
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
            if (FIND_BY_TYPE_RU.equals(featureName) || FIND_BY_TYPE_EN.equals(featureName))
            {
                continue;
            }
            resultAceptor.addIssue(Messages.ApkMetadataViaObjectCheck_Use_object_Metadata_method, featureAccess,
                FEATURE_ACCESS__NAME);
        }
    }
}
