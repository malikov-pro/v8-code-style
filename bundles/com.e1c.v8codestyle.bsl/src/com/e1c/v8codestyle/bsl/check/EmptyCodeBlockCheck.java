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
 *     malikov-pro - port of the BSL Language Server diagnostic EmptyCodeBlock
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.CONDITIONAL__STATEMENTS;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.IF_STATEMENT__ELSE_STATEMENTS;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.LOOP_STATEMENT__STATEMENTS;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Conditional;
import com._1c.g5.v8.dt.bsl.model.IfStatement;
import com._1c.g5.v8.dt.bsl.model.LoopStatement;
import com._1c.g5.v8.dt.bsl.model.Module;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: пустые блоки кода в конструкциях «Если», «ИначеЕсли», «Иначе»
 * и в телах циклов. Пустой блок — признак забытой реализации или удалённого
 * содержимого.
 * <p>
 * Не проверяются: тела методов (для них есть отдельная проверка), блоки
 * «Исключение» (покрыты проверкой пустого исключения), файловый блок модуля.
 * Параметр «Считать комментарии кодом» (по умолчанию выключен): блок,
 * содержащий комментарии, пустым не считается.
 * <p>
 * Перенос диагностики BSL Language Server EmptyCodeBlock
 * (тип CODE_SMELL, серьёзность MAJOR).
 *
 * @author malikov-pro
 */
public class EmptyCodeBlockCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "empty-code-block"; //$NON-NLS-1$

    private static final String PARAM_COMMENT_AS_CODE = "commentAsCode"; //$NON-NLS-1$

    private static final String DEFAULT_COMMENT_AS_CODE = Boolean.FALSE.toString();

    /**
     * Instantiates a new check.
     */
    public EmptyCodeBlockCheck()
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
        builder.title(Messages.EmptyCodeBlockCheck_title)
            .description(Messages.EmptyCodeBlockCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE)
            .parameter(PARAM_COMMENT_AS_CODE, Boolean.class, DEFAULT_COMMENT_AS_CODE,
                Messages.EmptyCodeBlockCheck_Treat_comments_as_code);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Module module = (Module)object;
        boolean commentAsCode = parameters.getBoolean(PARAM_COMMENT_AS_CODE);

        for (IfStatement ifStatement : EcoreUtil2.getAllContentsOfType(module, IfStatement.class))
        {
            checkConditional(ifStatement.getIfPart(), Messages.EmptyCodeBlockCheck_If_condition, commentAsCode,
                resultAceptor);
            for (Conditional elsIf : ifStatement.getElsIfParts())
            {
                checkConditional(elsIf, Messages.EmptyCodeBlockCheck_ElsIf_condition, commentAsCode, resultAceptor);
            }
            // оператор может вовсе не содержать ветки «Иначе»: пустой список
            // elseStatements означает и отсутствие секции, и пустую секцию,
            // поэтому наличие «Иначе» проверяем по ключевому слову в узле
            if (hasElseSection(ifStatement) && ifStatement.getElseStatements().isEmpty()
                && !hasComment(ifStatement, IF_STATEMENT__ELSE_STATEMENTS, commentAsCode))
            {
                resultAceptor.addIssue(emptyBlockMessage(Messages.EmptyCodeBlockCheck_Else_section), ifStatement,
                    IF_STATEMENT__ELSE_STATEMENTS);
            }
        }

        for (LoopStatement loop : EcoreUtil2.getAllContentsOfType(module, LoopStatement.class))
        {
            if (loop.getStatements().isEmpty() && !hasComment(loop, LOOP_STATEMENT__STATEMENTS, commentAsCode))
            {
                resultAceptor.addIssue(emptyBlockMessage(Messages.EmptyCodeBlockCheck_Loop_body), loop,
                    LOOP_STATEMENT__STATEMENTS);
            }
        }
    }

    private boolean hasElseSection(IfStatement ifStatement)
    {
        for (ILeafNode leaf : NodeModelUtils.findActualNodeFor(ifStatement).getLeafNodes())
        {
            String text = leaf.getText();
            if (leaf.getGrammarElement() instanceof Keyword
                && (text.equalsIgnoreCase("Иначе") || text.equalsIgnoreCase("Else"))) //$NON-NLS-1$ //$NON-NLS-2$
            {
                return true;
            }
        }
        return false;
    }

    private void checkConditional(Conditional conditional, String sectionName, boolean commentAsCode,
        ResultAcceptor resultAceptor)
    {
        if (conditional != null && conditional.getStatements().isEmpty()
            && !hasComment(conditional, CONDITIONAL__STATEMENTS, commentAsCode))
        {
            resultAceptor.addIssue(emptyBlockMessage(sectionName), conditional, CONDITIONAL__STATEMENTS);
        }
    }

    private String emptyBlockMessage(String sectionName)
    {
        return MessageFormat.format(Messages.EmptyCodeBlockCheck_Empty_code_block, sectionName);
    }

    private boolean hasComment(EObject owner, EStructuralFeature feature, boolean commentAsCode)
    {
        if (!commentAsCode)
        {
            return false;
        }
        List<INode> nodes = NodeModelUtils.findNodesForFeature(owner, feature);
        String text = nodes.isEmpty()
            ? NodeModelUtils.findActualNodeFor(owner).getText()
            : nodes.stream().map(INode::getText).collect(Collectors.joining());
        return text.contains("//"); //$NON-NLS-1$
    }
}
