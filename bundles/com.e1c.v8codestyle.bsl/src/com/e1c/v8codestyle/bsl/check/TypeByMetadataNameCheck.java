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
 *     malikov-pro - port of the APK check АПК_00305
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.BINARY_EXPRESSION;

import org.eclipse.core.runtime.IProgressMonitor;

import com._1c.g5.v8.dt.bsl.model.BinaryExpression;
import com._1c.g5.v8.dt.bsl.model.BinaryOperation;
import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.StringLiteral;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: тип значения переменной следует определять сравнением типа значения
 * с типом ({@code ТипЗнч(Значение) = Тип("...")}), а не по имени метаданных.
 * Порицается сравнение цепочки {@code <Выражение>.Метаданные().Имя} (а также
 * {@code <Выражение>.Метаданные().ПолноеИмя()}) со строковым литералом:
 * <pre>
 * Неправильно: Если Ссылка.Метаданные().Имя = "ПоступлениеТоваровУслуг" Тогда
 * Правильно:   Если ТипЗнч(Ссылка) = Тип("ДокументСсылка.ПоступлениеТоваровУслуг") Тогда
 * </pre>
 * <p>
 * Перенос проверки АПК_00305 (статья 442 стандарта 1С). Охват: цепочки с
 * методом {@code Метаданные}/{@code Metadata} и завершением {@code Имя}/
 * {@code Name} (сравнение с литералом слева или справа, операторы {@code =} и
 * {@code <>}), включая вариант завершения {@code ПолноеИмя()}/{@code FullName()}.
 * Источник цепочки не анализируется ({@code Ссылка.Метаданные()},
 * {@code Объект.Метаданные()}, глобальный {@code Метаданные()}).
 *
 * @author malikov-pro
 */
public class TypeByMetadataNameCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00305-type-by-metadata-name"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public TypeByMetadataNameCheck()
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
        builder.title(Messages.TypeByMetadataNameCheck_title)
            .description(Messages.TypeByMetadataNameCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(BINARY_EXPRESSION);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        BinaryExpression expression = (BinaryExpression)object;
        BinaryOperation operation = expression.getOperation();
        if (operation != BinaryOperation.EQ && operation != BinaryOperation.NE)
        {
            return;
        }
        Expression left = expression.getLeft();
        Expression right = expression.getRight();
        if (left == null || right == null)
        {
            return;
        }
        if (isMetadataNameAccess(left) && isStringLiteral(right) || isMetadataNameAccess(right)
            && isStringLiteral(left))
        {
            resultAceptor.addIssue(Messages.TypeByMetadataNameCheck_Type_determined_by_metadata_name, expression);
        }
    }

    private static boolean isStringLiteral(Expression expression)
    {
        return expression instanceof StringLiteral;
    }

    /**
     * Проверяет, что выражение — обращение к имени метаданных:
     * {@code <Выражение>.Метаданные().Имя} (обращение к свойству) либо
     * {@code <Выражение>.Метаданные().ПолноеИмя()} (вызов метода).
     *
     * @param expression проверяемое выражение, не может быть {@code null}.
     * @return {@code true}, если выражение определяет тип по имени метаданных.
     */
    static boolean isMetadataNameAccess(Expression expression)
    {
        if (expression instanceof DynamicFeatureAccess featureAccess)
        {
            // <Выражение>.Метаданные().Имя
            return isMetadataNameEntry(featureAccess) && isMetadataInvocation(featureAccess.getSource());
        }
        if (expression instanceof Invocation invocation)
        {
            // <Выражение>.Метаданные().ПолноеИмя()
            return isFullNameInvocation(invocation);
        }
        return false;
    }

    private static boolean isFullNameInvocation(Invocation invocation)
    {
        if (!invocation.getParams().isEmpty()
            || !(invocation.getMethodAccess() instanceof DynamicFeatureAccess methodAccess))
        {
            return false;
        }
        return isName(methodAccess.getName(), "ПолноеИмя", "FullName") //$NON-NLS-1$ //$NON-NLS-2$
            && isMetadataInvocation(methodAccess.getSource());
    }

    private static boolean isMetadataInvocation(Expression expression)
    {
        if (!(expression instanceof Invocation invocation) || !invocation.getParams().isEmpty())
        {
            return false;
        }
        return invocation.getMethodAccess() != null
            && isName(invocation.getMethodAccess().getName(), "Метаданные", "Metadata"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static boolean isMetadataNameEntry(DynamicFeatureAccess featureAccess)
    {
        return isName(featureAccess.getName(), "Имя", "Name"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Сравнивает последний сегмент имени (цепочки вида {@code А.Б}) с эталоном
     * без учёта регистра.
     *
     * @param name имя как в исходнике, может быть {@code null}.
     * @param ruName русское имя метода, не может быть {@code null}.
     * @param enName английское имя метода, не может быть {@code null}.
     * @return {@code true}, если последний сегмент совпадает с эталоном.
     */
    private static boolean isName(String name, String ruName, String enName)
    {
        if (name == null)
        {
            return false;
        }
        int dot = name.lastIndexOf('.');
        String last = dot < 0 ? name : name.substring(dot + 1);
        return last.equalsIgnoreCase(ruName) || last.equalsIgnoreCase(enName);
    }
}
