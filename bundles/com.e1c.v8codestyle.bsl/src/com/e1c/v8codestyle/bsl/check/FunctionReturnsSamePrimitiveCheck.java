/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FUNCTION;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.NAMED_ELEMENT__NAME;

import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Function;
import com._1c.g5.v8.dt.bsl.model.FunctionStyleCreator;
import com._1c.g5.v8.dt.bsl.model.OperatorStyleCreator;
import com._1c.g5.v8.dt.bsl.model.ReturnStatement;
import com._1c.g5.v8.dt.bsl.model.StringLiteral;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Checks functions with multiple returns of the same primitive expression. */
public class FunctionReturnsSamePrimitiveCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "function-returns-same-primitive"; //$NON-NLS-1$
    public static final String PARAM_ATTACHABLE = "skipAttachable"; //$NON-NLS-1$
    public static final String PARAM_CASE = "caseSensitiveForString"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.FunctionReturnsSamePrimitiveCheck_title)
            .description(Messages.FunctionReturnsSamePrimitiveCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(FUNCTION)
            .parameter(PARAM_ATTACHABLE, Boolean.class, "true", Messages.FunctionReturnsSamePrimitiveCheck_attachable) //$NON-NLS-1$
            .parameter(PARAM_CASE, Boolean.class, "false", Messages.FunctionReturnsSamePrimitiveCheck_case); //$NON-NLS-1$
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Function function = (Function)object;
        String name = function.getName().toLowerCase(Locale.ROOT);
        if (monitor.isCanceled() || parameters.getBoolean(PARAM_ATTACHABLE)
            && (name.startsWith("подключаемый_") || name.startsWith("attachable_"))) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return;
        }
        var returns = EcoreUtil2.getAllContentsOfType(function, ReturnStatement.class);
        if (returns.size() <= 1)
        {
            return;
        }
        String expected = null;
        boolean caseSensitive = parameters.getBoolean(PARAM_CASE);
        for (ReturnStatement statement : returns)
        {
            Expression expression = statement.getExpression();
            if (expression == null || !isPrimitiveExpression(expression))
            {
                return;
            }
            INode node = NodeModelUtils.findActualNodeFor(expression);
            if (node == null)
            {
                return;
            }
            StringBuilder text = new StringBuilder();
            for (ILeafNode leaf : node.getLeafNodes())
            {
                if (leaf.getSyntaxErrorMessage() != null)
                {
                    return;
                }
                if (!leaf.isHidden())
                {
                    text.append(leaf.getText());
                }
            }
            boolean hasString = expression instanceof StringLiteral
                || !EcoreUtil2.getAllContentsOfType(expression, StringLiteral.class).isEmpty();
            String value = caseSensitive && hasString ? text.toString() : text.toString().toUpperCase(Locale.ENGLISH);
            if (expected != null && !expected.equals(value))
            {
                return;
            }
            expected = value;
        }
        acceptor.addIssue(Messages.FunctionReturnsSamePrimitiveCheck_message, function, NAMED_ELEMENT__NAME);
    }

    private static boolean isPrimitiveExpression(Expression expression)
    {
        return !(expression instanceof FeatureAccess || expression instanceof OperatorStyleCreator
            || expression instanceof FunctionStyleCreator)
            && EcoreUtil2.getAllContentsOfType(expression, FeatureAccess.class).isEmpty()
            && EcoreUtil2.getAllContentsOfType(expression, OperatorStyleCreator.class).isEmpty()
            && EcoreUtil2.getAllContentsOfType(expression, FunctionStyleCreator.class).isEmpty();
    }
}
