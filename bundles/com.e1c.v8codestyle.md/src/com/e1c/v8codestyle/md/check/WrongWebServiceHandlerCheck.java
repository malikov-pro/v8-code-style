/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import static com._1c.g5.v8.dt.bsl.model.BslIndexPackage.Literals.MODULE_CONTEXT_DEF_INDEX;
import static com._1c.g5.v8.dt.bsl.model.BslIndexPackage.Literals.MODULE_CONTEXT_DEF_INDEX__CONTEXT_DEF;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.OPERATION__PROCEDURE_NAME;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.WEB_SERVICE;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.WEB_SERVICE__MODULE;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.WEB_SERVICE__OPERATIONS;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.util.EcoreUtil;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleContextDefIndex;
import com._1c.g5.v8.dt.metadata.mdclass.WebService;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckDefinition;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.md.CorePlugin;

/** Checks the residual missing method behind a nonempty WebService operation assignment. */
public class WrongWebServiceHandlerCheck extends BasicCheck
{
    public static final String CHECK_ID = "wrong-web-service-handler"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.WrongWebServiceHandlerCheck_title)
            .description(Messages.WrongWebServiceHandlerCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .topObject(WEB_SERVICE).checkTop()
            .features(WEB_SERVICE__MODULE, WEB_SERVICE__OPERATIONS);
    }

    @Override
    protected void postConfigureContextCollector(ICheckDefinition definition)
    {
        definition.addGenericModelFeatureChangeContextCollector((object, feature, event, session) -> {
            if (feature == MODULE_CONTEXT_DEF_INDEX__CONTEXT_DEF
                && object instanceof ModuleContextDefIndex index && index.getContextDef() != null)
            {
                var uri = index.getContextDef().getModuleUri();
                if (uri != null && uri.isPlatformResource())
                {
                    var source = object.bmGetTransaction().getExternalObjectByUri(uri);
                    if (source instanceof Module module && module.getOwner() instanceof WebService service)
                    {
                        var resolved = EcoreUtil.resolve(service, module);
                        if (!resolved.eIsProxy())
                        {
                            session.addModelCheck((IBmObject)resolved);
                        }
                    }
                }
            }
        }, MODULE_CONTEXT_DEF_INDEX);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        var service = (WebService)object;
        if (monitor.isCanceled())
        {
            return;
        }
        Module source = service.getModule();
        if (source != null && source.eIsProxy())
        {
            var resolved = ((IBmObject)service).bmGetTransaction().getExternalObjectByUri(EcoreUtil.getURI(source));
            source = resolved instanceof Module module ? module : null;
        }
        if (source == null || source.eIsProxy())
        {
            return;
        }
        for (var operation : service.getOperations())
        {
            if (monitor.isCanceled())
            {
                return;
            }
            String handler = operation.getProcedureName();
            if (handler == null || handler.isBlank())
            {
                continue;
            }
            if (source.allMethods().stream().noneMatch(method -> handler.equalsIgnoreCase(method.getName())))
            {
                acceptor.addIssue(Messages.WrongWebServiceHandlerCheck_missing, operation, OPERATION__PROCEDURE_NAME);
            }
        }
    }
}
