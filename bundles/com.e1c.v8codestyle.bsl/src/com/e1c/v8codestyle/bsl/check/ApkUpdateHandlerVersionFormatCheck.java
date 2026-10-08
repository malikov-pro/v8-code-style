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
 *     malikov-pro - port of the APK check АПК_01190
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.SIMPLE_STATEMENT;

import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.StringLiteral;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: номер версии в обработчике обновления данных ИБ указывается
 * присваиванием {@code Обработчик.Версия = "..."} в процедуре
 * {@code ПриДобавленииОбработчиковОбновления} (англ. {@code OnAddUpdateHandlers})
 * и должен иметь формат «Р.П.В.С» или «Р.П.В» (цифровые сегменты, разделённые
 * точками, не менее трёх) либо «*» — выполнять при каждом обновлении.
 * <p>
 * Перенос проверки АПК_01190. Не помечаются: версия «*», пустая строка
 * (АПК её пропускает; стандарт требует при этом свойства НачальноеЗаполнение),
 * а также присваивания не строкового литерала и присваивания свойству
 * {@code Версия} вне процедуры регистрации обработчиков. Имя переменной
 * слева не проверяется — статически не всякий обработчик называется
 * {@code Обработчик}.
 *
 * @author malikov-pro
 */
public class ApkUpdateHandlerVersionFormatCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-01190-update-version-format"; //$NON-NLS-1$

    // ПриДобавленииОбработчиковОбновления / англ. вариант БСП OnAddUpdateHandlers.
    private static final String HANDLERS_METHOD_RU = "придобавленииобработчиковобновления"; //$NON-NLS-1$
    private static final String HANDLERS_METHOD_EN = "onaddupdatehandlers"; //$NON-NLS-1$

    private static final Set<String> VERSION_PROPERTY_NAMES = Set.of("версия", "version"); //$NON-NLS-1$ //$NON-NLS-2$

    private static final String STAR_VERSION = "*"; //$NON-NLS-1$
    private static final int MIN_VERSION_SEGMENTS = 3;

    /**
     * Instantiates a new check.
     */
    public ApkUpdateHandlerVersionFormatCheck()
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
        builder.title(Messages.ApkUpdateHandlerVersionFormatCheck_title)
            .description(Messages.ApkUpdateHandlerVersionFormatCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(SIMPLE_STATEMENT);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (monitor.isCanceled() || !(object instanceof SimpleStatement statement))
        {
            return;
        }

        Method method = EcoreUtil2.getContainerOfType(statement, Method.class);
        if (!isHandlerRegistrationMethod(method))
        {
            return;
        }

        if (!(statement.getLeft() instanceof DynamicFeatureAccess versionProperty))
        {
            return;
        }
        String propertyName = versionProperty.getName();
        if (propertyName == null || !VERSION_PROPERTY_NAMES.contains(propertyName.toLowerCase(Locale.ROOT)))
        {
            return;
        }

        if (!(statement.getRight() instanceof StringLiteral literal))
        {
            return;
        }

        String version = String.join("", literal.lines(true)); //$NON-NLS-1$
        if (isValidVersion(version))
        {
            return;
        }
        resultAceptor.addIssue(Messages.ApkUpdateHandlerVersionFormatCheck_Invalid_version_format, statement);
    }

    private static boolean isHandlerRegistrationMethod(Method method)
    {
        if (method == null)
        {
            return false;
        }
        String methodName = method.getName();
        if (methodName == null)
        {
            return false;
        }
        String name = methodName.toLowerCase(Locale.ROOT);
        return HANDLERS_METHOD_RU.equals(name) || HANDLERS_METHOD_EN.equals(name);
    }

    private static boolean isValidVersion(String version)
    {
        if (STAR_VERSION.equals(version) || version.isEmpty())
        {
            return true;
        }
        String[] segments = version.split("\\.", -1); //$NON-NLS-1$
        if (segments.length < MIN_VERSION_SEGMENTS)
        {
            return false;
        }
        for (String segment : segments)
        {
            if (!isDigits(segment))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean isDigits(String segment)
    {
        if (segment.isEmpty())
        {
            return false;
        }
        for (int i = 0; i < segment.length(); i++)
        {
            char c = segment.charAt(i);
            if (c < '0' || c > '9')
            {
                return false;
            }
        }
        return true;
    }
}
