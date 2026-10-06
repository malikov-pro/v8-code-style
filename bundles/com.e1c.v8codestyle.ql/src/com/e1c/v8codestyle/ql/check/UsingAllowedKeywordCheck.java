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
 *     malikov-pro - port of the APK check АПК_00429
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check;

import static com._1c.g5.v8.dt.ql.model.QlPackage.Literals.QUERY_SCHEMA_OPERATOR__SELECT_ALLOWED;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.dt.ql.model.QuerySchemaOperator;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.g5.v8.dt.ql.check.QlBasicDelegateCheck;
import com.e1c.v8codestyle.check.OptInCheckExtension;

/**
 * Проверка-аудит: использование ключевого слова РАЗРЕШЕННЫЕ (ALLOWED) в запросах.
 * РАЗРЕШЕННЫЕ скрывает строки, к которым у пользователя нет прав, что может
 * искажать результат запроса: пользователь видит не все данные, а бизнес-логика
 * рассчитывает на полные данные. Допустимо там, где пользователь обязан видеть
 * всё разрешённое ему (динамические списки, интерактивные отчёты), и
 * недопустимо в запросах, влияющих на расчёты и проведение операций.
 * <p>
 * Перенос проверки АПК_00429 (статья 415 стандарта 1С). Проверка по сути
 * аудит: законность использования зависит от роли запроса в приложении, что
 * статически не доказуемо, поэтому каждое использование только помечается.
 * Так как РАЗРЕШЕННЫЕ легитимно в динамических списках, проверка выключена
 * по умолчанию ({@link OptInCheckExtension}) — включается пользователем для
 * аудита проекта.
 *
 * @author malikov-pro
 */
public class UsingAllowedKeywordCheck
    extends QlBasicDelegateCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00429-query-allowed-keyword"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public UsingAllowedKeywordCheck()
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
        builder.title(Messages.UsingAllowedKeywordCheck_title)
            .description(Messages.UsingAllowedKeywordCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.SECURITY)
            .extension(new OptInCheckExtension())
            .delegate(QuerySchemaOperator.class);
    }

    @Override
    protected void checkQlObject(EObject object, QueryOwner owner, IQlResultAcceptor resultAceptor,
        ICheckParameters parameters, IProgressMonitor monitor)
    {
        QuerySchemaOperator operator = (QuerySchemaOperator)object;
        if (operator.isSelectAllowed())
        {
            resultAceptor.addIssue(Messages.UsingAllowedKeywordCheck_Allowed_keyword_usage_should_be_verified, object,
                QUERY_SCHEMA_OPERATOR__SELECT_ALLOWED);
        }
    }
}
