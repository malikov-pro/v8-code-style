/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.dt.metadata.dbview.DbViewSelectDef;
import com._1c.g5.v8.dt.metadata.mdclass.FilterCriterion;
import com._1c.g5.v8.dt.ql.model.QuerySchemaTable;
import com._1c.g5.v8.dt.ql.model.QuerySchemaTableParameter;
import com._1c.g5.v8.dt.ql.model.EmptyExpression;
import com._1c.g5.v8.dt.ql.typesystem.IDynamicDbViewFieldComputer;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.g5.v8.dt.ql.check.QlBasicDelegateCheck;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.ql.CorePlugin;
import com.google.inject.Inject;

/** Checks empty virtual-table parameter positions, with criterion and Subconto boundaries. */
public class VirtualTableCallWithoutParametersCheck extends QlBasicDelegateCheck
{
    public static final String CHECK_ID = "virtual-table-call-without-parameters"; //$NON-NLS-1$
    private final IDynamicDbViewFieldComputer computer;

    @Inject
    public VirtualTableCallWithoutParametersCheck(IDynamicDbViewFieldComputer computer)
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
        builder.title(Messages.VirtualTableCallWithoutParametersCheck_title)
            .description(Messages.VirtualTableCallWithoutParametersCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .delegate(QuerySchemaTable.class);
    }

    @Override
    protected void checkQlObject(EObject object, QueryOwner owner, IQlResultAcceptor acceptor,
        ICheckParameters parameters, IProgressMonitor monitor)
    {
        var table = (QuerySchemaTable)object;
        if (monitor.isCanceled() || table.getTable() == null
            || !(computer.computeDbView(table.getTable()) instanceof DbViewSelectDef dbView))
        {
            // Accounting Subconto is a resolved DbViewTableDef, deliberately outside this rule.
            return;
        }
        var arguments = table.getParameters();
        int start = dbView.getMdObject() instanceof FilterCriterion || arguments.size() <= 1 ? 0 : 1;
        for (int i = start; i < arguments.size(); i++)
        {
            if (monitor.isCanceled() || hasContent(arguments.get(i)))
            {
                return;
            }
        }
        VirtualTableIssueLocation.addIssue(Messages.VirtualTableCallWithoutParametersCheck_message, table, acceptor);
    }

    private static boolean hasContent(QuerySchemaTableParameter parameter)
    {
        var expression = parameter.getExpression();
        if (expression != null && expression.getExpression() != null
            && !(expression.getExpression() instanceof EmptyExpression))
        {
            return true;
        }
        return parameter.getExpressionDcs().stream().anyMatch(item -> item.getExpression() != null
            && !(item.getExpression() instanceof EmptyExpression));
    }
}
