/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.NAMED_ELEMENT__NAME;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.resource.BslEventsService;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormPackage;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckDefinition;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;
import com.google.inject.Inject;

/** Validates parameter counts against a bound platform event contract. */
public class EventHandlerInvalidSignatureCheck extends BasicCheck
{
    public static final String CHECK_ID = "event-handler-invalid-signature"; //$NON-NLS-1$
    private final BslEventsService events;

    @Inject
    public EventHandlerInvalidSignatureCheck(BslEventsService events)
    {
        this.events = events;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.EventHandlerInvalidSignatureCheck_title)
            .description(Messages.EventHandlerInvalidSignatureCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(METHOD);
    }

    @Override
    protected void postConfigureContextCollector(ICheckDefinition definition)
    {
        definition.addGenericModelFeatureChangeContextCollector((object, feature, event, session) -> {
            if (feature == MdClassPackage.Literals.ABSTRACT_FORM__FORM_EVENTS
                && object instanceof Form form && form.getModule() != null)
            {
                var uri = EcoreUtil.getURI(form.getModule()).trimFragment();
                if (uri.isPlatformResource())
                {
                    session.addLanguageCheck(uri.toPlatformString(true));
                }
            }
        }, FormPackage.Literals.FORM);
        definition.addGenericModelFeatureChangeContextCollector((object, feature, event, session) -> {
            if (feature == FormPackage.Literals.EVENT_HANDLER__NAME
                || feature == FormPackage.Literals.EVENT_HANDLER__EVENT)
            {
                var form = EcoreUtil2.getContainerOfType(object, Form.class);
                if (form != null && form.getModule() != null)
                {
                    var uri = EcoreUtil.getURI(form.getModule()).trimFragment();
                    if (uri.isPlatformResource())
                    {
                        session.addLanguageCheck(uri.toPlatformString(true));
                    }
                }
            }
        }, FormPackage.Literals.EVENT_HANDLER, FormPackage.Literals.FORM);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Method method = (Method)object;
        if (monitor.isCanceled())
        {
            return;
        }
        Module module = EcoreUtil2.getContainerOfType(method, Module.class);
        if (module == null || !(module.getOwner() instanceof Form))
        {
            return;
        }
        for (var entry : events.getAllModuleEvents(module).entrySet())
        {
            if (!method.getName().equalsIgnoreCase(entry.getValue().getString()) || entry.getKey().eIsProxy()
                || entry.getKey().getParamSet().isEmpty())
            {
                continue;
            }
            int count = method.getFormalParams().size();
            boolean accepted = entry.getKey().getParamSet().stream()
                .anyMatch(set -> count >= set.getMinParams()
                    && (set.getMaxParams() == -1 || count <= set.getMaxParams()));
            if (!accepted)
            {
                acceptor.addIssue(Messages.EventHandlerInvalidSignatureCheck_message, method, NAMED_ELEMENT__NAME);
                return;
            }
        }
    }
}
