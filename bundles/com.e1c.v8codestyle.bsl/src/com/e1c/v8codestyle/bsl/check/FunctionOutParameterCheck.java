/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * This program and the accompanying materials are made available under the
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 * Contributors: malikov-pro - initial API and implementation
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FEATURE_ACCESS__NAME;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FUNCTION;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.FormalParam;
import com._1c.g5.v8.dt.bsl.model.Function;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.OptInCheckExtension;

/** Reports direct assignments to by-reference function parameters; opt-in as in LS. */
public class FunctionOutParameterCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "function-out-parameter"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.FunctionOutParameterCheck_title)
            .description(Messages.FunctionOutParameterCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.WARNING)
            .extension(new OptInCheckExtension())
            .module()
            .checkedObjectType(FUNCTION);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Function function = (Function)object;
        Set<String> parametersByReference = new HashSet<>();
        for (FormalParam parameter : function.getFormalParams())
        {
            if (!parameter.isByValue())
            {
                parametersByReference.add(parameter.getName().toLowerCase(Locale.ROOT));
            }
        }
        for (SimpleStatement statement : EcoreUtil2.getAllContentsOfType(function, SimpleStatement.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (statement.getRight() != null && statement.getLeft() instanceof StaticFeatureAccess access
                && parametersByReference.remove(access.getName().toLowerCase(Locale.ROOT)))
            {
                acceptor.addIssue(Messages.FunctionOutParameterCheck_message, access, FEATURE_ACCESS__NAME);
            }
        }
    }
}
