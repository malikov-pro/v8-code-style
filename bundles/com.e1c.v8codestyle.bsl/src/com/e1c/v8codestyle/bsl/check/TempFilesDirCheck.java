/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FEATURE_ACCESS__NAME;

import org.eclipse.core.runtime.IProgressMonitor;

import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.OptInCheckExtension;

/** Opt-in audit of global temporary-directory calls; member calls are outside the LS scope. */
public class TempFilesDirCheck extends BasicCheck
{
    public static final String CHECK_ID = "temp-files-dir"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.TempFilesDirCheck_title)
            .description(Messages.TempFilesDirCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.SECURITY)
            .extension(new OptInCheckExtension())
            .module().checkedObjectType(INVOCATION);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        if (monitor.isCanceled())
        {
            return;
        }
        Invocation call = (Invocation)object;
        if (call.getMethodAccess() instanceof StaticFeatureAccess access
            && ("TempFilesDir".equalsIgnoreCase(access.getName()) //$NON-NLS-1$
                || "КаталогВременныхФайлов".equalsIgnoreCase(access.getName()))) //$NON-NLS-1$
        {
            acceptor.addIssue(Messages.TempFilesDirCheck_message, access, FEATURE_ACCESS__NAME);
        }
    }
}
