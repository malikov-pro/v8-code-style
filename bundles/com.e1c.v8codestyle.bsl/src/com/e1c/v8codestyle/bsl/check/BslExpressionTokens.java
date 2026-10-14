/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.StringLiteral;

/** Compares source tokens without changing the content of string literals. */
final class BslExpressionTokens
{
    private BslExpressionTokens()
    {
    }

    static boolean equal(Expression left, Expression right)
    {
        if (left == null || right == null || left.eClass() != right.eClass())
        {
            return false;
        }
        List<String> leftTokens = tokens(left);
        List<String> rightTokens = tokens(right);
        return leftTokens != null && leftTokens.equals(rightTokens);
    }

    private static List<String> tokens(Expression expression)
    {
        INode node = NodeModelUtils.findActualNodeFor(expression);
        if (node == null)
        {
            return null;
        }
        List<String> tokens = new ArrayList<>();
        for (ILeafNode leaf : node.getLeafNodes())
        {
            if (leaf.getSyntaxErrorMessage() != null)
            {
                return null;
            }
            if (!leaf.isHidden())
            {
                String text = leaf.getText();
                boolean string = leaf.getSemanticElement() instanceof StringLiteral || text.startsWith("\""); //$NON-NLS-1$
                tokens.add(string ? text : text.toLowerCase(Locale.ROOT));
            }
        }
        return tokens;
    }
}
