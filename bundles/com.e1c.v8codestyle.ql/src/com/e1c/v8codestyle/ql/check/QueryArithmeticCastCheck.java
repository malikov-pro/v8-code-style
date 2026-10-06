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
 *     malikov-pro - port of the APK check АПК_00212
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check;

import static com._1c.g5.v8.dt.ql.model.QlPackage.Literals.ABINARY_OPERATORS_EXPRESSION__RIGHT;
import static com._1c.g5.v8.dt.ql.model.QlPackage.Literals.FUNCTION_INVOCATION_EXPRESSION__PARAMS;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.ql.model.AbstractExpression;
import com._1c.g5.v8.dt.ql.model.BracketCommonExpression;
import com._1c.g5.v8.dt.ql.model.CastOperationExpression;
import com._1c.g5.v8.dt.ql.model.CastingNumberType;
import com._1c.g5.v8.dt.ql.model.CommonDevOperatorExpression;
import com._1c.g5.v8.dt.ql.model.FieldWithCasting;
import com._1c.g5.v8.dt.ql.model.FunctionExpression;
import com._1c.g5.v8.dt.ql.model.FunctionInvocationExpression;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.g5.v8.dt.ql.check.QlBasicDelegateCheck;
import com.e1c.v8codestyle.check.OptInCheckExtension;

/**
 * Проверка-аудит: результат арифметической операции деления или агрегатной
 * функции СРЕДНЕЕ (AVG) в запросе не обрамлён оператором
 * ВЫРАЗИТЬ(... КАК Число(m, n)). Из-за особенностей работы различных СУБД
 * точность таких вычислений может отличаться от 8 разрядов дробной части,
 * что приводит к потере точности результатов на некоторых СУБД.
 * <p>
 * Перенос проверки АПК_00212. Проверка по сути аудит: необходимая разрядность
 * и точность зависят от прикладного решения (какие порядки чисел участвуют,
 * сколько разрядов требуется в результате), что статически не доказуемо,
 * поэтому каждый случай только помечается. Проверка выключена по умолчанию
 * ({@link OptInCheckExtension}) — включается пользователем для аудита проекта.
 * <p>
 * Осознанные упрощения против алгоритма АПК:
 * <ul>
 * <li>умножение не проверяется — статически не определить, может ли каждый
 * из множителей иметь дробную часть;</li>
 * <li>порядок операндов деления (избегать деления числа малого порядка на
 * число большого порядка) не проверяется — нужны порядки чисел;</li>
 * <li>достаточность и минимальность указанной разрядности ВЫРАЗИТЬ не
 * оценивается.</li>
 * </ul>
 *
 * @author malikov-pro
 */
public class QueryArithmeticCastCheck
    extends QlBasicDelegateCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00212-query-arithmetic-cast"; //$NON-NLS-1$

    private static final String AVG_FUNCTION_NAME = "AVG"; //$NON-NLS-1$

    private static final String AVG_FUNCTION_NAME_RU = "СРЕДНЕЕ"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public QueryArithmeticCastCheck()
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
        builder.title(Messages.QueryArithmeticCastCheck_title)
            .description(Messages.QueryArithmeticCastCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.PORTABILITY)
            .extension(new OptInCheckExtension())
            .delegate(CommonDevOperatorExpression.class, FunctionInvocationExpression.class);
    }

    @Override
    protected void checkQlObject(EObject object, QueryOwner owner, IQlResultAcceptor resultAceptor,
        ICheckParameters parameters, IProgressMonitor monitor)
    {
        if (object instanceof CommonDevOperatorExpression)
        {
            checkDivision((CommonDevOperatorExpression)object, resultAceptor);
        }
        else if (object instanceof FunctionInvocationExpression)
        {
            checkAverageFunction((FunctionInvocationExpression)object, resultAceptor);
        }
    }

    private void checkDivision(CommonDevOperatorExpression division, IQlResultAcceptor resultAceptor)
    {
        if (isCastToNumberAround(division) || isCastToNumber(division.getLeft())
            || isCastToNumber(division.getRight()))
        {
            return;
        }
        resultAceptor.addIssue(Messages.QueryArithmeticCastCheck_Division_result_should_be_cast_to_number, division,
            ABINARY_OPERATORS_EXPRESSION__RIGHT);
    }

    private void checkAverageFunction(FunctionInvocationExpression function, IQlResultAcceptor resultAceptor)
    {
        FunctionExpression functionType = function.getFunctionType();
        if (!isAverageFunction(functionType) || isCastToNumberAround(function))
        {
            return;
        }
        for (AbstractExpression param : function.getParams())
        {
            if (isCastToNumber(param))
            {
                return;
            }
        }
        resultAceptor.addIssue(Messages.QueryArithmeticCastCheck_Average_function_result_should_be_cast_to_number,
            function, FUNCTION_INVOCATION_EXPRESSION__PARAMS);
    }

    private boolean isAverageFunction(FunctionExpression functionType)
    {
        if (functionType == null)
        {
            return false;
        }
        return AVG_FUNCTION_NAME.equals(functionType.getName()) || AVG_FUNCTION_NAME_RU.equals(functionType.getNameRu());
    }

    /**
     * Checks whether the expression result is wrapped in CAST(... AS Number): the
     * expression itself or one of its parents is a cast operation to a number type.
     */
    private boolean isCastToNumberAround(AbstractExpression expression)
    {
        CastOperationExpression cast = EcoreUtil2.getContainerOfType(expression, CastOperationExpression.class);
        return cast != null && cast.getTypeCast() instanceof CastingNumberType;
    }

    /**
     * Checks whether the expression is a cast operation to a number type,
     * unwrapping brackets and field-with-casting wrappers.
     */
    private boolean isCastToNumber(AbstractExpression expression)
    {
        if (expression instanceof BracketCommonExpression)
        {
            return isCastToNumber(((BracketCommonExpression)expression).getBracketPart());
        }
        if (expression instanceof FieldWithCasting)
        {
            CastOperationExpression cast = ((FieldWithCasting)expression).getCastOperation();
            return cast != null && cast.getTypeCast() instanceof CastingNumberType;
        }
        if (expression instanceof CastOperationExpression)
        {
            return ((CastOperationExpression)expression).getTypeCast() instanceof CastingNumberType;
        }
        return false;
    }

}
