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
 *     malikov-pro - port of the BSL Language Server diagnostic EmptyStatement
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.EmptyStatement;
import com._1c.g5.v8.dt.bsl.model.Method;
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
 * Проверка: пустой оператор «;» в теле модуля. Лишняя точка с запятой —
 * мусор, затрудняющий чтение кода.
 * <p>
 * Перенос диагностики BSL Language Server EmptyStatement
 * (тип CODE_SMELL, серьёзность INFO).
 * <p>
 * Объекты EmptyStatement не доставляются фреймворком через
 * checkedObjectType (не попадают в BM-модель) — метод-якорь с собственным
 * обходом AST, как в upstream-проверке SemicolonMissing. Правило грамматики
 * EmptyStatement не потребляет текст («;» — соседний лист), поэтому
 * замечание ставится DirectLocation на лист «;».
 * Как и в LS, пустой оператор рядом с ошибкой разбора не фиксируется:
 * восстановление парсера может порождать фантомные «;».
 *
 * @author malikov-pro
 */
public class EmptyStatementCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "empty-statement"; //$NON-NLS-1$

    private static final String SEMICOLON = ";"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public EmptyStatementCheck()
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
        builder.title(Messages.EmptyStatementCheck_title)
            .description(Messages.EmptyStatementCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.TRIVIAL)
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

        for (EmptyStatement statement : EcoreUtil2.getAllContentsOfType(method, EmptyStatement.class))
        {
            ILeafNode semicolon = findSemicolon(NodeModelUtils.findActualNodeFor(statement));
            if (semicolon == null)
            {
                continue;
            }
            // «;» рядом с ошибкой разбора — вероятно, фантом восстановления парсера
            INode previous = semicolon.getPreviousSibling();
            if (containsSyntaxError(semicolon) || (previous != null && containsSyntaxError(previous)))
            {
                continue;
            }
            DirectLocation location =
                new DirectLocation(semicolon.getOffset(), semicolon.getLength(), semicolon.getStartLine(), method);

            Issue issue = new BslDirectLocationIssue(Messages.EmptyStatementCheck_Remove_empty_statement, location);
            resultAceptor.addIssue(issue);
        }
    }

    /**
     * Находит лист «;» для узла EmptyStatement: само правило грамматики не
     * потребляет текст, точка с запятой — соседний (или дочерний) лист узла.
     *
     * @param node узел EmptyStatement, может быть {@code null}.
     * @return лист с текстом «;» или {@code null}.
     */
    private static ILeafNode findSemicolon(INode node)
    {
        if (node == null)
        {
            return null;
        }
        if (node instanceof ILeafNode leaf && SEMICOLON.equals(leaf.getText()))
        {
            return leaf;
        }
        INode sibling = node;
        for (int i = 0; i < 3 && sibling != null; i++)
        {
            sibling = sibling.getNextSibling();
            if (sibling instanceof ILeafNode leaf && SEMICOLON.equals(leaf.getText()))
            {
                return leaf;
            }
        }
        for (INode child : node.getAsTreeIterable())
        {
            if (child instanceof ILeafNode leaf && SEMICOLON.equals(leaf.getText()))
            {
                return leaf;
            }
        }
        return null;
    }

    private static boolean containsSyntaxError(INode node)
    {
        for (INode child : node.getAsTreeIterable())
        {
            if (child.getSyntaxErrorMessage() != null)
            {
                return true;
            }
        }
        return false;
    }
}
