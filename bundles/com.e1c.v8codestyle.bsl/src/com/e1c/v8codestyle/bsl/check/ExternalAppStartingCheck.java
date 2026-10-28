/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FEATURE_ACCESS__NAME;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.WrongParameterException;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.OptInCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Name-only opt-in audit of global and member application-start calls. */
public class ExternalAppStartingCheck extends BasicCheck
{
    public static final String CHECK_ID = "external-app-starting"; //$NON-NLS-1$
    public static final String USER_PATTERN = "userPatternString"; //$NON-NLS-1$
    public static final String CHECK_GOTO_URL = "checkGotoUrl"; //$NON-NLS-1$
    public static final String DEFAULT_PATTERN =
        "КомандаСистемы|System|ЗапуститьСистему|RunSystem|ЗапуститьПриложение|RunApp" //$NON-NLS-1$
            + "|НачатьЗапускПриложения|BeginRunningApplication" //$NON-NLS-1$
            + "|ЗапуститьПриложениеАсинх|RunAppAsync|ЗапуститьПрограмму|ОткрытьПроводник|ОткрытьФайл"; //$NON-NLS-1$
    public static final String NAVIGATION_PATTERN =
        "|ПерейтиПоНавигационнойСсылке|GotoURL|ОткрытьНавигационнуюСсылку"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.ExternalAppStartingCheck_title)
            .description(Messages.ExternalAppStartingCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.SECURITY)
            .extension(new OptInCheckExtension())
            .module().checkedObjectType(MODULE)
            .parameter(USER_PATTERN, String.class, DEFAULT_PATTERN, Messages.ExternalAppStartingCheck_pattern)
            .parameter(CHECK_GOTO_URL, Boolean.class, Boolean.FALSE.toString(),
                Messages.ExternalAppStartingCheck_navigation);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        String regex;
        boolean navigation;
        try
        {
            regex = parameters.getString(USER_PATTERN);
            navigation = parameters.getBoolean(CHECK_GOTO_URL);
        }
        catch (WrongParameterException exception)
        {
            BslPlugin.log(BslPlugin.createWarningStatus(
                "Check external-app-starting: cannot read parameters: " + exception.getMessage())); //$NON-NLS-1$
            return;
        }
        if (regex == null)
        {
            return;
        }
        if (navigation)
        {
            // Append literally, without regrouping the user's regex, as pinned LS does.
            regex += NAVIGATION_PATTERN;
        }
        Pattern pattern;
        try
        {
            pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
        catch (PatternSyntaxException exception)
        {
            BslPlugin.log(BslPlugin.createWarningStatus(
                "Check external-app-starting: invalid regex: " + exception.getMessage())); //$NON-NLS-1$
            return;
        }
        var contents = ((Module)object).eAllContents();
        while (contents.hasNext() && !monitor.isCanceled())
        {
            if (contents.next() instanceof Invocation call)
            {
                var access = call.getMethodAccess();
                if (access != null && access.getName() != null && pattern.matcher(access.getName()).matches())
                {
                    acceptor.addIssue(Messages.ExternalAppStartingCheck_message, access, FEATURE_ACCESS__NAME);
                }
            }
        }
    }
}
