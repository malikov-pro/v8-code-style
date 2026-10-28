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
 *     malikov-pro - port of the BSL Language Server diagnostic FullOuterJoinQuery
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check;

import static com._1c.g5.v8.dt.ql.model.QlPackage.Literals.QUERY_SCHEMA_QUERY_SOURCE_JOIN__JOIN_TYPE;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.dt.ql.model.QuerySchemaJoinType;
import com._1c.g5.v8.dt.ql.model.QuerySchemaQuerySourceJoin;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.g5.v8.dt.ql.check.QlBasicDelegateCheck;
import com.e1c.v8codestyle.check.StandardCheckExtension;
import com.e1c.v8codestyle.internal.ql.CorePlugin;

/**
 * The full outer join in a query considerably decreases performance with
 * PostgreSQL DBMS, avoid it when possible.
 * <p>
 * Port of the BSL Language Server diagnostic FullOuterJoinQuery
 * (1C:Standards 435).
 *
 * @author malikov-pro
 */
public class FullOuterJoinCheck
    extends QlBasicDelegateCheck
{

    private static final String CHECK_ID = "ql-full-outer-join"; //$NON-NLS-1$

    /**
     * Instantiates a new full outer join check.
     */
    public FullOuterJoinCheck()
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
        builder.title(Messages.FullOuterJoinCheck_title)
            .description(Messages.FullOuterJoinCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new StandardCheckExtension(435, getCheckId(), CorePlugin.PLUGIN_ID))
            .delegate(QuerySchemaQuerySourceJoin.class);
    }

    @Override
    protected void checkQlObject(EObject object, QueryOwner owner, IQlResultAcceptor resultAceptor,
        ICheckParameters parameters, IProgressMonitor monitor)
    {
        QuerySchemaQuerySourceJoin join = (QuerySchemaQuerySourceJoin)object;

        if (join.getJoinType() == QuerySchemaJoinType.FULL_OUTER)
        {
            resultAceptor.addIssue(Messages.FullOuterJoinCheck_Full_outer_join_not_allowed, object,
                QUERY_SCHEMA_QUERY_SOURCE_JOIN__JOIN_TYPE);
        }
    }

}
