/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - initial API and implementation
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com.e1c.g5.v8.dt.ql.check.QlBasicDelegateCheck.IQlResultAcceptor;

/** Locates the OR keyword rather than the beginning of the owning query. */
final class LogicalOrIssueLocation
{
    private LogicalOrIssueLocation()
    {
    }

    static void addIssue(String message, EObject expression, IQlResultAcceptor acceptor)
    {
        INode node = NodeModelUtils.findActualNodeFor(expression);
        if (node != null)
        {
            for (ILeafNode leaf : node.getLeafNodes())
            {
                if (!leaf.isHidden() && leaf.getSemanticElement() == expression
                    && leaf.getGrammarElement() instanceof Keyword keyword
                    && ("ИЛИ".equalsIgnoreCase(keyword.getValue()) || "OR".equalsIgnoreCase(keyword.getValue()))) //$NON-NLS-1$ //$NON-NLS-2$
                {
                    acceptor.addIssue(message, leaf.getStartLine(), leaf.getOffset(), leaf.getLength());
                    return;
                }
            }
        }
        acceptor.addIssue(message, expression, null);
    }
}
