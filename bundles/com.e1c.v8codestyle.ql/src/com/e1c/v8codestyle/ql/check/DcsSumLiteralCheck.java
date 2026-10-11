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
 *     malikov-pro - port of the APK check АПК_01193
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.dcs.model.schema.DataCompositionSchemaDataSetQuery;
import com._1c.g5.v8.dt.ql.model.AbstractExpression;
import com._1c.g5.v8.dt.ql.model.CommonUnaryNegOperatorExpression;
import com._1c.g5.v8.dt.ql.model.FunctionInvocationExpression;
import com._1c.g5.v8.dt.ql.model.NumberLiteralExpression;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.g5.v8.dt.ql.check.QlBasicDelegateCheck;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.ql.CorePlugin;

/** Reports SUM of a numeric literal in a DCS query data set. */
public class DcsSumLiteralCheck
    extends QlBasicDelegateCheck
{
    /** Source rule identifier. */
    public static final String CHECK_ID = "apk-01193-dcs-sum-literal"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.DcsSumLiteralCheck_title)
            .description(Messages.DcsSumLiteralCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(CHECK_ID, CorePlugin.PLUGIN_ID))
            .delegate(FunctionInvocationExpression.class);
    }

    @Override
    protected void checkQlObject(EObject object, QueryOwner owner, IQlResultAcceptor resultAcceptor,
        ICheckParameters parameters, IProgressMonitor monitor)
    {
        if (monitor.isCanceled() || owner == null || !(owner.getOwner() instanceof DataCompositionSchemaDataSetQuery))
        {
            return;
        }
        FunctionInvocationExpression function = (FunctionInvocationExpression)object;
        var type = function.getFunctionType();
        if (type == null || !("SUM".equalsIgnoreCase(type.getName()) //$NON-NLS-1$
            || "СУММА".equalsIgnoreCase(type.getNameRu())) || function.isDistinct() || function.getParams().size() != 1) //$NON-NLS-1$
        {
            return;
        }
        AbstractExpression argument = function.getParams().get(0);
        if (!(argument instanceof NumberLiteralExpression)
            && !(argument instanceof CommonUnaryNegOperatorExpression unary
                && unary.getExpression() instanceof NumberLiteralExpression))
        {
            return;
        }
        INode argumentNode = NodeModelUtils.findActualNodeFor(argument);
        INode node = NodeModelUtils.findActualNodeFor(function);
        if (argumentNode == null || node == null)
        {
            return;
        }
        StringBuilder literal = new StringBuilder();
        for (var leaf : argumentNode.getLeafNodes())
        {
            if (leaf.getSyntaxErrorMessage() != null)
            {
                return;
            }
            if (!leaf.isHidden())
            {
                literal.append(leaf.getText());
            }
        }
        // APK skips only the spelling "0", not numeric zero in general.
        if ("0".contentEquals(literal)) //$NON-NLS-1$
        {
            return;
        }
        for (var leaf : node.getLeafNodes())
        {
            if (leaf.getSyntaxErrorMessage() != null)
            {
                return;
            }
        }
        resultAcceptor.addIssue(Messages.DcsSumLiteralCheck_message, node.getStartLine(), node.getOffset(),
            node.getLength());
    }
}
