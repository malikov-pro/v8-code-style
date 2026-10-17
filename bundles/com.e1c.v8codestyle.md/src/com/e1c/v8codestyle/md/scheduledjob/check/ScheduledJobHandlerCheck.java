/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.md.scheduledjob.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.COMMON_MODULE;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.COMMON_MODULE__CONTEXT_DEF;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.COMMON_MODULE__SERVER;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.SCHEDULED_JOB;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.SCHEDULED_JOB__METHOD_NAME;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.SCHEDULED_JOB__PREDEFINED;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.SCHEDULED_JOB__SCHEDULE;

import java.util.ArrayList;
import java.util.Comparator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.util.EcoreUtil;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.core.event.BmChangeEvent;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com._1c.g5.v8.dt.metadata.mdclass.ScheduledJob;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckDefinition;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.md.CorePlugin;

/** Residual scheduled-job contracts not covered by native missing/export validation. */
public class ScheduledJobHandlerCheck extends BasicCheck
{
    public static final String CHECK_ID = "scheduled-job-handler"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.ScheduledJobHandlerCheck_title)
            .description(Messages.ScheduledJobHandlerCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .topObject(SCHEDULED_JOB).checkTop()
            .features(SCHEDULED_JOB__METHOD_NAME, SCHEDULED_JOB__PREDEFINED);
    }

    @Override
    protected void postConfigureContextCollector(ICheckDefinition definition)
    {
        definition.addGenericModelFeatureChangeContextCollector((object, feature, event, session) -> {
            if ((feature != SCHEDULED_JOB__METHOD_NAME && feature != SCHEDULED_JOB__SCHEDULE)
                || !(object instanceof ScheduledJob changed))
            {
                return;
            }
            String oldHandler = event instanceof BmChangeEvent change
                && change.getOldValues().get(SCHEDULED_JOB__METHOD_NAME) instanceof String value ? value : null;
            var jobs = object.bmGetTransaction().getTopObjectIterator(SCHEDULED_JOB);
            while (jobs.hasNext())
            {
                var job = (ScheduledJob)jobs.next();
                // File synchronization only reports derived schedule, without the previous handler.
                // Recompute the job subset rather than caching potentially stale group membership.
                if (feature == SCHEDULED_JOB__SCHEDULE || sameHandler(job.getMethodName(), changed.getMethodName())
                    || sameHandler(job.getMethodName(), oldHandler))
                {
                    session.addModelCheck((IBmObject)job);
                }
            }
        }, SCHEDULED_JOB);
        definition.addGenericModelFeatureChangeContextCollector((object, feature, event, session) -> {
            if ((feature != COMMON_MODULE__SERVER && feature != COMMON_MODULE__CONTEXT_DEF)
                || !(object instanceof CommonModule))
            {
                return;
            }
            var jobs = object.bmGetTransaction().getTopObjectIterator(SCHEDULED_JOB);
            while (jobs.hasNext())
            {
                var job = (ScheduledJob)jobs.next();
                var parts = handlerParts(job.getMethodName());
                if (parts != null && object.bmGetFqn().equalsIgnoreCase("CommonModule." + parts[1])) //$NON-NLS-1$
                {
                    session.addModelCheck((IBmObject)job);
                }
            }
        }, COMMON_MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        var job = (ScheduledJob)object;
        if (monitor.isCanceled())
        {
            return;
        }
        var parts = handlerParts(job.getMethodName());
        if (parts == null)
        {
            return;
        }
        var transaction = ((IBmObject)job).bmGetTransaction();
        var modules = transaction.getTopObjectsByFqnIgnoreCase("CommonModule." + parts[1]); //$NON-NLS-1$
        CommonModule provider = null;
        while (modules.hasNext())
        {
            if (!(modules.next() instanceof CommonModule module) || provider != null)
            {
                return;
            }
            provider = module;
        }
        if (provider == null)
        {
            return;
        }
        if (!provider.isServer())
        {
            acceptor.addIssue(Messages.ScheduledJobHandlerCheck_server, SCHEDULED_JOB__METHOD_NAME);
            return;
        }
        // LS groups duplicates after resolving a server module, even if the method is missing.
        var duplicates = new ArrayList<ScheduledJob>();
        var jobs = transaction.getTopObjectIterator(SCHEDULED_JOB);
        while (jobs.hasNext())
        {
            var other = (ScheduledJob)jobs.next();
            var otherParts = handlerParts(other.getMethodName());
            if (otherParts != null && parts[1].equalsIgnoreCase(otherParts[1]) && parts[2].equals(otherParts[2]))
            {
                duplicates.add(other);
            }
        }
        duplicates.sort(Comparator.comparing(ScheduledJob::getName));
        if (duplicates.size() > 1 && duplicates.get(0) == job)
        {
            acceptor.addIssue(Messages.ScheduledJobHandlerCheck_duplicate, SCHEDULED_JOB__METHOD_NAME);
        }
        if (!job.isPredefined())
        {
            return;
        }
        Module source = provider.getModule();
        if (source != null && source.eIsProxy())
        {
            var resolved = transaction.getExternalObjectByUri(EcoreUtil.getURI(source));
            source = resolved instanceof Module module ? module : null;
        }
        if (source != null && !source.eIsProxy())
        {
            Method target = null;
            for (var method : source.allMethods())
            {
                if (parts[2].equalsIgnoreCase(method.getName()))
                {
                    if (target != null)
                    {
                        return;
                    }
                    target = method;
                }
            }
            if (target != null && !target.getFormalParams().isEmpty())
            {
                acceptor.addIssue(Messages.ScheduledJobHandlerCheck_parameters, SCHEDULED_JOB__METHOD_NAME);
            }
        }
    }

    private static String[] handlerParts(String handler)
    {
        if (handler == null)
        {
            return null;
        }
        var parts = handler.split("\\.", -1); //$NON-NLS-1$
        if (parts.length != 3 || parts[1].isBlank() || parts[2].isBlank()
            || !("CommonModule".equalsIgnoreCase(parts[0]) || "ОбщийМодуль".equalsIgnoreCase(parts[0]))) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return null;
        }
        return parts;
    }

    private static boolean sameHandler(String first, String second)
    {
        var left = handlerParts(first);
        var right = handlerParts(second);
        return left != null && right != null && left[1].equalsIgnoreCase(right[1]) && left[2].equals(right[2]);
    }
}
