/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.ql.model.QuerySchemaOperator;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.g5.v8.dt.ql.check.QlBasicDelegateCheck;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.ql.CorePlugin;

/** Reports UNION without ALL; no automatic rewrite because duplicate semantics differ. */
public class UnionAllCheck extends QlBasicDelegateCheck
{
    public static final String CHECK_ID = "union-all"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.UnionAllCheck_title)
            .description(Messages.UnionAllCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .delegate(QuerySchemaOperator.class);
    }

    @Override
    protected void checkQlObject(EObject object, QueryOwner owner, IQlResultAcceptor acceptor,
        ICheckParameters parameters, IProgressMonitor monitor)
    {
        var operator = (QuerySchemaOperator)object;
        // The first SELECT has unionType=UNION too, but no union operator flag.
        if (monitor.isCanceled() || !operator.isFlagUnionType() || operator.isFlagUnionTypeAll())
        {
            return;
        }
        var node = NodeModelUtils.getNode(operator);
        if (node != null)
        {
            for (var leaf : node.getLeafNodes())
            {
                if (!leaf.isHidden() && leaf.getSemanticElement() == operator
                    && leaf.getGrammarElement() instanceof Keyword keyword
                    && ("UNION".equalsIgnoreCase(keyword.getValue()) //$NON-NLS-1$
                        || "ОБЪЕДИНИТЬ".equalsIgnoreCase(keyword.getValue()))) //$NON-NLS-1$
                {
                    acceptor.addIssue(Messages.UnionAllCheck_message, leaf.getStartLine(), leaf.getOffset(),
                        leaf.getLength());
                    return;
                }
            }
        }
    }
}
