/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Module;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Missing explicit declared languages in NStr used by a global StrTemplate call. */
public final class MultilingualStringUsingWithTemplateCheck extends BasicCheck
{
    public static final String CHECK_ID = "multilingual-string-using-with-template"; //$NON-NLS-1$
    public static final String DECLARED_LANGUAGES = "declaredLanguages"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.MultilingualStringUsingWithTemplateCheck_title)
            .description(Messages.MultilingualStringUsingWithTemplateCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module().checkedObjectType(INVOCATION)
            .parameter(DECLARED_LANGUAGES, String.class, "ru", Messages.MultilingualStringCheck_languages); //$NON-NLS-1$
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Invocation call = (Invocation)object;
        var missing = MultilingualStringAnalysis.missing(call, parameters.getString(DECLARED_LANGUAGES));
        var node = NodeModelUtils.getNode(call);
        if (monitor.isCanceled() || missing.isEmpty() || node == null || !MultilingualStringAnalysis.template(call))
        {
            return;
        }
        for (var leaf : node.getLeafNodes())
        {
            if (leaf.getSyntaxErrorMessage() != null)
            {
                return;
            }
        }
        Module module = EcoreUtil2.getContainerOfType(call, Module.class);
        acceptor.addIssue(new BslDirectLocationIssue(MessageFormat.format(Messages.MultilingualStringCheck_message,
            missing.toString()), new DirectLocation(node.getOffset(), node.getLength(), node.getStartLine(), module)));
    }
}
