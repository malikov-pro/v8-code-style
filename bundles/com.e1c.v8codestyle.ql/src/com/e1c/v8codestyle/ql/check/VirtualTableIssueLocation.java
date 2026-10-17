/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check;

import static com._1c.g5.v8.dt.ql.model.QlPackage.Literals.ABSTRACT_QUERY_SCHEMA_SOURCE__ALIAS;

import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.ql.model.QuerySchemaTable;
import com.e1c.g5.v8.dt.ql.check.QlBasicDelegateCheck.IQlResultAcceptor;

/** Virtual-table reference and arguments, excluding explicit or implicit alias. */
final class VirtualTableIssueLocation
{
    private VirtualTableIssueLocation()
    {
    }

    static void addIssue(String message, QuerySchemaTable table, IQlResultAcceptor acceptor)
    {
        var node = NodeModelUtils.getNode(table);
        if (node == null)
        {
            return;
        }
        var aliasNodes = NodeModelUtils.findNodesForFeature(table, ABSTRACT_QUERY_SCHEMA_SOURCE__ALIAS);
        int aliasOffset = aliasNodes.isEmpty() ? node.getEndOffset() : aliasNodes.get(0).getOffset();
        int start = -1;
        int end = -1;
        for (var leaf : node.getLeafNodes())
        {
            if (leaf.isHidden())
            {
                continue;
            }
            if (leaf.getOffset() >= aliasOffset || (leaf.getSemanticElement() == table
                && ("AS".equalsIgnoreCase(leaf.getText()) || "КАК".equalsIgnoreCase(leaf.getText())))) //$NON-NLS-1$ //$NON-NLS-2$
            {
                break;
            }
            if (start < 0)
            {
                start = leaf.getOffset();
            }
            end = leaf.getEndOffset();
        }
        if (start >= 0 && end > start)
        {
            acceptor.addIssue(message, NodeModelUtils.getLineAndColumn(node.getRootNode(), start).getLine(),
                start, end - start);
        }
    }
}
