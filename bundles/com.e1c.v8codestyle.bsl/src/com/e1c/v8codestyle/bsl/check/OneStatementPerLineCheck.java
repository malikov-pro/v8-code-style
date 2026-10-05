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
 *     malikov-pro - port of the BSL Language Server diagnostic OneStatementPerLine
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.EmptyStatement;
import com._1c.g5.v8.dt.bsl.model.IfPreprocessorStatement;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: одно выражение в одной строке. Несколько операторов в одной
 * строке затрудняют чтение и отладку (точка останова на конкретный оператор).
 * <p>
 * Перенос диагностики BSL Language Server OneStatementPerLine
 * (тип CODE_SMELL, серьёзность MINOR). Как и в LS, препроцессорные
 * инструкции и пустые операторы «;» не учитываются; замечание ставится
 * на каждый оператор строки. Quick fix: см.
 * {@link com.e1c.v8codestyle.bsl.qfix.OneStatementPerLineFix} — переносит
 * оператор на новую строку (для первого оператора строки фикс недоступен).
 *
 * @author malikov-pro
 */
public class OneStatementPerLineCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "one-statement-per-line"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public OneStatementPerLineCheck()
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
        builder.title(Messages.OneStatementPerLineCheck_title)
            .description(Messages.OneStatementPerLineCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(METHOD);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Method method = (Method)object;

        Map<Integer, List<Statement>> statementsByLine = new HashMap<>();
        for (Statement statement : EcoreUtil2.getAllContentsOfType(method, Statement.class))
        {
            // пустые «;» и препроцессорные инструкции не учитываются, как в LS
            if (statement instanceof EmptyStatement || statement instanceof IfPreprocessorStatement)
            {
                continue;
            }
            INode node = NodeModelUtils.findActualNodeFor(statement);
            if (node == null)
            {
                continue;
            }
            int line = node.getStartLine();
            statementsByLine.computeIfAbsent(line, k -> new ArrayList<>())
                .add(statement);
        }

        for (List<Statement> perLine : statementsByLine.values())
        {
            if (perLine.size() < 2)
            {
                continue;
            }
            for (Statement statement : perLine)
            {
                resultAceptor.addIssue(Messages.OneStatementPerLineCheck_Move_to_new_line, statement);
            }
        }
    }
}
