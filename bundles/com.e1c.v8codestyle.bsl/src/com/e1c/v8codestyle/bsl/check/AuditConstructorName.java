/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

/** Written constructor name with the pinned LS first-string-member semantics. */
final class AuditConstructorName
{
    private AuditConstructorName()
    {
    }

    static String of(EObject creator)
    {
        var node = NodeModelUtils.getNode(creator);
        if (node == null)
        {
            return null;
        }
        var tokens = java.util.stream.StreamSupport.stream(node.getLeafNodes().spliterator(), false)
            .filter(leaf -> !leaf.isHidden()).map(leaf -> leaf.getText()).toList();
        if (tokens.size() < 2)
        {
            return null;
        }
        // Do not resolve aliases or evaluate expressions: LS uses the written name.
        if (!"(".equals(tokens.get(1))) //$NON-NLS-1$
        {
            return tokens.get(1);
        }
        for (int index = 2; index < tokens.size(); index++)
        {
            String token = tokens.get(index);
            if ("Not".equalsIgnoreCase(token) || "Не".equalsIgnoreCase(token) //$NON-NLS-1$ //$NON-NLS-2$
                || "+".equals(token) || "-".equals(token)) //$NON-NLS-1$ //$NON-NLS-2$
            {
                continue;
            }
            // A concatenation can begin with a string member, but ("name") cannot.
            return token.length() >= 2 && token.startsWith("\"") && token.endsWith("\"") //$NON-NLS-1$ //$NON-NLS-2$
                ? token.substring(1, token.length() - 1).strip() : null;
        }
        return null;
    }
}
