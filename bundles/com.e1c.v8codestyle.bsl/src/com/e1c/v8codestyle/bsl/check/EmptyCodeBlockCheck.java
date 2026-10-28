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
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.TRY_EXCEPT_STATEMENT__TRY_STATEMENTS;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Conditional;
import com._1c.g5.v8.dt.bsl.model.ForStatement;
import com._1c.g5.v8.dt.bsl.model.IfStatement;
import com._1c.g5.v8.dt.bsl.model.LoopStatement;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.TryExceptStatement;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: пустые блоки кода в конструкциях «Если», «ИначеЕсли», «Иначе»
 * и в телах циклов и «Попытки». Пустой блок — признак забытой реализации или удалённого
 * содержимого.
 * <p>
 * Не проверяются: тела методов (для них есть отдельная проверка), блоки
 * «Исключение» (покрыты проверкой пустого исключения), файловый блок модуля.
 * Параметр «Считать комментарии кодом» (по умолчанию выключен): блок,
 * содержащий комментарии в своей секции, пустым не считается.
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
        checkTryBlocks(module, commentAsCode, resultAceptor, progressMonitor);

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
                addHeaderIssue(ifStatement, "Иначе", "Else", //$NON-NLS-1$ //$NON-NLS-2$
                    Messages.EmptyCodeBlockCheck_Else_section, resultAceptor);
            }
        }

        for (LoopStatement loop : EcoreUtil2.getAllContentsOfType(module, LoopStatement.class))
        {
            if (loop.getStatements().isEmpty() && !hasComment(loop, LOOP_STATEMENT__STATEMENTS, commentAsCode))
            {
                addHeaderIssue(loop, "Цикл", "Do", Messages.EmptyCodeBlockCheck_Loop_body, resultAceptor); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    private void checkTryBlocks(Module module, boolean commentAsCode, ResultAcceptor resultAcceptor,
        IProgressMonitor monitor)
    {
        for (TryExceptStatement statement : EcoreUtil2.getAllContentsOfType(module, TryExceptStatement.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (!statement.getTryStatements().isEmpty()
                || hasComment(statement, TRY_EXCEPT_STATEMENT__TRY_STATEMENTS, commentAsCode))
            {
                continue;
            }
            addHeaderIssue(statement, "Попытка", "Try", Messages.EmptyCodeBlockCheck_Try_block, resultAcceptor); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private boolean hasElseSection(IfStatement ifStatement)
    {
        for (ILeafNode leaf : NodeModelUtils.findActualNodeFor(ifStatement).getLeafNodes())
        {
            String text = leaf.getText();
            if (leaf.getSemanticElement() == ifStatement && leaf.getGrammarElement() instanceof Keyword
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
            addHeaderIssue(conditional, "Тогда", "Then", sectionName, resultAceptor); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /** Match LS's direct parent terminals on the final line of an empty block header. */
    private void addHeaderIssue(EObject owner, String ruEnd, String enEnd, String sectionName,
        ResultAcceptor acceptor)
    {
        EObject scanOwner = owner instanceof Conditional ? owner.eContainer() : owner;
        // ForTo/ForEach are created by an unassigned grammar rule after the For keyword.
        INode node = owner instanceof ForStatement ? NodeModelUtils.findActualNodeFor(scanOwner)
            : NodeModelUtils.getNode(scanOwner);
        if (node == null)
        {
            return;
        }
        List<ILeafNode> terminals = new ArrayList<>();
        if (owner instanceof Conditional)
        {
            // If/ElsIf belongs to IfStatement, not to its Conditional child.
            ILeafNode opener = null;
            INode conditionalNode = NodeModelUtils.getNode(owner);
            if (conditionalNode == null)
            {
                return;
            }
            for (ILeafNode leaf : node.getLeafNodes())
            {
                if (leaf.getOffset() >= conditionalNode.getOffset())
                {
                    break;
                }
                if (!leaf.isHidden() && leaf.getSemanticElement() == scanOwner
                    && leaf.getGrammarElement() instanceof Keyword keyword
                    && (matches(keyword.getValue(), "Если", "If") //$NON-NLS-1$ //$NON-NLS-2$
                        || matches(keyword.getValue(), "ИначеЕсли", "ElsIf"))) //$NON-NLS-1$ //$NON-NLS-2$
                {
                    opener = leaf;
                }
            }
            if (opener != null)
            {
                terminals.add(opener);
            }
        }
        for (ILeafNode leaf : node.getLeafNodes())
        {
            if (owner instanceof ForStatement && !leaf.isHidden()
                && leaf.getGrammarElement() instanceof Keyword opener
                && matches(opener.getValue(), "Для", "For")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                terminals.add(leaf);
                continue;
            }
            if (!leaf.isHidden() && leaf.getSemanticElement() == owner
                && leaf.getGrammarElement() instanceof Keyword keyword)
            {
                terminals.add(leaf);
                if (matches(keyword.getValue(), ruEnd, enEnd))
                {
                    int line = leaf.getStartLine();
                    ILeafNode first = terminals.stream().filter(item -> item.getStartLine() == line)
                        .findFirst().orElse(leaf);
                    ILeafNode last = leaf;
                    if (owner instanceof LoopStatement)
                    {
                        for (ILeafNode closing : node.getLeafNodes())
                        {
                            if (closing.getOffset() > leaf.getOffset() && closing.getStartLine() == line
                                && !closing.isHidden() && closing.getSemanticElement() == owner
                                && closing.getGrammarElement() instanceof Keyword end
                                && matches(end.getValue(), "КонецЦикла", "EndDo")) //$NON-NLS-1$ //$NON-NLS-2$
                            {
                                last = closing;
                                break;
                            }
                        }
                    }
                    acceptor.addIssue(new BslDirectLocationIssue(emptyBlockMessage(sectionName),
                        new DirectLocation(first.getOffset(), last.getEndOffset() - first.getOffset(), line, owner)));
                    return;
                }
            }
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
        EObject scanOwner = owner instanceof Conditional && owner.eContainer() instanceof IfStatement
            ? owner.eContainer() : owner;
        INode node = NodeModelUtils.findActualNodeFor(scanOwner);
        if (node == null)
        {
            return false;
        }
        boolean inside = false;
        for (ILeafNode leaf : node.getLeafNodes())
        {
            if (inside && scanOwner != owner && leaf.getSemanticElement() == scanOwner
                && leaf.getGrammarElement() instanceof Keyword keyword
                && (matches(keyword.getValue(), "ИначеЕсли", "ElsIf") //$NON-NLS-1$ //$NON-NLS-2$
                    || matches(keyword.getValue(), "Иначе", "Else") //$NON-NLS-1$ //$NON-NLS-2$
                    || matches(keyword.getValue(), "КонецЕсли", "EndIf"))) //$NON-NLS-1$ //$NON-NLS-2$
            {
                return false;
            }
            if (leaf.getGrammarElement() instanceof Keyword keyword && leaf.getSemanticElement() == owner)
            {
                String value = keyword.getValue();
                boolean start = feature == CONDITIONAL__STATEMENTS && matches(value, "Тогда", "Then") //$NON-NLS-1$ //$NON-NLS-2$
                    || feature == IF_STATEMENT__ELSE_STATEMENTS && matches(value, "Иначе", "Else") //$NON-NLS-1$ //$NON-NLS-2$
                    || feature == LOOP_STATEMENT__STATEMENTS && matches(value, "Цикл", "Do") //$NON-NLS-1$ //$NON-NLS-2$
                    || feature == TRY_EXCEPT_STATEMENT__TRY_STATEMENTS && matches(value, "Попытка", "Try"); //$NON-NLS-1$ //$NON-NLS-2$
                if (start)
                {
                    inside = true;
                    continue;
                }
                if (inside && (matches(value, "КонецЕсли", "EndIf") //$NON-NLS-1$ //$NON-NLS-2$
                    || matches(value, "КонецЦикла", "EndDo") || matches(value, "Исключение", "Except"))) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                {
                    return false;
                }
            }
            if (inside && leaf.isHidden() && leaf.getText().stripLeading().startsWith("//")) //$NON-NLS-1$
            {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(String value, String ru, String en)
    {
        return ru.equalsIgnoreCase(value) || en.equalsIgnoreCase(value);
    }
}
