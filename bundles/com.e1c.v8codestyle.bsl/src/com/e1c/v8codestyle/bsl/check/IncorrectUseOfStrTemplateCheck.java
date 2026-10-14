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
 *     malikov-pro - initial API and implementation
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.StringLiteral;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Checks known StrTemplate templates against supplied substitution arguments. */
public class IncorrectUseOfStrTemplateCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "incorrect-use-of-str-template"; //$NON-NLS-1$

    private static final Pattern INDEX = Pattern.compile("(?<!%)%(?:(10|[1-9])(?!\\d)|\\((10|[1-9])\\))"); //$NON-NLS-1$
    private static final Pattern INVALID_INDEX = Pattern.compile(
        "(?<!%)%(?:(1[1-9]\\d*|[2-9]\\d+|0|10\\d+)(?!\\d)|\\((1[1-9]\\d*|[2-9]\\d+|0|10\\d+)\\))"); //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.IncorrectUseOfStrTemplateCheck_title)
            .description(Messages.IncorrectUseOfStrTemplateCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAcceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        Module module = (Module)object;
        for (Invocation call : EcoreUtil2.getAllContentsOfType(module, Invocation.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (!isGlobalCall(call, "СтрШаблон", "StrTemplate") || call.getParams().isEmpty()) //$NON-NLS-1$ //$NON-NLS-2$
            {
                continue;
            }
            String template = template(call.getParams().get(0), call);
            if (template == null || template.isEmpty())
            {
                continue;
            }
            int arguments = call.getParams().size() - 1;
            if (isWrongTemplate(template, arguments)
                && isWrongTemplate(template.replace("%%", ""), arguments)) //$NON-NLS-1$ //$NON-NLS-2$
            {
                INode node = NodeModelUtils.getNode(call);
                if (node != null && !hasSyntaxErrors(node))
                {
                    resultAcceptor.addIssue(new BslDirectLocationIssue(Messages.IncorrectUseOfStrTemplateCheck_message,
                        new DirectLocation(node.getOffset(), node.getLength(), node.getStartLine(), module)));
                }
            }
        }
    }

    private static boolean isWrongTemplate(String template, int arguments)
    {
        if (INVALID_INDEX.matcher(template).find())
        {
            return true;
        }
        Set<Integer> used = new HashSet<>();
        Matcher matcher = INDEX.matcher(template);
        while (matcher.find())
        {
            int index = Integer.parseInt(matcher.group(1) == null ? matcher.group(2) : matcher.group(1));
            if (index > arguments)
            {
                return true;
            }
            used.add(index);
        }
        for (int i = 1; i <= arguments; i++)
        {
            if (!used.contains(i))
            {
                return true;
            }
        }
        return false;
    }

    private static String template(Expression expression, Invocation call)
    {
        if (expression instanceof StringLiteral literal)
        {
            return String.join("", literal.lines(true)); //$NON-NLS-1$
        }
        if (expression instanceof Invocation nstr && isGlobalCall(nstr, "НСтр", "NStr") //$NON-NLS-1$ //$NON-NLS-2$
            && !nstr.getParams().isEmpty())
        {
            return template(nstr.getParams().get(0), call);
        }
        if (!(expression instanceof StaticFeatureAccess || expression instanceof DynamicFeatureAccess))
        {
            return null;
        }
        Statement current = EcoreUtil2.getContainerOfType(call, Statement.class);
        if (current == null || current.eContainer() == null)
        {
            return null;
        }
        Object siblingsValue = current.eContainer().eGet(current.eContainingFeature());
        if (!(siblingsValue instanceof List<?> siblings))
        {
            return null;
        }
        String name = accessText(expression);
        for (int i = siblings.indexOf(current) - 1; i >= 0; i--)
        {
            if (siblings.get(i) instanceof SimpleStatement assignment
                && assignment.getRight() instanceof StringLiteral literal
                && name.equalsIgnoreCase(accessText(assignment.getLeft())))
            {
                return String.join("", literal.lines(true)); //$NON-NLS-1$
            }
        }
        return null;
    }

    private static String accessText(EObject expression)
    {
        INode node = NodeModelUtils.findActualNodeFor(expression);
        return node == null ? "" : NodeModelUtils.getTokenText(node).replaceAll("\\s+", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    private static boolean isGlobalCall(Invocation call, String ru, String en)
    {
        return call.getMethodAccess() instanceof StaticFeatureAccess access
            && (ru.equalsIgnoreCase(access.getName()) || en.equalsIgnoreCase(access.getName()));
    }

    private static boolean hasSyntaxErrors(INode node)
    {
        for (INode leaf : node.getLeafNodes())
        {
            if (leaf.getSyntaxErrorMessage() != null)
            {
                return true;
            }
        }
        return false;
    }
}
