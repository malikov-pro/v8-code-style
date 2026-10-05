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
 *     malikov-pro - port of the BSL Language Server diagnostic UselessTernaryOperator
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.BooleanLiteral;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: бесполезный тернарный оператор {@code ?(Условие, Ветка1, Ветка2)}:
 * <ul>
 * <li>условие — булева константа (Истина/Ложь), результат известен заранее;</li>
 * <li>обе ветки — булевы константы: {@code ?(X, Истина, Ложь)} → {@code X},
 * {@code ?(X, Ложь, Истина)} → {@code НЕ X}, ветки одинаковы — результат
 * не зависит от условия.</li>
 * </ul>
 * <p>
 * Перенос диагностики BSL Language Server UselessTernaryOperator
 * (тип CODE_SMELL, серьёзность INFO). В EDT-модели тернарник — это Invocation
 * с methodAccess «?». Quick fix: см. {@link com.e1c.v8codestyle.bsl.qfix.UselessTernaryFix}.
 *
 * @author malikov-pro
 */
public class UselessTernaryOperatorCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "useless-ternary-operator"; //$NON-NLS-1$

    /** Имя тернарного оператора в EDT-модели (токен «?»). */
    private static final String QUESTION_MARK = "?"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public UselessTernaryOperatorCheck()
    {
        super();
    }

    /**
     * Проверяет, что вызов является тернарным оператором {@code ?(А, Б, В)}.
     *
     * @param invocation вызов, не может быть {@code null}.
     * @return {@code true}, если это тернарный оператор.
     */
    public static boolean isTernaryOperator(Invocation invocation)
    {
        return invocation.getMethodAccess() instanceof StaticFeatureAccess access
            && QUESTION_MARK.equals(access.getName());
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.UselessTernaryOperatorCheck_title)
            .description(Messages.UselessTernaryOperatorCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.TRIVIAL)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(INVOCATION);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Invocation invocation = (Invocation)object;
        if (!isTernaryOperator(invocation))
        {
            return;
        }
        if (invocation.getParams().size() != 3)
        {
            return;
        }
        Expression condition = invocation.getParams().get(0);
        Boolean trueBranch = booleanLiteral(invocation.getParams().get(1));
        Boolean falseBranch = booleanLiteral(invocation.getParams().get(2));

        if (booleanLiteral(condition) != null || (trueBranch != null && falseBranch != null))
        {
            resultAceptor.addIssue(Messages.UselessTernaryOperatorCheck_Useless_ternary_operator, invocation);
        }
    }

    /**
     * Возвращает {@code Boolean.TRUE}/{@code Boolean.FALSE}, если выражение —
     * булев литерал (Истина/Ложь), иначе {@code null}.
     *
     * @param expression выражение, не может быть {@code null}.
     * @return значение литерала или {@code null}.
     */
    protected static Boolean booleanLiteral(Expression expression)
    {
        if (expression instanceof BooleanLiteral literal)
        {
            return Boolean.valueOf(literal.isIsTrue());
        }
        return null;
    }

    /**
     * Возвращает текст выражения как он написан в модуле.
     *
     * @param expression выражение, не может быть {@code null}.
     * @return текст выражения.
     */
    protected static String expressionText(Expression expression)
    {
        return NodeModelUtils.findActualNodeFor(expression).getText().trim();
    }
}
