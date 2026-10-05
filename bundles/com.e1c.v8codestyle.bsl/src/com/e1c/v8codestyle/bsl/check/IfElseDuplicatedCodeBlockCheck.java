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
 *     malikov-pro - port of the BSL Language Server diagnostic IfElseDuplicatedCodeBlock
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.IF_STATEMENT;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Conditional;
import com._1c.g5.v8.dt.bsl.model.IfStatement;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: повторяющиеся блоки кода в синтаксической конструкции
 * {@code Если…Тогда…ИначеЕсли…}. Дублирующиеся ветви — вероятная ошибка
 * копипасты либо признак того, что условие лишнее.
 * <p>
 * Перенос диагностики BSL Language Server IfElseDuplicatedCodeBlock
 * (тип CODE_SMELL, серьёзность MINOR). Блоки сравниваются по тексту
 * (без учёта регистра и пробелов); два пустых блока повторением не считаются.
 * В отличие от LS (related information) замечание ставится на каждый
 * повторяющийся (более поздний) блок.
 *
 * @author malikov-pro
 */
public class IfElseDuplicatedCodeBlockCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "if-else-duplicated-code-block"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public IfElseDuplicatedCodeBlockCheck()
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
        builder.title(Messages.IfElseDuplicatedCodeBlockCheck_title)
            .description(Messages.IfElseDuplicatedCodeBlockCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(IF_STATEMENT);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        IfStatement ifStatement = (IfStatement)object;

        List<List<Statement>> blocks = new ArrayList<>();
        if (ifStatement.getIfPart() != null)
        {
            blocks.add(ifStatement.getIfPart().getStatements());
        }
        for (Conditional conditional : ifStatement.getElsIfParts())
        {
            blocks.add(conditional.getStatements());
        }
        // пустой список означает и «нет Иначе», и «пустое Иначе» — оба пусты и не сравниваются
        blocks.add(ifStatement.getElseStatements());

        List<String> signatures = new ArrayList<>(blocks.size());
        for (List<Statement> block : blocks)
        {
            signatures.add(signature(block));
        }

        for (int i = 1; i < blocks.size(); i++)
        {
            if (isEmptyBlock(blocks.get(i)))
            {
                continue;
            }
            for (int j = 0; j < i; j++)
            {
                if (isEmptyBlock(blocks.get(j)))
                {
                    continue;
                }
                if (signatures.get(j).equals(signatures.get(i)))
                {
                    String message = MessageFormat.format(
                        Messages.IfElseDuplicatedCodeBlockCheck_Duplicated_code_block, i + 1, j + 1);
                    resultAceptor.addIssue(message, blocks.get(i).get(0));
                    break;
                }
            }
        }
    }

    private static boolean isEmptyBlock(List<Statement> block)
    {
        return block == null || block.isEmpty();
    }

    private static String signature(List<Statement> block)
    {
        StringBuilder builder = new StringBuilder();
        for (Statement statement : block)
        {
            String text = NodeModelUtils.findActualNodeFor(statement).getText();
            builder.append(normalize(text)).append('\n');
        }
        return builder.toString();
    }

    private static String normalize(String text)
    {
        return text.trim().replaceAll("\\s+", " ") //$NON-NLS-1$ //$NON-NLS-2$
            .toLowerCase();
    }
}
