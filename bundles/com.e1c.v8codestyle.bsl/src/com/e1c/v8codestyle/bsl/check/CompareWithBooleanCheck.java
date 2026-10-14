/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.BINARY_EXPRESSION;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.BinaryExpression;
import com._1c.g5.v8.dt.bsl.model.BinaryOperation;
import com._1c.g5.v8.dt.bsl.model.BooleanLiteral;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.resource.TypesComputer;
import com._1c.g5.v8.dt.mcore.Environmental;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.platform.IEObjectTypeNames;
import com._1c.g5.v8.dt.mcore.util.McoreUtil;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;
import com.google.inject.Inject;

/** Checks comparison with a Boolean literal only when the other type is solely Boolean. */
public class CompareWithBooleanCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "compare-with-boolean"; //$NON-NLS-1$
    private final TypesComputer computer;

    @Inject
    public CompareWithBooleanCheck(TypesComputer computer)
    {
        this.computer = computer;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.CompareWithBooleanCheck_title)
            .description(Messages.CompareWithBooleanCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(BINARY_EXPRESSION);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        BinaryExpression expression = (BinaryExpression)object;
        if (monitor.isCanceled() || (expression.getOperation() != BinaryOperation.EQ
            && expression.getOperation() != BinaryOperation.NE))
        {
            return;
        }
        boolean redundant = expression.getLeft() instanceof BooleanLiteral && isSoleBoolean(expression.getRight())
            || expression.getRight() instanceof BooleanLiteral && isSoleBoolean(expression.getLeft());
        if (!redundant)
        {
            return;
        }
        INode node = NodeModelUtils.getNode(expression);
        if (node == null)
        {
            return;
        }
        ILeafNode first = null;
        ILeafNode last = null;
        for (ILeafNode leaf : node.getLeafNodes())
        {
            if (leaf.getSyntaxErrorMessage() != null)
            {
                return;
            }
            if (!leaf.isHidden())
            {
                if (first == null)
                {
                    first = leaf;
                }
                last = leaf;
            }
        }
        if (first != null && last != null)
        {
            acceptor.addIssue(new BslDirectLocationIssue(Messages.CompareWithBooleanCheck_message,
                new DirectLocation(first.getOffset(), last.getEndOffset() - first.getOffset(),
                    first.getStartLine(), expression)));
        }
    }

    private boolean isSoleBoolean(Expression expression)
    {
        if (expression instanceof BooleanLiteral)
        {
            return true;
        }
        if (expression == null)
        {
            return false;
        }
        Environmental environment = EcoreUtil2.getContainerOfType(expression, Environmental.class);
        if (environment == null)
        {
            return false;
        }
        var types = computer.computeTypes(expression, environment.environments());
        if (types.isEmpty())
        {
            return false;
        }
        for (TypeItem type : types)
        {
            TypeItem resolved = (TypeItem)EcoreUtil.resolve(type, expression);
            if (resolved.eIsProxy() || !IEObjectTypeNames.BOOLEAN.equals(McoreUtil.getTypeName(resolved)))
            {
                return false;
            }
        }
        return true;
    }
}
