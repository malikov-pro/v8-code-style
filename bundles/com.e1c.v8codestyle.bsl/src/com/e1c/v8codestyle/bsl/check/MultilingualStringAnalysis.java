/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Statement;

/** Pinned LS lexical language extraction and template classification, not data flow. */
final class MultilingualStringAnalysis
{
    private static final Pattern LANG = Pattern.compile("\\w+\\s*=\\s*['|\"{2}]", //$NON-NLS-1$
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern CUT = Pattern.compile("\\s*=\\s*['|\"{2}]", //$NON-NLS-1$
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private MultilingualStringAnalysis()
    {
    }

    static List<String> missing(Invocation call, String declared)
    {
        if (!global(call, "НСтр", "NStr")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return List.of();
        }
        String text = call.getParams().isEmpty() ? "" : tokens(call.getParams().get(0)); //$NON-NLS-1$
        if (!text.isEmpty() && !text.startsWith("\"")) //$NON-NLS-1$
        {
            return List.of();
        }
        Set<String> found = new HashSet<>();
        var matcher = LANG.matcher(text);
        while (matcher.find())
        {
            found.add(CUT.matcher(matcher.group()).replaceAll("")); //$NON-NLS-1$
        }
        // Intentionally retain duplicates, empty entries and case, as the pinned analyser does.
        var expected = Arrays.asList(declared.replaceAll("\\s", "").split(",")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        List<String> missing = new ArrayList<>();
        for (String language : expected)
        {
            if (!found.contains(language))
            {
                missing.add(language);
            }
        }
        return missing;
    }

    static boolean template(Invocation call)
    {
        for (EObject parent = call.eContainer(); parent != null && !(parent instanceof Statement);
            parent = parent.eContainer())
        {
            if (parent instanceof Invocation invocation && global(invocation, "СтрШаблон", "StrTemplate")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                return true;
            }
        }
        SimpleStatement assignment = EcoreUtil2.getContainerOfType(call, SimpleStatement.class);
        if (assignment == null || assignment.getRight() == null)
        {
            return false;
        }
        String variable = tokens(assignment.getLeft());
        var block = assignment.eContainer();
        INode node = NodeModelUtils.getNode(call);
        if (block == null || node == null)
        {
            return false;
        }
        // Try and Except (and other statement lists) can share an EDT container,
        // but are distinct LS codeBlocks. Keep the assignment's containment feature.
        for (EObject statement : block.eContents())
        {
            if (statement.eContainingFeature() != assignment.eContainingFeature())
            {
                continue;
            }
            for (Invocation later : EcoreUtil2.getAllContentsOfType(statement, Invocation.class))
            {
                var laterNode = NodeModelUtils.getNode(later);
                if (global(later, "СтрШаблон", "StrTemplate") && laterNode != null //$NON-NLS-1$ //$NON-NLS-2$
                    && laterNode.getStartLine() > node.getStartLine()
                    && later.getParams().stream().anyMatch(p -> variable.equalsIgnoreCase(tokens(p))))
                {
                    return true;
                }
            }
        }
        return false;
    }

    static String tokens(EObject object)
    {
        // Unassigned grammar calls carry parentheses absent from the semantic node.
        INode node = NodeModelUtils.findActualNodeFor(object);
        if (node == null)
        {
            return ""; //$NON-NLS-1$
        }
        StringBuilder result = new StringBuilder();
        for (var leaf : node.getLeafNodes())
        {
            if (!leaf.isHidden())
            {
                result.append(leaf.getText());
            }
        }
        return result.toString();
    }

    private static boolean global(Invocation call, String ru, String en)
    {
        return call.getMethodAccess() instanceof StaticFeatureAccess access
            && (ru.equalsIgnoreCase(access.getName()) || en.equalsIgnoreCase(access.getName()));
    }
}
