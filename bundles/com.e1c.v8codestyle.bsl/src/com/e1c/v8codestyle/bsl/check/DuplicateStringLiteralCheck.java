/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.StringLiteral;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Groups raw string literals per method/file block or across the whole file. */
public class DuplicateStringLiteralCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "duplicate-string-literal"; //$NON-NLS-1$
    public static final String PARAM_COPIES = "allowedNumberCopies"; //$NON-NLS-1$
    public static final String PARAM_FILE = "analyzeFile"; //$NON-NLS-1$
    public static final String PARAM_CASE = "caseSensitive"; //$NON-NLS-1$
    public static final String PARAM_LENGTH = "minTextLength"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.DuplicateStringLiteralCheck_title)
            .description(Messages.DuplicateStringLiteralCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE)
            .parameter(PARAM_COPIES, Integer.class, "2", Messages.DuplicateStringLiteralCheck_copies) //$NON-NLS-1$
            .parameter(PARAM_FILE, Boolean.class, "false", Messages.DuplicateStringLiteralCheck_file) //$NON-NLS-1$
            .parameter(PARAM_CASE, Boolean.class, "false", Messages.DuplicateStringLiteralCheck_case) //$NON-NLS-1$
            .parameter(PARAM_LENGTH, Integer.class, "5", Messages.DuplicateStringLiteralCheck_length); //$NON-NLS-1$
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Module module = (Module)object;
        int allowed = parameters.getInt(PARAM_COPIES);
        if (allowed < 1)
        {
            allowed = 2;
        }
        int minLength = Math.max(5, parameters.getInt(PARAM_LENGTH));
        boolean analyzeFile = parameters.getBoolean(PARAM_FILE);
        boolean caseSensitive = parameters.getBoolean(PARAM_CASE);
        Map<Object, Map<String, List<StringLiteral>>> scopes = new LinkedHashMap<>();
        List<Method> methods = EcoreUtil2.getAllContentsOfType(module, Method.class);
        // EMF containment order may visit parameter defaults after the body;
        // the diagnostic must anchor the first literal in source order.
        for (StringLiteral literal : EcoreUtil2.getAllContentsOfType(module, StringLiteral.class).stream()
            .sorted(Comparator.comparingInt(item -> {
                INode node = NodeModelUtils.getNode(item);
                return node == null ? Integer.MAX_VALUE : node.getOffset();
            })).toList())
        {
            if (monitor.isCanceled())
            {
                return;
            }
            INode node = NodeModelUtils.getNode(literal);
            if (node == null)
            {
                continue;
            }
            String text = rawText(node);
            if (text.length() < minLength)
            {
                continue;
            }
            Object scope = module;
            if (!analyzeFile)
            {
                Method method = EcoreUtil2.getContainerOfType(literal, Method.class);
                if (method != null)
                {
                    scope = method;
                }
                else
                {
                    // LS checks file-code blocks before/after methods separately.
                    int offset = node.getOffset();
                    scope = Long.valueOf(methods.stream().filter(item -> {
                        INode methodNode = NodeModelUtils.getNode(item);
                        return methodNode != null && methodNode.getEndOffset() <= offset;
                    }).count());
                }
            }
            String key = caseSensitive ? text : text.toLowerCase(Locale.ROOT);
            scopes.computeIfAbsent(scope, unused -> new LinkedHashMap<>())
                .computeIfAbsent(key, unused -> new java.util.ArrayList<>()).add(literal);
        }
        for (Map<String, List<StringLiteral>> groups : scopes.values())
        {
            for (List<StringLiteral> group : groups.values())
            {
                if (group.size() <= allowed)
                {
                    continue;
                }
                INode node = NodeModelUtils.getNode(group.get(0));
                ILeafNode first = null;
                ILeafNode last = null;
                for (ILeafNode leaf : node.getLeafNodes())
                {
                    if (!leaf.isHidden())
                    {
                        if (first == null)
                        {
                            first = leaf;
                        }
                        last = leaf;
                    }
                }
                if (first != null && last != null)
                {
                    acceptor.addIssue(new BslDirectLocationIssue(
                        MessageFormat.format(Messages.DuplicateStringLiteralCheck_message, rawText(node)),
                        new DirectLocation(first.getOffset(), last.getEndOffset() - first.getOffset(),
                            first.getStartLine(), module)));
                }
            }
        }
    }

    private static String rawText(INode node)
    {
        StringBuilder text = new StringBuilder();
        for (ILeafNode leaf : node.getLeafNodes())
        {
            if (!leaf.isHidden())
            {
                text.append(leaf.getText());
            }
        }
        return text.toString();
    }
}
