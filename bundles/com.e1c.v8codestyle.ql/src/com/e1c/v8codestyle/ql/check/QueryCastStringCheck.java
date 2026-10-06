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
 *     malikov-pro - port of the APK check АПК_00142
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check;

import static com._1c.g5.v8.dt.ql.model.QlPackage.Literals.CASTING_STRING_TYPE__LENGTH;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.dt.ql.model.CastingStringType;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.g5.v8.dt.ql.check.QlBasicDelegateCheck;
import com.e1c.v8codestyle.check.OptInCheckExtension;

/**
 * Проверка-аудит: в запросе используется приведение строки к ограниченной
 * длине — ВЫРАЗИТЬ(... КАК СТРОКА(N)) / CAST(... AS STRING(N)). Строки
 * неограниченной длины приходится урезать в запросе при сравнении,
 * группировке и получении различных, однако частое приведение — признак
 * неверного проектного решения: тип строкового реквизита стоит пересмотреть
 * в пользу ограниченной длины строки.
 * <p>
 * Перенос проверки АПК_00142. Проверка по сути аудит: убедиться, что
 * установленной длины строки будет достаточно для верного вычисления
 * выражения, может только разработчик (в АПК это ручной шаг алгоритма),
 * поэтому каждый случай приведения только помечается. Проверка выключена
 * по умолчанию ({@link OptInCheckExtension}) — включается пользователем
 * для аудита проекта. Родственная проверка md-канала
 * {@code apk-00141-unlimited-string} помечает сами реквизиты неограниченной
 * длины; инварианты не пересекаются.
 *
 * @author malikov-pro
 */
public class QueryCastStringCheck
    extends QlBasicDelegateCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00142-query-cast-string"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public QueryCastStringCheck()
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
        builder.title(Messages.QueryCastStringCheck_title)
            .description(Messages.QueryCastStringCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.PORTABILITY)
            .extension(new OptInCheckExtension())
            .delegate(CastingStringType.class);
    }

    @Override
    protected void checkQlObject(EObject object, QueryOwner owner, IQlResultAcceptor resultAceptor,
        ICheckParameters parameters, IProgressMonitor monitor)
    {
        CastingStringType casting = (CastingStringType)object;
        if (casting.getLength() == null)
        {
            // Приведение к СТРОКА без указания длины не урезает строку.
            return;
        }
        resultAceptor.addIssue(Messages.QueryCastStringCheck_Cast_to_limited_string_usage_should_be_verified, object,
            CASTING_STRING_TYPE__LENGTH);
    }

}
