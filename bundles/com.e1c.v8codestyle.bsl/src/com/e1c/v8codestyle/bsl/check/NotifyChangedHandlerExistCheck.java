/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the upstream issue #781 (standard 467)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.STRING_LITERAL__LINES;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.StringLiteral;
import com._1c.g5.v8.dt.common.StringUtils;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;
import com.google.inject.Inject;

/**
 * Checks that the notification handler name passed as the first parameter
 * of the {@code NotifyChanged} ({@code ОповеститьОбИзменении}) call
 * resolves to an existing export procedure or function of a server common
 * module of the configuration. The platform calls such a handler on the
 * server; a name that resolves to nothing means a lost notification —
 * a typo that is silently ignored at runtime.
 * <p>
 * The handler name may be simple ({@code ProcedureName}) or qualified
 * ({@code ModuleName.ProcedureName}). Only a string literal in the first
 * parameter is checked: a variable or an expression is not resolved.
 * The check is not applied to modules adopted in extensions, where the
 * handler may be located in the parent configuration.
 *
 * @author malikov-pro
 */
public class NotifyChangedHandlerExistCheck
    extends BasicCheck
{

    /** The check id of the upstream issue #781 port. */
    public static final String CHECK_ID = "up-781-notify-handler-nonexistent"; //$NON-NLS-1$

    private static final String NOTIFY_CHANGED_NAME = "NotifyChanged"; //$NON-NLS-1$

    private static final String NOTIFY_CHANGED_NAME_RU = "ОповеститьОбИзменении"; //$NON-NLS-1$

    private static final char DOT = '.';

    private final IV8ProjectManager v8ProjectManager;

    /**
     * Instantiates a new notify changed handler exist check.
     *
     * @param v8ProjectManager the V8 project manager service, cannot be {@code null}
     */
    @Inject
    public NotifyChangedHandlerExistCheck(IV8ProjectManager v8ProjectManager)
    {
        super();
        this.v8ProjectManager = v8ProjectManager;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.NotifyChangedHandlerExistCheck_title)
            .description(Messages.NotifyChangedHandlerExistCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .extension(new SkipAdoptedInExtensionModuleOwnerExtension())
            .module()
            .checkedObjectType(INVOCATION);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        Invocation inv = (Invocation)object;
        if (inv.getParams().isEmpty() || inv.getMethodAccess() == null
            || !(NOTIFY_CHANGED_NAME_RU.equalsIgnoreCase(inv.getMethodAccess().getName())
                || NOTIFY_CHANGED_NAME.equalsIgnoreCase(inv.getMethodAccess().getName())))
        {
            return;
        }

        Expression first = inv.getParams().get(0);
        // A dynamic name (a variable or an expression) is not resolved.
        if (!(first instanceof StringLiteral literal) || literal.getLines().size() != 1)
        {
            return;
        }
        String handlerName = literal.lines(true).get(0);
        if (monitor.isCanceled() || StringUtils.isBlank(handlerName))
        {
            return;
        }

        Configuration configuration = OverridableModuleUtil.getConfiguration(v8ProjectManager, inv);
        if (configuration == null)
        {
            return;
        }

        if (!existsExportMethod(configuration, handlerName, inv))
        {
            resultAceptor.addIssue(MessageFormat.format(
                Messages.NotifyChangedHandlerExistCheck_Handler_does_not_exist, handlerName), first,
                STRING_LITERAL__LINES);
        }
    }

    /**
     * Checks that the handler name resolves to an export method of a server
     * common module of the configuration: a simple name is searched in all
     * server common modules, a qualified name
     * {@code ModuleName.MethodName} — in the named common module. The
     * module being edited is searched as its actual in-memory object: the
     * module reference of its owner may be outdated during the editing.
     *
     * @param configuration the configuration, cannot be {@code null}
     * @param handlerName the handler name from the string literal, cannot be blank
     * @param context the invocation object, cannot be {@code null}
     * @return {@code true} if the handler exists
     */
    private static boolean existsExportMethod(Configuration configuration, String handlerName, Invocation context)
    {
        int dot = handlerName.indexOf(DOT);
        String moduleName = null;
        String methodName = handlerName;
        if (dot > 0)
        {
            moduleName = handlerName.substring(0, dot).strip();
            methodName = handlerName.substring(dot + 1).strip();
        }
        Module editedModule = EcoreUtil2.getContainerOfType(context, Module.class);
        String editedModuleOwner = OverridableModuleUtil.getOwnerName(editedModule);
        for (CommonModule commonModule : configuration.getCommonModules())
        {
            String name = commonModule.getName();
            if (name == null || !commonModule.isServer())
            {
                continue;
            }
            if (moduleName != null && !name.equalsIgnoreCase(moduleName))
            {
                continue;
            }
            Module module = name.equalsIgnoreCase(editedModuleOwner)
                ? editedModule
                : OverridableModuleUtil.getModuleOf(commonModule);
            if (isExportMethod(module, methodName))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isExportMethod(Module module, String methodName)
    {
        Method method = OverridableModuleUtil.findMethodInModule(module, methodName);
        return method != null && method.isExport();
    }

}
