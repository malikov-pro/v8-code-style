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
 *     malikov-pro - port of the BSL Language Server diagnostic LogicalOrInJoinQuerySection
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.ql.model.CommonExpression;
import com._1c.g5.v8.dt.ql.model.LogicalOROperatorExpression;
import com._1c.g5.v8.dt.ql.model.MultiPartCommonExpression;
import com._1c.g5.v8.dt.ql.model.QuerySchemaQuerySourceJoin;
import com._1c.g5.v8.dt.ql.model.QuerySchemaSelectQuery;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.g5.v8.dt.ql.check.QlBasicDelegateCheck;
import com.e1c.v8codestyle.check.StandardCheckExtension;
import com.e1c.v8codestyle.internal.ql.CorePlugin;

/**
 * The logical OR operator in join conditions may prevent the DBMS from using
 * table indexes, which leads to scans and lock contention.
 * <p>
 * Port of the BSL Language Server diagnostic LogicalOrInJoinQuerySection
 * (1C:Standards 658).
 *
 * @author malikov-pro
 */
public class LogicalOrInJoinCheck
    extends QlBasicDelegateCheck
{

    private static final String CHECK_ID = "ql-logical-or-in-join"; //$NON-NLS-1$

    /**
     * Instantiates a new logical OR in join check.
     */
    public LogicalOrInJoinCheck()
    {
        super();
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.LogicalOrInJoinCheck_title)
            .description(Messages.LogicalOrInJoinCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.PERFORMANCE)
            .extension(new StandardCheckExtension(658, getCheckId(), CorePlugin.PLUGIN_ID))
            .delegate(LogicalOROperatorExpression.class);
    }

    @Override
    protected void checkQlObject(EObject object, QueryOwner owner, IQlResultAcceptor resultAceptor,
        ICheckParameters parameters, IProgressMonitor monitor)
    {
        if (isJoinCondition(object) && hasMultipleFields(object))
        {
            LogicalOrIssueLocation.addIssue(Messages.LogicalOrInJoinCheck_Logical_OR_in_join_condition_not_allowed,
                object, resultAceptor);
        }
    }

    private boolean isJoinCondition(EObject object)
    {
        EObject child = object;
        for (EObject parent = child.eContainer(); parent != null; parent = child.eContainer())
        {
            if (parent instanceof QuerySchemaSelectQuery)
            {
                return false;
            }
            if (parent instanceof QuerySchemaQuerySourceJoin join)
            {
                return child == join.getCondition();
            }
            child = parent;
        }
        return false;
    }

    private boolean hasMultipleFields(EObject object)
    {
        INode group = NodeModelUtils.getNode(object);
        // Parentheses have no EObject in QL. The CST preserves the logical group,
        // including all ORs in a chain but excluding adjacent AND predicates.
        while (group != null && !(group.getGrammarElement() instanceof RuleCall call
            && "LogicalExpression".equals(call.getRule().getName()))) //$NON-NLS-1$
        {
            group = group.getParent();
        }
        if (group == null)
        {
            group = NodeModelUtils.findActualNodeFor(object);
        }
        if (group == null)
        {
            return false;
        }
        Set<String> fields = new HashSet<>();
        for (ILeafNode leaf : group.getLeafNodes())
        {
            if (leaf.isHidden() || !(leaf.getSemanticElement() instanceof CommonExpression field))
            {
                continue;
            }
            while (field.eContainer() instanceof MultiPartCommonExpression parent)
            {
                field = parent;
            }
            fields.add(field instanceof MultiPartCommonExpression multipart
                ? multipart.getFullContent() : field.getContent());
            if (fields.size() > 1)
            {
                return true;
            }
        }
        return false;
    }

}
