/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.COMMON_MODULE;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.COMMON_MODULE__CONTEXT_DEF;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.COMMON_MODULE__SERVER;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.EVENT_SUBSCRIPTION;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.EVENT_SUBSCRIPTION__HANDLER;

import org.eclipse.core.runtime.IProgressMonitor;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com._1c.g5.v8.dt.metadata.mdclass.EventSubscription;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckDefinition;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.md.CorePlugin;

/** Residual server-flag validation; native EDT validates missing and private handlers. */
public class MissingEventSubscriptionHandlerCheck extends BasicCheck
{
    public static final String CHECK_ID = "missing-event-subscription-handler"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.MissingEventSubscriptionHandlerCheck_title)
            .description(Messages.MissingEventSubscriptionHandlerCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .topObject(EVENT_SUBSCRIPTION)
            .checkTop()
            .features(EVENT_SUBSCRIPTION__HANDLER);
    }

    @Override
    protected void postConfigureContextCollector(ICheckDefinition definition)
    {
        definition.addGenericModelFeatureChangeContextCollector((object, feature, event, session) -> {
            // File synchronization updates derived contextDef without emitting the persisted server feature.
            if ((feature != COMMON_MODULE__SERVER && feature != COMMON_MODULE__CONTEXT_DEF)
                || !(object instanceof CommonModule))
            {
                return;
            }
            String changedFqn = object.bmGetFqn();
            var subscriptions = object.bmGetTransaction().getTopObjectIterator(EVENT_SUBSCRIPTION);
            while (subscriptions.hasNext())
            {
                var subscription = (EventSubscription)subscriptions.next();
                if (changedFqn.equalsIgnoreCase(moduleFqn(subscription.getHandler())))
                {
                    session.addModelCheck((IBmObject)subscription);
                }
            }
        }, COMMON_MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        var subscription = (EventSubscription)object;
        if (monitor.isCanceled())
        {
            return;
        }
        String moduleFqn = moduleFqn(subscription.getHandler());
        if (moduleFqn == null)
        {
            return;
        }
        // The metadata handler is a qualified object reference, not a lexical BSL identifier.
        var transaction = ((IBmObject)subscription).bmGetTransaction();
        var modules = transaction.getTopObjectsByFqnIgnoreCase(moduleFqn);
        CommonModule provider = null;
        while (modules.hasNext())
        {
            if (!(modules.next() instanceof CommonModule module) || provider != null)
            {
                return;
            }
            provider = module;
        }
        if (provider != null && !provider.isServer())
        {
            acceptor.addIssue(Messages.MissingEventSubscriptionHandlerCheck_message, EVENT_SUBSCRIPTION__HANDLER);
        }
    }

    private static String moduleFqn(String handler)
    {
        if (handler == null)
        {
            return null;
        }
        String[] parts = handler.split("\\.", -1); //$NON-NLS-1$
        if (parts.length != 3 || parts[1].isBlank() || parts[2].isBlank()
            || !("CommonModule".equalsIgnoreCase(parts[0]) || "ОбщийМодуль".equalsIgnoreCase(parts[0]))) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return null;
        }
        return "CommonModule." + parts[1]; //$NON-NLS-1$
    }
}
