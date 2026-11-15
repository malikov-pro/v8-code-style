/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;

import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.Pragma;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Reports calls of local server methods from the form events
 * {@code ПриАктивизацииСтроки}/{@code OnActivateRow} and
 * {@code НачалоВыбора}/{@code OnStartChoice}. Only {@code &НаСервере}
 * ({@code &AtServer}) methods count; {@code &НаСервереБезКонтекста}
 * ({@code &AtServerNoContext}) is allowed and excluded.
 */
public final class ServerCallsInFormEventsCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "server-calls-in-form-events"; //$NON-NLS-1$

    private static final Pattern FORBIDDEN_EVENT =
        Pattern.compile(".*(ПриАктивизацииСтроки|OnActivateRow|НачалоВыбора|OnStartChoice)$", //$NON-NLS-1$
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.ServerCallsInFormEventsCheck_title)
            .description(Messages.ServerCallsInFormEventsCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module().checkedObjectType(INVOCATION);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Invocation call = (Invocation)object;
        if (monitor.isCanceled() || !(call.getMethodAccess() instanceof StaticFeatureAccess access))
        {
            return;
        }
        Module module = EcoreUtil2.getContainerOfType(call, Module.class);
        if (module == null)
        {
            return;
        }
        Method event = EcoreUtil2.getContainerOfType(call, Method.class);
        if (event == null || !FORBIDDEN_EVENT.matcher(event.getName()).matches())
        {
            return;
        }
        Method server = EcoreUtil2.getAllContentsOfType(module, Method.class).stream()
            .filter(method -> access.getName().equalsIgnoreCase(method.getName()))
            .filter(ServerCallsInFormEventsCheck::isAtServer)
            .findFirst().orElse(null);
        if (server == null)
        {
            return;
        }
        INode node = NodeModelUtils.getNode(call);
        if (node != null)
        {
            acceptor.addIssue(new BslDirectLocationIssue(Messages.ServerCallsInFormEventsCheck_message,
                new DirectLocation(node.getOffset(), node.getLength(), node.getStartLine(), module)));
        }
    }

    private static boolean isAtServer(Method method)
    {
        for (Pragma pragma : method.getPragmas())
        {
            String symbol = pragma.getSymbol();
            if (symbol != null && ("НаСервере".equalsIgnoreCase(symbol) || "AtServer".equalsIgnoreCase(symbol))) //$NON-NLS-1$ //$NON-NLS-2$
            {
                return true;
            }
        }
        return false;
    }
}
