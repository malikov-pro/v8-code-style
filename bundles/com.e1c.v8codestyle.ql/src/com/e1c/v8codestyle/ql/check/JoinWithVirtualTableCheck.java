/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check;

import static com._1c.g5.v8.dt.metadata.dbview.DbViewPackage.Literals.ACCOUNTING_REGISTER_DB_VIEW_DEFS__EXT_DIMENSIONS_VIEW;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.dt.metadata.dbview.DbViewSelectDef;
import com._1c.g5.v8.dt.ql.model.QuerySchemaSource;
import com._1c.g5.v8.dt.ql.model.QuerySchemaQuerySourceJoin;
import com._1c.g5.v8.dt.ql.model.QuerySchemaTable;
import com._1c.g5.v8.dt.ql.typesystem.IDynamicDbViewFieldComputer;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.g5.v8.dt.ql.check.QlBasicDelegateCheck;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.ql.CorePlugin;
import com.google.inject.Inject;

/** Detects a virtual table directly participating in a query join. */
public class JoinWithVirtualTableCheck extends QlBasicDelegateCheck
{
    public static final String CHECK_ID = "join-with-virtual-table"; //$NON-NLS-1$
    private final IDynamicDbViewFieldComputer computer;

    @Inject
    public JoinWithVirtualTableCheck(IDynamicDbViewFieldComputer computer)
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
        builder.title(Messages.JoinWithVirtualTableCheck_title)
            .description(Messages.JoinWithVirtualTableCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .delegate(QuerySchemaTable.class);
    }

    @Override
    protected void checkQlObject(EObject object, QueryOwner owner, IQlResultAcceptor acceptor,
        ICheckParameters parameters, IProgressMonitor monitor)
    {
        var table = (QuerySchemaTable)object;
        if (monitor.isCanceled() || table.getTable() == null
            || !(table.eContainer() instanceof QuerySchemaSource source)
            || (source.getJoins().isEmpty() && !(source.eContainer() instanceof QuerySchemaQuerySourceJoin)))
        {
            return;
        }
        var dbView = computer.computeDbView(table.getTable());
        if (!(dbView instanceof DbViewSelectDef) && (dbView == null
            || dbView.eContainingFeature() != ACCOUNTING_REGISTER_DB_VIEW_DEFS__EXT_DIMENSIONS_VIEW))
        {
            return;
        }
        VirtualTableIssueLocation.addIssue(Messages.JoinWithVirtualTableCheck_message, table, acceptor);
    }
}
