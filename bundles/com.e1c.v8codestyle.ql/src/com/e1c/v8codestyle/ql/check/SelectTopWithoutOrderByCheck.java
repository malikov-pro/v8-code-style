/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.ql.model.QuerySchemaOperator;
import com._1c.g5.v8.dt.ql.model.QuerySchemaSelectQuery;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.g5.v8.dt.ql.check.QlBasicDelegateCheck;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.ql.CorePlugin;

/** TOP limitations without ORDER BY, retaining the pinned LS TOP0/1 and WHERE exclusions. */
public class SelectTopWithoutOrderByCheck extends QlBasicDelegateCheck
{
    public static final String CHECK_ID = "select-top-without-order-by"; //$NON-NLS-1$
    public static final String SKIP_SELECT_TOP_ONE = "skipSelectTopOne"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.SelectTopWithoutOrderByCheck_title)
            .description(Messages.SelectTopWithoutOrderByCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .delegate(QuerySchemaOperator.class);
        builder.parameter(SKIP_SELECT_TOP_ONE, Boolean.class, Boolean.TRUE.toString(),
            Messages.SelectTopWithoutOrderByCheck_skipOne);
    }

    @Override
    protected void checkQlObject(EObject object, QueryOwner owner, IQlResultAcceptor acceptor,
        ICheckParameters parameters, IProgressMonitor monitor)
    {
        var operator = (QuerySchemaOperator)object;
        String count = operator.getGetRecordsCount();
        if (monitor.isCanceled() || count == null || !count.matches("[0-9]+") //$NON-NLS-1$
            || !(operator.eContainer() instanceof QuerySchemaSelectQuery query))
        {
            return;
        }
        boolean union = query.getOperators().size() > 1;
        if (!union && !query.getOrderExpressions().isEmpty())
        {
            return;
        }
        boolean allowedNumber = "0".equals(count) || "1".equals(count); //$NON-NLS-1$ //$NON-NLS-2$
        if (allowedNumber && (operator.getFilters() != null
            || (!union && parameters.getBoolean(SKIP_SELECT_TOP_ONE))))
        {
            return;
        }
        var node = NodeModelUtils.getNode(operator);
        if (node != null)
        {
            ILeafNode top = null;
            for (var leaf : node.getLeafNodes())
            {
                if (leaf.isHidden())
                {
                    continue;
                }
                if (top != null)
                {
                    // Pinned LS selects the TOP keyword and its decimal, including intervening trivia.
                    if (leaf.getSemanticElement() == operator && count.equals(leaf.getText()))
                    {
                        acceptor.addIssue(Messages.SelectTopWithoutOrderByCheck_message, top.getStartLine(),
                            top.getOffset(), leaf.getEndOffset() - top.getOffset());
                    }
                    return;
                }
                if (leaf.getSemanticElement() == operator && leaf.getGrammarElement() instanceof Keyword keyword
                    && ("TOP".equalsIgnoreCase(keyword.getValue()) //$NON-NLS-1$
                        || "ПЕРВЫЕ".equalsIgnoreCase(keyword.getValue()))) //$NON-NLS-1$
                {
                    top = leaf;
                }
            }
        }
    }
}
