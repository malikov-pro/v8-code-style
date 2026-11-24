/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FEATURE_ACCESS__NAME;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.EmptyExpression;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.OperatorStyleCreator;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.WrongParameterException;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Reports initialization of method/constructor parameters by nested calls: a multi-line call whose actual parameters
 * contain another method call, a constructor with arguments, or a global function that is not whitelisted.
 * <p>
 * Port of the BSL LS {@code NestedFunctionInParameters} diagnostic, LS contract reproduced:
 * <ul>
 * <li>a call expression that fits on one line is always skipped (unconditional oneliner skip);
 * <li>a call without actual parameters is skipped;
 * <li>the diagnostic is raised when the parameters contain a forbidden nested call: any call with a receiver
 * ({@code Объект.Метод(...)}), a constructor with at least one non-empty argument, or a global function whose name is
 * not in {@code allowedMethodNames} (compared case-insensitively, the whole name as the LS matches it);
 * <li>and when at least one actual parameter itself spans more than one line. With {@code allowOneliner=false} the
 * last condition is not required, only the "whole call on one line" skip remains;
 * <li>constructors declared as {@code Новый Тип(...)} are checked as well; the marker is placed on the method name or on
 * the type name, exactly as the LS marks the method/type name.
 * </ul>
 * Parameters are read from the check settings: {@code allowOneliner} (default {@code true}) and
 * {@code allowedMethodNames} (default {@code НСтр,NStr,ПредопределенноеЗначение,PredefinedValue}).
 * <p>
 * <b>Partial scope (v1):</b> a constructor with the type given by an expression, {@code Новый(Тип("..."), ...)}, is not
 * analyzed, because the EDT model keeps it as a {@code FunctionStyleCreator} without a typed argument list; and the
 * type name of a constructor is taken from the written token, so an aliased type is reported as written.
 *
 * @author malikov-pro
 */
public final class NestedFunctionInParametersCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "nested-function-in-parameters"; //$NON-NLS-1$

    private static final String PARAM_ALLOW_ONELINER = "allowOneliner"; //$NON-NLS-1$

    private static final String PARAM_ALLOWED_METHOD_NAMES = "allowedMethodNames"; //$NON-NLS-1$

    private static final boolean DEFAULT_ALLOW_ONELINER = true;

    private static final String DEFAULT_ALLOWED_METHOD_NAMES =
        "НСтр,NStr,ПредопределенноеЗначение,PredefinedValue"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.NestedFunctionInParametersCheck_title)
            .description(Messages.NestedFunctionInParametersCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE)
            .parameter(PARAM_ALLOW_ONELINER, Boolean.class, Boolean.toString(DEFAULT_ALLOW_ONELINER),
                Messages.NestedFunctionInParametersCheck_allowOneliner)
            .parameter(PARAM_ALLOWED_METHOD_NAMES, String.class, DEFAULT_ALLOWED_METHOD_NAMES,
                Messages.NestedFunctionInParametersCheck_allowedMethodNames);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Module module = (Module)object;
        boolean allowOneliner = booleanParameter(parameters, PARAM_ALLOW_ONELINER, DEFAULT_ALLOW_ONELINER);
        Set<String> allowedMethods = allowedMethods(parameters);
        for (Invocation call : EcoreUtil2.getAllContentsOfType(module, Invocation.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            checkCall(call, allowOneliner, allowedMethods, module, acceptor);
        }
        for (OperatorStyleCreator constructor : EcoreUtil2.getAllContentsOfType(module, OperatorStyleCreator.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            checkConstructor(constructor, allowOneliner, allowedMethods, module, acceptor);
        }
    }

    private void checkCall(Invocation call, boolean allowOneliner, Set<String> allowedMethods, Module module,
        ResultAcceptor acceptor)
    {
        List<Expression> params = call.getParams();
        if (params.isEmpty() || !isMultiLine(call) || !multilineParameter(params, allowOneliner)
            || !containsForbidden(params, allowedMethods))
        {
            return;
        }
        FeatureAccess access = call.getMethodAccess();
        String name = access == null ? null : access.getName();
        INode node = nameNode(access);
        if (node == null)
        {
            node = NodeModelUtils.findActualNodeFor(call);
        }
        addIssue(module, node, Messages.NestedFunctionInParametersCheck_message,
            Messages.NestedFunctionInParametersCheck_method, name, acceptor);
    }

    private void checkConstructor(OperatorStyleCreator constructor, boolean allowOneliner, Set<String> allowedMethods,
        Module module, ResultAcceptor acceptor)
    {
        List<Expression> params = constructor.getParams();
        if (!hasArguments(params) || !isMultiLine(constructor) || !multilineParameter(params, allowOneliner)
            || !containsForbidden(params, allowedMethods))
        {
            return;
        }
        String name = AuditConstructorName.of(constructor);
        INode node = constructorNameNode(constructor);
        if (node == null)
        {
            node = NodeModelUtils.findActualNodeFor(constructor);
        }
        if (name == null || name.isBlank())
        {
            addIssue(module, node, Messages.NestedFunctionInParametersCheck_message_without_name,
                Messages.NestedFunctionInParametersCheck_constructor, null, acceptor);
        }
        else
        {
            addIssue(module, node, Messages.NestedFunctionInParametersCheck_message,
                Messages.NestedFunctionInParametersCheck_constructor, name, acceptor);
        }
    }

    private static void addIssue(Module module, INode node, String template, String kind, String name,
        ResultAcceptor acceptor)
    {
        if (node == null)
        {
            return;
        }
        String message = name == null ? MessageFormat.format(template, kind)
            : MessageFormat.format(template, kind, name);
        acceptor.addIssue(new BslDirectLocationIssue(message,
            new DirectLocation(node.getOffset(), node.getLength(), node.getStartLine(), module)));
    }

    /**
     * The LS "one line" test of the whole call: a call written on a single line is never reported.
     */
    private static boolean isMultiLine(EObject call)
    {
        INode node = NodeModelUtils.findActualNodeFor(call);
        return node != null && node.getStartLine() != node.getEndLine();
    }

    /**
     * The LS multiline-parameter test: with {@code allowOneliner} at least one actual parameter must span lines,
     * otherwise the flag itself allows reporting any nested call.
     */
    private static boolean multilineParameter(List<Expression> params, boolean allowOneliner)
    {
        if (!allowOneliner)
        {
            return true;
        }
        for (Expression param : params)
        {
            INode node = NodeModelUtils.findActualNodeFor(param);
            if (node != null && node.getStartLine() != node.getEndLine())
            {
                return true;
            }
        }
        return false;
    }

    private static boolean containsForbidden(List<Expression> params, Set<String> allowedMethods)
    {
        for (Expression param : params)
        {
            if (isForbidden(param, allowedMethods))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Walks the expression including its root: a call with a receiver is forbidden, a constructor with non-empty
     * arguments is forbidden, a global function is forbidden unless it is whitelisted. The children of a whitelisted
     * call are still inspected, as the LS does.
     */
    private static boolean isForbidden(EObject object, Set<String> allowedMethods)
    {
        if (object instanceof Invocation call)
        {
            if (!(call.getMethodAccess() instanceof StaticFeatureAccess access)
                || !allowedMethods.contains(normalize(access.getName())))
            {
                return true;
            }
        }
        else if (object instanceof OperatorStyleCreator constructor && hasArguments(constructor.getParams()))
        {
            return true;
        }
        for (EObject child : object.eContents())
        {
            if (isForbidden(child, allowedMethods))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean hasArguments(List<Expression> params)
    {
        return params.stream().anyMatch(param -> !(param instanceof EmptyExpression));
    }

    private static Set<String> allowedMethods(ICheckParameters parameters)
    {
        String value = stringParameter(parameters, PARAM_ALLOWED_METHOD_NAMES, DEFAULT_ALLOWED_METHOD_NAMES);
        Set<String> names = new LinkedHashSet<>();
        for (String name : Arrays.asList(value.split(","))) //$NON-NLS-1$
        {
            String trimmed = name.trim();
            if (!trimmed.isEmpty())
            {
                names.add(normalize(trimmed));
            }
        }
        return names;
    }

    private static String normalize(String name)
    {
        return name == null ? "" : name.toLowerCase(Locale.ROOT); //$NON-NLS-1$
    }

    private static INode nameNode(FeatureAccess access)
    {
        if (access == null)
        {
            return null;
        }
        List<INode> nodes = NodeModelUtils.findNodesForFeature(access, FEATURE_ACCESS__NAME);
        return nodes.isEmpty() ? null : nodes.get(0);
    }

    /**
     * The node of the written constructor type name: the first significant token is {@code Новый}/{@code New}, the
     * second one is the type name unless the parentheses follow immediately.
     */
    private static INode constructorNameNode(OperatorStyleCreator constructor)
    {
        INode node = NodeModelUtils.findActualNodeFor(constructor);
        if (node == null)
        {
            return null;
        }
        ILeafNode newKeyword = null;
        for (ILeafNode leaf : node.getLeafNodes())
        {
            if (leaf.isHidden())
            {
                continue;
            }
            if (newKeyword == null)
            {
                newKeyword = leaf;
                continue;
            }
            String text = leaf.getText();
            return "(".equals(text) ? newKeyword : leaf; //$NON-NLS-1$
        }
        return newKeyword;
    }

    private static boolean booleanParameter(ICheckParameters parameters, String name, boolean defaultValue)
    {
        try
        {
            return parameters.getBoolean(name);
        }
        catch (WrongParameterException e)
        {
            return defaultValue;
        }
    }

    private static String stringParameter(ICheckParameters parameters, String name, String defaultValue)
    {
        String value;
        try
        {
            value = parameters.getString(name);
        }
        catch (WrongParameterException e)
        {
            return defaultValue;
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
