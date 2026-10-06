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
 *     malikov-pro - port of the BSL Language Server diagnostic FunctionShouldHaveReturn
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FUNCTION;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.NAMED_ELEMENT__NAME;

import java.text.MessageFormat;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.Function;
import com._1c.g5.v8.dt.bsl.model.ReturnStatement;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: функция должна содержать хотя бы один оператор «Возврат».
 * <p>
 * Функция без «Возврат» неявно возвращает «Неопределено», что обычно является
 * ошибкой: такой метод должен вернуть значение либо быть переписан
 * в процедуру. Платформа 1С этот случай не диагностирует.
 * <p>
 * Перенос диагностики BSL Language Server FunctionShouldHaveReturn
 * (тип ERROR, серьёзность MAJOR).
 *
 * @author malikov-pro
 */
public class FunctionShouldHaveReturnCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "function-should-have-return"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public FunctionShouldHaveReturnCheck()
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
        builder.title(Messages.FunctionShouldHaveReturnCheck_title)
            .description(Messages.FunctionShouldHaveReturnCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(FUNCTION);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Function function = (Function)object;
        List<ReturnStatement> returns = EcoreUtil2.getAllContentsOfType(function, ReturnStatement.class);
        if (!returns.isEmpty())
        {
            return;
        }
        String message = MessageFormat.format(
            Messages.FunctionShouldHaveReturnCheck_Function_has_no_Return_statement, function.getName());
        resultAceptor.addIssue(message, function, NAMED_ELEMENT__NAME);
    }
}
