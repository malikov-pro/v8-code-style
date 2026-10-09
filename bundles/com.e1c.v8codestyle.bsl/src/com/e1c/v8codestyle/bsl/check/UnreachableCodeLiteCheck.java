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
 *     malikov-pro - unreachable code check (lite), upstream issue #1068
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com._1c.g5.v8.dt.bsl.model.Conditional;
import com._1c.g5.v8.dt.bsl.model.EmptyStatement;
import com._1c.g5.v8.dt.bsl.model.IfStatement;
import com._1c.g5.v8.dt.bsl.model.LabeledStatement;
import com._1c.g5.v8.dt.bsl.model.LoopStatement;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.RaiseStatement;
import com._1c.g5.v8.dt.bsl.model.ReturnStatement;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com._1c.g5.v8.dt.bsl.model.TryExceptStatement;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка недостижимого кода (лайт-версия без графа потока управления).
 * <p>
 * Операторы, следующие в том же блоке за оператором «Возврат» (ReturnStatement)
 * или «ВызватьИсключение» (RaiseStatement), никогда не будут выполнены: такой
 * код обычно остаётся после редактирования «чужого кода» и говорит об ошибке
 * в алгоритме. Блок — это тело метода, ветвь Если/ИначеЕсли/Иначе, тело цикла
 * или секция Попытки/Исключения. Одно замечание на блок — на первый
 * недостижимый оператор. Если все ветви «Если» (Если + все ИначеЕсли + Иначе)
 * завершаются «Возвратом»/«ВызватьИсключением» (в том числе вложенным
 * полностью терминальным «Если»), операторы после «КонецЕсли» в том же блоке
 * тоже недостижимы.
 * <p>
 * Упрощение (лайт-версия без построения графа потока управления, в отличие
 * от диагностики unreachable-code BSL Language Server): операторы после
 * «Прервать», «Продолжить» и «Перейти» не выявляются; мёртвые условия и код
 * уровня модуля вне методов не проверяются. По материалам апстрим-issue
 * 1C-Company/v8-code-style#1068.
 *
 * @author malikov-pro
 */
public class UnreachableCodeLiteCheck
    extends BasicCheck
{

    /** Идентификатор проверки (источник — апстрим issue #1068). */
    public static final String CHECK_ID = "up-1068-unreachable-code"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public UnreachableCodeLiteCheck()
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
        builder.title(Messages.UnreachableCodeLiteCheck_title)
            .description(Messages.UnreachableCodeLiteCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(METHOD);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Method method = (Method)object;
        checkBlock(method.getStatements(), resultAceptor);
    }

    /**
     * Checks one block of statements: reports the first statement unreachable
     * within the block (the one following the first terminating statement)
     * and stops - one issue per block; statements of the already unreachable
     * tail are not descended into.
     */
    private void checkBlock(List<Statement> statements, ResultAcceptor resultAceptor)
    {
        for (int i = 0; i < statements.size(); i++)
        {
            Statement statement = effectiveStatement(statements.get(i));
            if (statement instanceof EmptyStatement)
            {
                continue;
            }
            visitSubBlocks(statement, resultAceptor);
            if (isTerminating(statement))
            {
                reportFirstUnreachable(statements, i, resultAceptor);
                return;
            }
        }
    }

    private void visitSubBlocks(Statement statement, ResultAcceptor resultAceptor)
    {
        if (statement instanceof IfStatement ifStatement)
        {
            Conditional ifPart = ifStatement.getIfPart();
            if (ifPart != null)
            {
                checkBlock(ifPart.getStatements(), resultAceptor);
            }
            for (Conditional elsIfPart : ifStatement.getElsIfParts())
            {
                checkBlock(elsIfPart.getStatements(), resultAceptor);
            }
            checkBlock(ifStatement.getElseStatements(), resultAceptor);
        }
        else if (statement instanceof LoopStatement loopStatement)
        {
            checkBlock(loopStatement.getStatements(), resultAceptor);
        }
        else if (statement instanceof TryExceptStatement tryExceptStatement)
        {
            checkBlock(tryExceptStatement.getTryStatements(), resultAceptor);
            checkBlock(tryExceptStatement.getExceptStatements(), resultAceptor);
        }
    }

    private void reportFirstUnreachable(List<Statement> statements, int terminalIndex, ResultAcceptor resultAceptor)
    {
        for (int j = terminalIndex + 1; j < statements.size(); j++)
        {
            if (!(statements.get(j) instanceof EmptyStatement))
            {
                resultAceptor.addIssue(Messages.UnreachableCodeLiteCheck_Unreachable_code, statements.get(j));
                return;
            }
        }
    }

    private boolean isTerminating(Statement statement)
    {
        if (statement instanceof ReturnStatement || statement instanceof RaiseStatement)
        {
            return true;
        }
        return statement instanceof IfStatement ifStatement && isFullyTerminatingIf(ifStatement);
    }

    /**
     * Returns whether every branch of the If (If + all ElseIf + Else)
     * terminates with Return/Raise. An If without Else (or with an empty
     * Else) never terminates: control flow continues after the EndIf when
     * no condition holds.
     */
    private boolean isFullyTerminatingIf(IfStatement ifStatement)
    {
        Conditional ifPart = ifStatement.getIfPart();
        if (ifPart == null || !isTerminatingBlock(ifPart.getStatements()))
        {
            return false;
        }
        for (Conditional elsIfPart : ifStatement.getElsIfParts())
        {
            if (!isTerminatingBlock(elsIfPart.getStatements()))
            {
                return false;
            }
        }
        // An empty list means both "no ELSE" and "empty ELSE", so checking it as a
        // terminating block covers both cases the same way
        return isTerminatingBlock(ifStatement.getElseStatements());
    }

    private boolean isTerminatingBlock(List<Statement> statements)
    {
        Statement last = null;
        for (Statement statement : statements)
        {
            Statement effective = effectiveStatement(statement);
            if (effective instanceof EmptyStatement)
            {
                continue;
            }
            last = effective;
        }
        return last != null && isTerminating(last);
    }

    private Statement effectiveStatement(Statement statement)
    {
        Statement result = statement;
        while (result instanceof LabeledStatement labeledStatement)
        {
            result = labeledStatement.getStatement();
        }
        return result;
    }
}
