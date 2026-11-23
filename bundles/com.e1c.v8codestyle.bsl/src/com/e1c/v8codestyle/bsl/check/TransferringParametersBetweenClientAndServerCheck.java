/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;

import java.text.MessageFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.FeatureEntry;
import com._1c.g5.v8.dt.bsl.model.FormalParam;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.Pragma;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.SourceObjectLinkProvider;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.resource.DynamicFeatureAccessComputer;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.mcore.Environmental;
import com._1c.g5.v8.dt.mcore.util.Environment;
import com._1c.g5.v8.dt.mcore.util.Environments;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;
import com.google.inject.Inject;

/**
 * Reports parameters passed by reference (without the {@code Знач}/{@code ByValue} modifier) from a client method to a
 * server method when the server method never assigns the parameter: the value copy is transferred back from the
 * server to the client for nothing.
 * <p>
 * Port of the BSL LS {@code TransferringParametersBetweenClientAndServer} diagnostic, including the LS behaviour of
 * reporting the parameter in a client-server call pair. Only direct calls with a resolvable target are analyzed: an
 * invocation is resolved either through the {@code DynamicFeatureAccess} features ({@link DynamicFeatureAccessComputer})
 * or, as a fallback for {@code Module.Method(...)}, by the common module name of the access receiver. Calls through a
 * variable and unresolved/indirect calls are skipped silently.
 * <p>
 * <b>Partial scope (v1):</b> assignment detection walks {@link SimpleStatement}s of the target method and compares the
 * name of the plain {@link StaticFeatureAccess} on the left side. Assignments through a property or an index
 * ({@code Parameter.Field = ...}, {@code Parameter[0] = ...}) are not treated as assignments, so a parameter modified
 * only that way is reported. The LS {@code cachedValueNames} parameter is not implemented.
 *
 * @author malikov-pro
 */
public final class TransferringParametersBetweenClientAndServerCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "transferring-parameters-between-client-and-server"; //$NON-NLS-1$

    private static final Environments CLIENT_ENVIRONMENTS = Environments.ALL_CLIENTS.add(Environment.CLIENT);

    private static final Environments SERVER_ENVIRONMENTS = Environments.ALL_SERVERS;

    private final DynamicFeatureAccessComputer dynamicComputer;

    private final IBmModelManager modelManager;

    private final IV8ProjectManager projectManager;

    @Inject
    public TransferringParametersBetweenClientAndServerCheck(DynamicFeatureAccessComputer dynamicComputer,
        IBmModelManager modelManager, IV8ProjectManager projectManager)
    {
        this.dynamicComputer = dynamicComputer;
        this.modelManager = modelManager;
        this.projectManager = projectManager;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.TransferringParametersBetweenClientAndServerCheck_title)
            .description(Messages.TransferringParametersBetweenClientAndServerCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(INVOCATION);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Invocation call = (Invocation)object;
        if (monitor.isCanceled())
        {
            return;
        }
        FeatureAccess access = call.getMethodAccess();
        Module callerModule = EcoreUtil2.getContainerOfType(call, Module.class);
        Method caller = EcoreUtil2.getContainerOfType(call, Method.class);
        if (access == null || callerModule == null || caller == null)
        {
            return;
        }
        Method target = resolveTarget(call, access, callerModule, caller);
        if (target == null || target == caller || !isClientOnly(caller) || !isServerOnly(target))
        {
            return;
        }
        List<FormalParam> formals = target.getFormalParams();
        List<Expression> actuals = call.getParams();
        int count = Math.min(formals.size(), actuals.size());
        for (int i = 0; i < count; i++)
        {
            if (monitor.isCanceled())
            {
                return;
            }
            FormalParam formal = formals.get(i);
            if (formal.isByValue() || hasNoName(formal.getName()) || isAssigned(target, formal.getName()))
            {
                continue;
            }
            Expression actual = actuals.get(i);
            INode node = NodeModelUtils.getNode(actual);
            if (node == null)
            {
                continue;
            }
            String message = MessageFormat.format(
                Messages.TransferringParametersBetweenClientAndServerCheck_message, formal.getName(),
                target.getName());
            acceptor.addIssue(new BslDirectLocationIssue(message,
                new DirectLocation(node.getOffset(), node.getLength(), node.getStartLine(), callerModule)));
        }
    }

    private Method resolveTarget(Invocation call, FeatureAccess access, Module callerModule, Method caller)
    {
        if (access instanceof StaticFeatureAccess)
        {
            // A plain Method(...) call in the same module: the invocation name is the local method name.
            return OverridableModuleUtil.findMethodInModule(callerModule, access.getName());
        }
        if (!(access instanceof DynamicFeatureAccess dynamicAccess))
        {
            return null;
        }
        Environments callerEnvironments = environments(caller);
        Method resolved = methodFromEntries(dynamicComputer.resolveObject(dynamicAccess, callerEnvironments), call);
        if (resolved == null && !callerEnvironments.isEmpty())
        {
            resolved = methodFromEntries(dynamicComputer.resolveObject(dynamicAccess, Environments.ALL), call);
        }
        if (resolved == null)
        {
            resolved = methodFromCommonModuleName(dynamicAccess, call, callerModule);
        }
        return resolved;
    }

    private Method methodFromEntries(List<FeatureEntry> entries, EObject context)
    {
        Set<Method> methods = new LinkedHashSet<>();
        for (FeatureEntry entry : entries)
        {
            EObject feature = resolveFeature(entry.getFeature(), context);
            if (feature instanceof Method method)
            {
                methods.add(method);
            }
        }
        return methods.size() == 1 ? methods.iterator().next() : null;
    }

    @SuppressWarnings("deprecation")
    private EObject resolveFeature(EObject feature, EObject context)
    {
        if (feature == null)
        {
            return null;
        }
        if (feature.eIsProxy())
        {
            feature = EcoreUtil.resolve(feature, context);
        }
        if (feature == null || feature.eIsProxy())
        {
            return null;
        }
        if (feature instanceof SourceObjectLinkProvider link)
        {
            URI sourceUri = link.getSourceUri();
            if (sourceUri != null)
            {
                try
                {
                    EObject external = modelManager.getModel(context).getEngine().getCurrentTransaction()
                        .getExternalObjectByUri(sourceUri);
                    return external == null ? null : external;
                }
                catch (RuntimeException e)
                {
                    return null;
                }
            }
        }
        return feature;
    }

    /**
     * Fallback for a direct {@code Module.Method(...)} call: looks the receiver name up in the configuration common
     * modules and searches the method by name. Calls through a variable have no common module receiver and are
     * skipped.
     */
    private Method methodFromCommonModuleName(DynamicFeatureAccess dynamicAccess, EObject context, Module callerModule)
    {
        if (!(dynamicAccess.getSource() instanceof StaticFeatureAccess receiver))
        {
            return null;
        }
        Configuration configuration = OverridableModuleUtil.getConfiguration(projectManager, context);
        CommonModule commonModule = OverridableModuleUtil.findCommonModule(configuration, receiver.getName());
        if (commonModule == null)
        {
            return null;
        }
        Module targetModule = receiver.getName().equalsIgnoreCase(OverridableModuleUtil.getOwnerName(callerModule))
            ? callerModule
            : OverridableModuleUtil.getModuleOf(commonModule);
        return OverridableModuleUtil.findMethodInModule(targetModule, dynamicAccess.getName());
    }

    private static boolean isAssigned(Method target, String parameterName)
    {
        for (SimpleStatement statement : EcoreUtil2.getAllContentsOfType(target, SimpleStatement.class))
        {
            if (statement.getRight() != null && statement.getLeft() instanceof StaticFeatureAccess left
                && parameterName.equalsIgnoreCase(left.getName()))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isClientOnly(Method method)
    {
        Environments environments = environments(method);
        if (!environments.isEmpty())
        {
            return environments.containsAny(CLIENT_ENVIRONMENTS) && !environments.containsAny(SERVER_ENVIRONMENTS);
        }
        return hasClientPragma(method) && !hasServerPragma(method);
    }

    private static boolean isServerOnly(Method method)
    {
        Environments environments = environments(method);
        if (!environments.isEmpty())
        {
            return environments.containsAny(SERVER_ENVIRONMENTS) && !environments.containsAny(CLIENT_ENVIRONMENTS);
        }
        return hasServerPragma(method) && !hasClientPragma(method);
    }

    private static Environments environments(Method method)
    {
        if (method instanceof Environmental environmental)
        {
            Environments environments = environmental.environments();
            return environments == null ? Environments.EMPTY : environments;
        }
        return Environments.EMPTY;
    }

    private static boolean hasClientPragma(Method method)
    {
        for (Pragma pragma : method.getPragmas())
        {
            String symbol = pragma.getSymbol();
            if (symbol == null)
            {
                continue;
            }
            switch (symbol.toLowerCase(Locale.ROOT))
            {
            case "наклиенте": //$NON-NLS-1$
            case "atclient": //$NON-NLS-1$
                return true;
            case "наклиентенасерверебезконтекста": //$NON-NLS-1$
            case "atclientatservernocontext": //$NON-NLS-1$
                return true;
            default:
                break;
            }
        }
        return false;
    }

    private static boolean hasServerPragma(Method method)
    {
        for (Pragma pragma : method.getPragmas())
        {
            String symbol = pragma.getSymbol();
            if (symbol == null)
            {
                continue;
            }
            switch (symbol.toLowerCase(Locale.ROOT))
            {
            case "насервере": //$NON-NLS-1$
            case "atserver": //$NON-NLS-1$
            case "насерверебезконтекста": //$NON-NLS-1$
            case "atservernocontext": //$NON-NLS-1$
            case "наклиентенасерверебезконтекста": //$NON-NLS-1$
            case "atclientatservernocontext": //$NON-NLS-1$
                return true;
            default:
                break;
            }
        }
        return false;
    }

    private static boolean hasNoName(String name)
    {
        return name == null || name.isBlank();
    }
}
