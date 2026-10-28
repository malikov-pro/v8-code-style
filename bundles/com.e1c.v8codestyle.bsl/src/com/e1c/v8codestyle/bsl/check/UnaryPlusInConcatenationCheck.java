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
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.NumberLiteral;
import com._1c.g5.v8.dt.bsl.model.UnaryExpression;
import com._1c.g5.v8.dt.bsl.model.UnaryOperation;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Reports a unary plus immediately following a plus, except on numeric literals. */
public class UnaryPlusInConcatenationCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "unary-plus-in-concatenation"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.UnaryPlusInConcatenationCheck_title)
            .description(Messages.UnaryPlusInConcatenationCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAcceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        Module module = (Module)object;
        INode moduleNode = NodeModelUtils.findActualNodeFor(module);
        if (moduleNode == null)
        {
            return;
        }
        Map<Integer, ILeafNode> previousTokens = new HashMap<>();
        ILeafNode previous = null;
        for (ILeafNode leaf : moduleNode.getLeafNodes())
        {
            if (!leaf.isHidden())
            {
                if (previous != null)
                {
                    previousTokens.put(leaf.getOffset(), previous);
                }
                previous = leaf;
            }
        }
        for (UnaryExpression unary : EcoreUtil2.getAllContentsOfType(module, UnaryExpression.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (unary.getOperation() != UnaryOperation.PLUS || unary.getOperand() == null
                || unary.getOperand() instanceof NumberLiteral)
            {
                continue;
            }
            INode node = NodeModelUtils.getNode(unary);
            if (node == null)
            {
                continue;
            }
            ILeafNode plus = null;
            boolean syntaxError = false;
            for (ILeafNode leaf : node.getLeafNodes())
            {
                syntaxError |= leaf.getSyntaxErrorMessage() != null;
                if (plus == null && !leaf.isHidden())
                {
                    plus = leaf;
                }
            }
            if (!syntaxError && plus != null && "+".equals(plus.getText())) //$NON-NLS-1$
            {
                ILeafNode before = previousTokens.get(plus.getOffset());
                if (before != null && "+".equals(before.getText())) //$NON-NLS-1$
                {
                    resultAcceptor.addIssue(new BslDirectLocationIssue(
                        Messages.UnaryPlusInConcatenationCheck_message,
                        new DirectLocation(plus.getOffset(), plus.getLength(), plus.getStartLine(), module)));
                }
            }
        }
    }
}
