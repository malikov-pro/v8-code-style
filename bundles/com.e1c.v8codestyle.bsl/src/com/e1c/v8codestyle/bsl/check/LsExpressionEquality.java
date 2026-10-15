/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.BinaryExpression;
import com._1c.g5.v8.dt.bsl.model.BinaryOperation;
import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.EmptyExpression;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.IndexAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Literal;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.StringLiteral;
import com._1c.g5.v8.dt.bsl.model.UnaryExpression;

/** Structural operand comparison for IdenticalExpressions, not general algebra. */
final class LsExpressionEquality
{
    private LsExpressionEquality()
    {
    }

    static boolean equal(Expression left, Expression right)
    {
        return equal(left, right, 0);
    }

    private static boolean equal(Expression left, Expression right, int depth)
    {
        if (left == null || right == null || depth > 128 || left.eClass() != right.eClass()
            || left instanceof DynamicFeatureAccess || left instanceof IndexAccess)
        {
            return false;
        }
        if (left instanceof BinaryExpression first && right instanceof BinaryExpression second)
        {
            if (first.getOperation() != second.getOperation())
            {
                return false;
            }
            boolean ordered = equal(first.getLeft(), second.getLeft(), depth + 1)
                && equal(first.getRight(), second.getRight(), depth + 1);
            return ordered || swappable(first.getOperation())
                && equal(first.getLeft(), second.getRight(), depth + 1)
                && equal(first.getRight(), second.getLeft(), depth + 1);
        }
        if (left instanceof UnaryExpression first && right instanceof UnaryExpression second)
        {
            return first.getOperation() == second.getOperation()
                && equal(first.getOperand(), second.getOperand(), depth + 1);
        }
        if (left instanceof Invocation first && right instanceof Invocation second)
        {
            if (!(first.getMethodAccess() instanceof StaticFeatureAccess firstName)
                || !(second.getMethodAccess() instanceof StaticFeatureAccess secondName)
                || !firstName.getName().equalsIgnoreCase(secondName.getName())
                || first.getParams().size() != second.getParams().size())
            {
                return false;
            }
            for (int i = 0; i < first.getParams().size(); i++)
            {
                if (!equal(first.getParams().get(i), second.getParams().get(i), depth + 1))
                {
                    return false;
                }
            }
            return true;
        }
        if (left instanceof EmptyExpression)
        {
            return true;
        }
        if (!(left instanceof Literal || left instanceof StaticFeatureAccess))
        {
            // Constructors and unsupported expression kinds require their own contracts.
            return false;
        }
        List<String> first = tokens(left);
        return first != null && first.equals(tokens(right));
    }

    private static boolean swappable(BinaryOperation operation)
    {
        return operation == BinaryOperation.PLUS || operation == BinaryOperation.MULTIPLY
            || operation == BinaryOperation.EQ || operation == BinaryOperation.AND || operation == BinaryOperation.OR;
    }

    private static List<String> tokens(Expression expression)
    {
        var node = NodeModelUtils.findActualNodeFor(expression);
        if (node == null)
        {
            return null;
        }
        List<String> result = new ArrayList<>();
        for (var leaf : node.getLeafNodes())
        {
            if (leaf.getSyntaxErrorMessage() != null)
            {
                return null;
            }
            if (!leaf.isHidden())
            {
                result.add(expression instanceof StringLiteral ? leaf.getText()
                    : leaf.getText().toLowerCase(Locale.ROOT));
            }
        }
        while (outerParentheses(result))
        {
            result = new ArrayList<>(result.subList(1, result.size() - 1));
        }
        return result;
    }

    private static boolean outerParentheses(List<String> tokens)
    {
        if (tokens.size() < 3 || !"(".equals(tokens.get(0)) || !")".equals(tokens.get(tokens.size() - 1))) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return false;
        }
        int balance = 0;
        for (int i = 0; i < tokens.size(); i++)
        {
            if ("(".equals(tokens.get(i))) //$NON-NLS-1$
            {
                balance++;
            }
            else if (")".equals(tokens.get(i))) //$NON-NLS-1$
            {
                balance--;
            }
            if (balance == 0 && i < tokens.size() - 1)
            {
                return false;
            }
        }
        return balance == 0;
    }

    static boolean parenthesized(Expression expression)
    {
        var node = NodeModelUtils.findActualNodeFor(expression);
        if (node == null)
        {
            return false;
        }
        List<String> tokens = new ArrayList<>();
        for (var leaf : node.getLeafNodes())
        {
            if (!leaf.isHidden())
            {
                tokens.add(leaf.getText());
            }
        }
        return outerParentheses(tokens);
    }
}
