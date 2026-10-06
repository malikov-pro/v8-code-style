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
 *     malikov-pro - port of the BSL Language Server diagnostic IdenticalExpressions
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.BINARY_EXPRESSION;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.BinaryExpression;
import com._1c.g5.v8.dt.bsl.model.BinaryOperation;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.WrongParameterException;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: одинаковые подвыражения слева и справа от бинарного оператора,
 * например {@code Если А = А} или {@code В = А = 1 И А = 1}. Обычно это
 * ошибка: оператор не делает то, что ожидал автор.
 * <p>
 * Перенос диагностики BSL Language Server IdenticalExpressions
 * (тип ERROR, серьёзность MAJOR). Упрощение относительно LS (expression tree):
 * сравнивается текст левого и правого операнда каждого бинарного выражения
 * (без учёта регистра и пробелов); цепочки вида {@code А = Б ИЛИ А <> Б}
 * не анализируются. Как и в LS, повтор операнда в сложении и умножении
 * ({@code А + А}) допустим; деление на популярные делители (параметр,
 * по умолчанию «60, 1024») ошибкой не считается.
 *
 * @author malikov-pro
 */
public class IdenticalExpressionsCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "identical-expressions"; //$NON-NLS-1$

    private static final String PARAM_POPULAR_DIVISORS = "popularDivisors"; //$NON-NLS-1$

    private static final String DEFAULT_POPULAR_DIVISORS = "60, 1024"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public IdenticalExpressionsCheck()
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
        builder.title(Messages.IdenticalExpressionsCheck_title)
            .description(Messages.IdenticalExpressionsCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(BINARY_EXPRESSION)
            .parameter(PARAM_POPULAR_DIVISORS, String.class, DEFAULT_POPULAR_DIVISORS,
                Messages.IdenticalExpressionsCheck_Popular_divisors);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        BinaryExpression expression = (BinaryExpression)object;
        BinaryOperation operation = expression.getOperation();
        // повтор операнда в сложении/умножении (А + А — удвоение) допустим, как и в LS
        if (operation == BinaryOperation.PLUS || operation == BinaryOperation.MULTIPLY)
        {
            return;
        }
        Expression left = expression.getLeft();
        Expression right = expression.getRight();
        if (left == null || right == null)
        {
            return;
        }
        String leftText = expressionText(left);
        String rightText = expressionText(right);
        if (leftText.isEmpty() || rightText.isEmpty())
        {
            return;
        }
        if (!normalize(leftText).equals(normalize(rightText)))
        {
            return;
        }
        if (operation == BinaryOperation.DIVIDE && isPopularDivisor(leftText, parameters))
        {
            return;
        }
        String operator = operatorText(expression, left, right);
        String message = MessageFormat.format(Messages.IdenticalExpressionsCheck_Identical_expressions, operator,
            leftText);
        resultAceptor.addIssue(message, expression);
    }

    private static boolean isPopularDivisor(String leftText, ICheckParameters parameters)
    {
        String value;
        try
        {
            value = parameters.getString(PARAM_POPULAR_DIVISORS);
        }
        catch (WrongParameterException e)
        {
            value = DEFAULT_POPULAR_DIVISORS;
        }
        if (value == null || value.isBlank())
        {
            return false;
        }
        Set<String> divisors = Arrays.stream(value.split(",")) //$NON-NLS-1$
            .map(String::trim)
            .collect(Collectors.toSet());
        return divisors.contains(leftText);
    }

    /**
     * Текст оператора как он написан в модуле — сегмент между операндами.
     *
     * @param expression бинарное выражение, не может быть {@code null}.
     * @param left левый операнд, не может быть {@code null}.
     * @param right правый операнд, не может быть {@code null}.
     * @return текст оператора, например «=» или «И».
     */
    private static String operatorText(BinaryExpression expression, Expression left, Expression right)
    {
        INode node = NodeModelUtils.findActualNodeFor(expression);
        INode leftNode = NodeModelUtils.findActualNodeFor(left);
        INode rightNode = NodeModelUtils.findActualNodeFor(right);
        int start = leftNode.getTotalEndOffset();
        int end = rightNode.getTotalOffset();
        int nodeStart = node.getTotalOffset();
        int nodeEnd = node.getTotalEndOffset();
        if (start < nodeStart || end > nodeEnd || end < start)
        {
            return expression.getOperation().getName();
        }
        String operator = node.getText().substring(start - nodeStart, end - nodeStart).trim();
        return operator.isEmpty() ? expression.getOperation().getName() : operator;
    }

    private static String expressionText(Expression expression)
    {
        INode node = NodeModelUtils.findActualNodeFor(expression);
        return node == null ? "" : node.getText().trim(); //$NON-NLS-1$
    }

    private static String normalize(String text)
    {
        return text.trim().replaceAll("\\s+", " ") //$NON-NLS-1$ //$NON-NLS-2$
            .toLowerCase(Locale.ROOT);
    }
}
