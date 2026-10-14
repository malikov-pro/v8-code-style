/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.NAMED_ELEMENT__NAME;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.ReturnStatement;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.OptInCheckExtension;

/** Counts return statements in methods, including nested and unreachable ones. */
public class TooManyReturnsCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "too-many-returns"; //$NON-NLS-1$
    public static final String PARAM_MAX_RETURNS = "maxReturnsCount"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.TooManyReturnsCheck_title)
            .description(Messages.TooManyReturnsCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.WARNING)
            .extension(new OptInCheckExtension())
            .module()
            .checkedObjectType(METHOD)
            .parameter(PARAM_MAX_RETURNS, Integer.class, "3", Messages.TooManyReturnsCheck_parameter); //$NON-NLS-1$
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        if (monitor.isCanceled())
        {
            return;
        }
        Method method = (Method)object;
        int count = EcoreUtil2.getAllContentsOfType(method, ReturnStatement.class).size();
        int max = parameters.getInt(PARAM_MAX_RETURNS);
        if (count > max)
        {
            acceptor.addIssue(MessageFormat.format(Messages.TooManyReturnsCheck_message,
                method.getName(), Integer.valueOf(count), Integer.valueOf(max)), method, NAMED_ELEMENT__NAME);
        }
    }
}
