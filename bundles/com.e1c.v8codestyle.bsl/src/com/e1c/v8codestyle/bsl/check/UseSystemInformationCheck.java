/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.OPERATOR_STYLE_CREATOR;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FUNCTION_STYLE_CREATOR;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;

import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.OptInCheckExtension;

/** Opt-in audit of system-information constructors, following the pinned LS syntax contract. */
public class UseSystemInformationCheck extends BasicCheck
{
    public static final String CHECK_ID = "use-system-information"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.UseSystemInformationCheck_title)
            .description(Messages.UseSystemInformationCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.SECURITY)
            .extension(new OptInCheckExtension())
            .module().checkedObjectType(OPERATOR_STYLE_CREATOR, FUNCTION_STYLE_CREATOR);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        if (monitor.isCanceled())
        {
            return;
        }
        EObject creator = (EObject)object;
        String name = AuditConstructorName.of(creator);
        if ("SystemInfo".equalsIgnoreCase(name) || "СистемнаяИнформация".equalsIgnoreCase(name)) //$NON-NLS-1$ //$NON-NLS-2$
        {
            acceptor.addIssue(Messages.UseSystemInformationCheck_message, creator);
        }
    }

}
