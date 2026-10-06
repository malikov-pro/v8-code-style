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
 *     malikov-pro - port of the APK check АПК_01171
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.LOOP_STATEMENT__STATEMENTS;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.BinaryExpression;
import com._1c.g5.v8.dt.bsl.model.BinaryOperation;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.LoopStatement;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.NumberLiteral;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.UnaryExpression;
import com._1c.g5.v8.dt.bsl.model.UnaryOperation;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: массовая конкатенация строк — аккумуляция строки в цикле
 * через присваивание вида «Переменная = Переменная + ...».
 * <p>
 * Перенос проверки АПК_01171 (статья 782 стандарта 1С). Правило АПК считает
 * конкатенации в рантайме и срабатывает от 1000 операций; статически это
 * непроверяемо, поэтому перенос — приближение: в теле цикла ищутся
 * присваивания, где переменная в левой части участвует и в правой части
 * цепочки сложения (самоприсоединение). Циклы: Пока/Для/Для Каждого.
 * Одно замечание на цикл (якорь — оператор цикла).
 * <p>
 * Упрощение: без вычисления типов не отличить строковую аккумуляцию от
 * числовой, поэтому числовые счётчики вида «Счетчик = Счетчик + 1»
 * (все прочие операнды правой части — числовые литералы) не помечаются;
 * аккумуляция с переменными и вызовами функций помечается и может быть
 * ложным срабатыванием для числовых накоплений. В карточке — рекомендация
 * стандарта: при массовых операциях использовать СтрСоединить/СтрРазделить.
 *
 * @author malikov-pro
 */
public class ApkStringConcatInLoopCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-01171-string-concat-in-loop"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public ApkStringConcatInLoopCheck()
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
        builder.title(Messages.ApkStringConcatInLoopCheck_title)
            .description(Messages.ApkStringConcatInLoopCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
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
        for (LoopStatement loop : EcoreUtil2.getAllContentsOfType(module, LoopStatement.class))
        {
            if (progressMonitor.isCanceled())
            {
                return;
            }
            if (hasSelfAccumulatingAssignment(loop))
            {
                resultAceptor.addIssue(Messages.ApkStringConcatInLoopCheck_String_concatenation_in_loop, loop,
                    LOOP_STATEMENT__STATEMENTS);
            }
        }
    }

    private static boolean hasSelfAccumulatingAssignment(LoopStatement loop)
    {
        for (SimpleStatement statement : EcoreUtil2.getAllContentsOfType(loop, SimpleStatement.class))
        {
            // присваивание приписывается самому глубокому охватывающему циклу,
            // чтобы вложенный цикл не дублировал замечание внешнего
            if (EcoreUtil2.getContainerOfType(statement, LoopStatement.class) != loop)
            {
                continue;
            }
            if (!(statement.getLeft() instanceof StaticFeatureAccess left))
            {
                continue;
            }
            List<Expression> operands = new ArrayList<>();
            collectPlusOperands(statement.getRight(), operands);
            boolean selfReferenced = operands.stream()
                .anyMatch(operand -> isFeatureNamed(operand, left.getName()));
            if (!selfReferenced)
            {
                continue;
            }
            // числовой счётчик «Счетчик = Счетчик + 1» не помечается:
            // все прочие операнды правой части — числовые литералы
            boolean numericCounter = operands.stream()
                .filter(operand -> !isFeatureNamed(operand, left.getName()))
                .allMatch(ApkStringConcatInLoopCheck::isNumericExpression);
            if (!numericCounter)
            {
                return true;
            }
        }
        return false;
    }

    private static void collectPlusOperands(Expression expression, List<Expression> operands)
    {
        if (expression instanceof BinaryExpression binary && binary.getOperation() == BinaryOperation.PLUS)
        {
            collectPlusOperands(binary.getLeft(), operands);
            collectPlusOperands(binary.getRight(), operands);
        }
        else if (expression != null)
        {
            operands.add(expression);
        }
    }

    private static boolean isFeatureNamed(Expression expression, String name)
    {
        return expression instanceof StaticFeatureAccess access && access.getName() != null
            && access.getName().equalsIgnoreCase(name);
    }

    private static boolean isNumericExpression(Expression expression)
    {
        if (expression instanceof NumberLiteral)
        {
            return true;
        }
        return expression instanceof UnaryExpression unary && unary.getOperation() != UnaryOperation.NOT
            && unary.getOperand() instanceof NumberLiteral;
    }
}
