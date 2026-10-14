/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.BinaryExpression;
import com._1c.g5.v8.dt.bsl.model.BinaryOperation;
import com._1c.g5.v8.dt.bsl.model.Conditional;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.GotoStatement;
import com._1c.g5.v8.dt.bsl.model.IfStatement;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.LoopStatement;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.TryExceptStatement;
import com._1c.g5.v8.dt.bsl.model.UnaryExpression;
import com._1c.g5.v8.dt.bsl.model.UnaryOperation;

/** Computes nesting and logical series on AST, without control-flow analysis. */
final class LsComplexityComputer
{
    private final boolean cognitive;
    private final String methodName;

    private LsComplexityComputer(boolean cognitive, String methodName)
    {
        this.cognitive = cognitive;
        this.methodName = methodName;
    }

    static int method(Method method, boolean cognitive)
    {
        return (cognitive ? 0 : 1)
            + new LsComplexityComputer(cognitive, method.getName()).statements(method.allStatements(), 0);
    }

    static int body(Module module, boolean cognitive)
    {
        return new LsComplexityComputer(cognitive, null).statements(module.allStatements(), 0);
    }

    private int structural(int depth)
    {
        return cognitive ? 1 + depth : 1;
    }

    private int statements(List<? extends EObject> statements, int depth)
    {
        int result = 0;
        for (EObject statement : statements)
        {
            result += visit(statement, depth);
        }
        return result;
    }

    private int visit(EObject object, int depth)
    {
        if (object == null)
        {
            return 0;
        }
        if (object instanceof IfStatement statement)
        {
            int result = structural(depth) + branch(statement.getIfPart(), depth + 1);
            for (Conditional branch : statement.getElsIfParts())
            {
                result += 1 + branch(branch, depth + 1);
            }
            if (hasElse(statement))
            {
                result += 1 + statements(statement.getElseStatements(), depth + 1);
            }
            return result;
        }
        if (object instanceof LoopStatement loop)
        {
            int result = structural(depth);
            for (EObject child : loop.eContents())
            {
                result += visit(child, depth + 1);
            }
            return result;
        }
        if (object instanceof TryExceptStatement statement)
        {
            return statements(statement.getTryStatements(), depth) + structural(depth)
                + statements(statement.getExceptStatements(), depth + 1);
        }
        if (object instanceof GotoStatement)
        {
            return 1;
        }
        if (object instanceof Expression expression)
        {
            return logicalCost(expression) + expressionStructures(expression, depth);
        }
        int result = 0;
        for (EObject child : object.eContents())
        {
            result += visit(child, depth);
        }
        return result;
    }

    private int branch(Conditional conditional, int depth)
    {
        return conditional == null ? 0
            : visit(conditional.getPredicate(), depth) + statements(conditional.getStatements(), depth);
    }

    private int expressionStructures(Expression expression, int depth)
    {
        if (expression instanceof Invocation invocation)
        {
            boolean ternary = UselessTernaryOperatorCheck.isTernaryOperator(invocation);
            int result = ternary ? structural(depth) : 0;
            if (!ternary && methodName != null && invocation.getMethodAccess() instanceof StaticFeatureAccess access
                && methodName.equalsIgnoreCase(access.getName()))
            {
                result++;
            }
            for (Expression parameter : invocation.getParams())
            {
                result += visit(parameter, ternary ? depth + 1 : depth);
            }
            if (invocation.getMethodAccess() != null)
            {
                for (EObject receiver : invocation.getMethodAccess().eContents())
                {
                    if (receiver instanceof Expression receiverExpression)
                    {
                        result += visit(receiverExpression, depth);
                    }
                }
            }
            return result;
        }
        int result = 0;
        for (EObject child : expression.eContents())
        {
            if (child instanceof Expression nested)
            {
                result += expression instanceof BinaryExpression || expression instanceof UnaryExpression
                    ? expressionStructures(nested, depth) : visit(nested, depth);
            }
        }
        return result;
    }

    private int logicalCost(Expression expression)
    {
        List<BinaryOperation> operations = new ArrayList<>();
        flatten(expression, operations);
        int result = 0;
        BinaryOperation previous = null;
        for (BinaryOperation operation : operations)
        {
            if (operation != null && (!cognitive || operation != previous))
            {
                result++;
            }
            previous = operation;
        }
        return result;
    }

    private void flatten(Expression expression, List<BinaryOperation> operations)
    {
        if (expression instanceof BinaryExpression binary)
        {
            flatten(binary.getLeft(), operations);
            if (binary.getOperation() == BinaryOperation.AND || binary.getOperation() == BinaryOperation.OR)
            {
                operations.add(binary.getOperation());
            }
            flatten(binary.getRight(), operations);
        }
        else if (expression instanceof UnaryExpression unary)
        {
            List<BinaryOperation> nested = new ArrayList<>();
            flatten(unary.getOperand(), nested);
            boolean split = unary.getOperation() == UnaryOperation.NOT && !nested.isEmpty();
            if (split)
            {
                operations.add(null);
            }
            operations.addAll(nested);
            if (split)
            {
                operations.add(null);
            }
        }
    }

    private static boolean hasElse(IfStatement statement)
    {
        INode node = NodeModelUtils.getNode(statement);
        if (node != null)
        {
            for (ILeafNode leaf : node.getLeafNodes())
            {
                if (leaf.getSemanticElement() == statement && !leaf.isHidden()
                    && ("Иначе".equalsIgnoreCase(leaf.getText()) || "Else".equalsIgnoreCase(leaf.getText()))) //$NON-NLS-1$ //$NON-NLS-2$
                {
                    return true;
                }
            }
        }
        return false;
    }
}
