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
 *     malikov-pro - port of the BSL Language Server diagnostic AssignAliasFieldsInQuery
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.ql.model.QuerySchemaExpression;
import com._1c.g5.v8.dt.ql.model.QuerySchemaNestedQuery;
import com._1c.g5.v8.dt.ql.model.QuerySchemaOperator;
import com._1c.g5.v8.dt.ql.model.QuerySchemaSelectQuery;
import com._1c.g5.v8.dt.ql.model.StarExpression;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.g5.v8.dt.ql.check.QlBasicDelegateCheck;
import com.e1c.v8codestyle.check.StandardCheckExtension;
import com.e1c.v8codestyle.internal.ql.CorePlugin;

/**
 * Fields of a top-level query should have explicit aliases: it makes the
 * query text clearer and the code using the query result more stable.
 * <p>
 * Port of the BSL Language Server diagnostic AssignAliasFieldsInQuery
 * (1C:Standards 437). Asterisk fields ({@code Т.*}) are skipped as well as
 * fields of sub-queries.
 *
 * @author malikov-pro
 */
public class AssignAliasFieldsCheck
    extends QlBasicDelegateCheck
{

    private static final String CHECK_ID = "ql-assign-alias-fields"; //$NON-NLS-1$

    /**
     * Instantiates a new assign alias fields check.
     */
    public AssignAliasFieldsCheck()
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
        builder.title(Messages.AssignAliasFieldsCheck_title)
            .description(Messages.AssignAliasFieldsCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new StandardCheckExtension(437, getCheckId(), CorePlugin.PLUGIN_ID))
            .delegate(QuerySchemaExpression.class);
    }

    @Override
    protected void checkQlObject(EObject object, QueryOwner owner, IQlResultAcceptor resultAceptor,
        ICheckParameters parameters, IProgressMonitor monitor)
    {
        QuerySchemaOperator operator = EcoreUtil2.getContainerOfType(object, QuerySchemaOperator.class);
        if (operator == null || !operator.getSelectFields().contains(object))
        {
            // Only select fields of an operator are checked.
            return;
        }
        QuerySchemaSelectQuery selectQuery = EcoreUtil2.getContainerOfType(operator, QuerySchemaSelectQuery.class);
        if (selectQuery == null || selectQuery.eContainer() instanceof QuerySchemaNestedQuery)
        {
            // Fields of sub-queries are not checked (LS semantics).
            return;
        }
        QuerySchemaExpression field = (QuerySchemaExpression)object;
        if (field.getExpression() instanceof StarExpression)
        {
            // No alias is expected for asterisk fields.
            return;
        }
        String alias = field.getAlias();
        if (field.isComputeAlias() || alias == null || alias.isBlank())
        {
            resultAceptor.addIssue(
                Messages.AssignAliasFieldsCheck_Select_field_must_have_an_alias);
        }
    }

}
