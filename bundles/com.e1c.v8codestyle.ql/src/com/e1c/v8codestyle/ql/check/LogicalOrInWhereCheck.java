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
 *     malikov-pro - port of the BSL Language Server diagnostic LogicalOrInTheWhereSectionOfQuery
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.ql.model.LogicalOROperatorExpression;
import com._1c.g5.v8.dt.ql.model.QuerySchemaExpression;
import com._1c.g5.v8.dt.ql.model.QuerySchemaOperator;
import com._1c.g5.v8.dt.ql.model.QuerySchemaQuerySourceJoin;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.g5.v8.dt.ql.check.QlBasicDelegateCheck;
import com.e1c.v8codestyle.check.StandardCheckExtension;
import com.e1c.v8codestyle.internal.ql.CorePlugin;

/**
 * The logical OR operator in the WHERE section of a query may prevent the
 * DBMS from using table indexes, which leads to scans.
 * <p>
 * Port of the BSL Language Server diagnostic LogicalOrInTheWhereSectionOfQuery
 * (1C:Standards 658). OR in join conditions is reported by the
 * {@code ql-logical-or-in-join} check.
 *
 * @author malikov-pro
 */
public class LogicalOrInWhereCheck
    extends QlBasicDelegateCheck
{

    private static final String CHECK_ID = "ql-logical-or-in-where"; //$NON-NLS-1$

    /**
     * Instantiates a new logical OR in WHERE section check.
     */
    public LogicalOrInWhereCheck()
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
        builder.title(Messages.LogicalOrInWhereCheck_title)
            .description(Messages.LogicalOrInWhereCheck_description)
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
        if (EcoreUtil2.getContainerOfType(object, QuerySchemaQuerySourceJoin.class) != null)
        {
            // OR in a join condition: reported by ql-logical-or-in-join.
            return;
        }
        QuerySchemaOperator operator = EcoreUtil2.getContainerOfType(object, QuerySchemaOperator.class);
        if (operator == null || !isInFilter(operator, object))
        {
            return;
        }
        LogicalOrIssueLocation.addIssue(Messages.LogicalOrInWhereCheck_Logical_OR_in_where_section_not_allowed,
            object, resultAceptor);
    }

    private boolean isInFilter(QuerySchemaOperator operator, EObject object)
    {
        QuerySchemaExpression filter = operator.getFilters();
        return filter != null && (filter == object || EcoreUtil.isAncestor(filter, object));
    }

}
