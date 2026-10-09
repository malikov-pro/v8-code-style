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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.ql.model.LogicalOROperatorExpression;
import com._1c.g5.v8.dt.ql.model.QuerySchemaQuerySourceJoin;
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
        QuerySchemaQuerySourceJoin join = EcoreUtil2.getContainerOfType(object, QuerySchemaQuerySourceJoin.class);
        if (join != null)
        {
            resultAceptor.addIssue(Messages.LogicalOrInJoinCheck_Logical_OR_in_join_condition_not_allowed);
        }
    }

}
