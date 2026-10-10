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

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.BinaryExpression;
import com._1c.g5.v8.dt.bsl.model.BinaryOperation;
import com._1c.g5.v8.dt.bsl.model.Method;
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

/** Reports NOT applied directly to inequality or to another NOT. */
public class DoubleNegativesCheck
    extends BasicCheck
{
    /** The BSL Language Server diagnostic identifier. */
    public static final String CHECK_ID = "double-negatives"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.DoubleNegativesCheck_title)
            .description(Messages.DoubleNegativesCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(METHOD);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAcceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        Method method = (Method)object;
        for (UnaryExpression unary : EcoreUtil2.getAllContentsOfType(method, UnaryExpression.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (unary.getOperation() != UnaryOperation.NOT || !isDoubleNegative(unary))
            {
                continue;
            }
            INode node = NodeModelUtils.findActualNodeFor(unary);
            if (node == null)
            {
                continue;
            }
            ILeafNode first = null;
            ILeafNode last = null;
            boolean syntaxError = false;
            for (ILeafNode leaf : node.getLeafNodes())
            {
                syntaxError |= leaf.getSyntaxErrorMessage() != null;
                if (!leaf.isHidden())
                {
                    if (first == null)
                    {
                        first = leaf;
                    }
                    last = leaf;
                }
            }
            if (!syntaxError && first != null && last != null)
            {
                resultAcceptor.addIssue(new BslDirectLocationIssue(Messages.DoubleNegativesCheck_message,
                    new DirectLocation(first.getOffset(), last.getEndOffset() - first.getOffset(),
                        first.getStartLine(), method)));
            }
        }
    }

    private static boolean isDoubleNegative(UnaryExpression unary)
    {
        if (unary.getOperand() instanceof BinaryExpression binary)
        {
            return binary.getOperation() == BinaryOperation.NE && binary.getLeft() != null
                && binary.getRight() != null;
        }
        return unary.getOperand() instanceof UnaryExpression nested
            && nested.getOperation() == UnaryOperation.NOT && nested.getOperand() != null;
    }
}
