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
 *     malikov-pro - port of the APK check АПК_01192
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Module;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.Issue;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: функция СУММА() с числовым операндом в запросах в тексте модулей.
 * <p>
 * Перенос проверки АПК_01192 (статья 787 стандарта 1С). При вычислении
 * количества записей на языке запросов следует всегда использовать функцию
 * КОЛИЧЕСТВО, а не СУММА: при количестве записей 10 млн и более произойдёт
 * переполнение из-за разрядности числа по умолчанию. Как и в алгоритме АПК:
 * текстовым поиском находятся вызовы СУММА()/SUM() в любом регистре
 * (пробелы, табы и символы «|» переноса строк игнорируются); СУММА(0) и
 * нечисловой операнд (поле, выражение) не фиксируются. Каждое вхождение
 * помечается отдельным замечанием. Комментарии модуля не проверяются.
 *
 * @author malikov-pro
 */
public class ApkSumInQueryCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-01192-summa-in-query"; //$NON-NLS-1$

    // Вызов СУММА(/SUM( в любом регистре; перед скобкой и в операнде допустимы
    // пробелы, табы и «|» переноса строк (АПК вырезает их из текста модуля).
    // Первый «)» закрывает операнд — вложенные вызовы не фиксируются, как в АПК.
    // Взгляд назад исключает части имён (например, «ПолучитьСУММА(»).
    private static final Pattern SUM_CALL_PATTERN = Pattern.compile(
        "(?<![\\p{L}\\p{Nd}_])(?:СУММА|SUM)[\\s|]*\\(([^)]*)\\)", //$NON-NLS-1$
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // Операнд — число (в АПК — «ЭтоЧисло(Операнд)»)
    private static final Pattern NUMBER_OPERAND_PATTERN = Pattern.compile("[+-]?\\d+(?:\\.\\d+)?"); //$NON-NLS-1$

    private static final String ZERO_OPERAND = "0"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public ApkSumInQueryCheck()
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
        builder.title(Messages.ApkSumInQueryCheck_title)
            .description(Messages.ApkSumInQueryCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Module module = (Module)object;
        INode node = NodeModelUtils.findActualNodeFor(module);
        if (node == null)
        {
            return;
        }
        String moduleName = EcoreUtil.getURI(module).lastSegment();
        for (ILeafNode leafNode : node.getLeafNodes())
        {
            // скрытые листья — пробелы и комментарии; тексты запросов —
            // строковые литералы (независимо от способа задания текста запроса)
            if (leafNode.isHidden())
            {
                continue;
            }
            String text = leafNode.getText();
            if (text.isEmpty())
            {
                continue;
            }
            Matcher matcher = SUM_CALL_PATTERN.matcher(text);
            while (matcher.find())
            {
                // операнд без пробелов, табов и «|» переноса строк (как в АПК)
                String operand = matcher.group(1).replaceAll("[\\s|]", ""); //$NON-NLS-1$ //$NON-NLS-2$
                if (operand.equals(ZERO_OPERAND) || !NUMBER_OPERAND_PATTERN.matcher(operand).matches())
                {
                    continue;
                }
                int line = leafNode.getStartLine() + newLinesBefore(text, matcher.start());
                String message = MessageFormat.format(
                    Messages.ApkSumInQueryCheck_Sum_with_constant_operand_in_query, matcher.group().trim(),
                    moduleName, line);
                DirectLocation location =
                    new DirectLocation(leafNode.getOffset() + matcher.start(), matcher.end() - matcher.start(),
                        line, module);
                resultAceptor.addIssue(new BslDirectLocationIssue(message, location));
            }
        }
    }

    private static int newLinesBefore(String text, int position)
    {
        int count = 0;
        for (int i = 0; i < position; i++)
        {
            if (text.charAt(i) == '\n')
            {
                count++;
            }
        }
        return count;
    }
}
