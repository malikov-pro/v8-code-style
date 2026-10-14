/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.NAMED_ELEMENT__NAME;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;

import com._1c.g5.v8.dt.bsl.model.FormalParam;
import com._1c.g5.v8.dt.bsl.model.Method;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Configurable reserved formal parameter names; empty mask has no effect. */
public class ReservedParameterNamesCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "reserved-parameter-names"; //$NON-NLS-1$
    public static final String PARAM_RESERVED_WORDS = "reservedWords"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.ReservedParameterNamesCheck_title)
            .description(Messages.ReservedParameterNamesCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(METHOD)
            .parameter(PARAM_RESERVED_WORDS, String.class, "", Messages.ReservedParameterNamesCheck_parameter); //$NON-NLS-1$
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        String mask = parameters.getString(PARAM_RESERVED_WORDS).trim();
        if (mask.isEmpty())
        {
            return;
        }
        Pattern pattern;
        try
        {
            pattern = Pattern.compile("^" + mask + "$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE); //$NON-NLS-1$ //$NON-NLS-2$
        }
        catch (PatternSyntaxException e)
        {
            BslPlugin.log(BslPlugin.createWarningStatus("Check " + CHECK_ID + ": invalid reservedWords regex: " //$NON-NLS-1$ //$NON-NLS-2$
                + e.getMessage()));
            return;
        }
        for (FormalParam parameter : ((Method)object).getFormalParams())
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (pattern.matcher(parameter.getName()).find())
            {
                acceptor.addIssue(Messages.ReservedParameterNamesCheck_message, parameter, NAMED_ELEMENT__NAME);
            }
        }
    }
}
