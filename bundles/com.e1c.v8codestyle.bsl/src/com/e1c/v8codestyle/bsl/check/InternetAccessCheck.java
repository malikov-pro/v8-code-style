/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.OPERATOR_STYLE_CREATOR;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FUNCTION_STYLE_CREATOR;

import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;

import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.OptInCheckExtension;

/** Opt-in audit of internet-resource constructors, independent of timeout checks. */
public class InternetAccessCheck extends BasicCheck
{
    public static final String CHECK_ID = "internet-access"; //$NON-NLS-1$

    private static final Pattern TYPES = Pattern.compile(
        "FTPСоединение|FTPConnection|HTTPСоединение|HTTPConnection|WSОпределения|WSDefinitions|WSПрокси|WSProxy" //$NON-NLS-1$
            + "|ИнтернетПочтовыйПрофиль|InternetMailProfile|ИнтернетПочта|InternetMail|Почта|Mail" //$NON-NLS-1$
            + "|HTTPЗапрос|HTTPRequest|ИнтернетПрокси|InternetProxy", //$NON-NLS-1$
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.InternetAccessCheck_title)
            .description(Messages.InternetAccessCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
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
        if (name != null && TYPES.matcher(name).matches())
        {
            acceptor.addIssue(Messages.InternetAccessCheck_message, creator);
        }
    }
}
