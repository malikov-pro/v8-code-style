/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.EmptyExpression;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.OperatorStyleCreator;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Checks direct non-empty nested constructors in multi-argument structures. */
public class NestedConstructorsInStructureDeclarationCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "nested-constructors-in-structure-declaration"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.NestedConstructorsInStructureDeclarationCheck_title)
            .description(Messages.NestedConstructorsInStructureDeclarationCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Module module = (Module)object;
        for (OperatorStyleCreator creator : EcoreUtil2.getAllContentsOfType(module, OperatorStyleCreator.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (creator.getParams().size() <= 1 || !isStructure(creator))
            {
                continue;
            }
            for (Expression argument : creator.getParams())
            {
                INode argumentNode = NodeModelUtils.findActualNodeFor(argument);
                if (argument instanceof OperatorStyleCreator nested && argumentNode != null
                    && startsWithNew(argumentNode)
                    && nested.getParams().stream().anyMatch(parameter -> !(parameter instanceof EmptyExpression)))
                {
                    INode node = NodeModelUtils.getNode(creator);
                    if (node != null)
                    {
                        acceptor.addIssue(new BslDirectLocationIssue(Messages.NestedConstructorsInStructureDeclarationCheck_message,
                            new DirectLocation(node.getOffset(), node.getLength(), node.getStartLine(), module)));
                    }
                    break;
                }
            }
        }
    }

    private static boolean startsWithNew(INode node)
    {
        for (ILeafNode leaf : node.getLeafNodes())
        {
            if (!leaf.isHidden())
            {
                return "Новый".equalsIgnoreCase(leaf.getText()) || "New".equalsIgnoreCase(leaf.getText()); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        return false;
    }

    private static boolean isStructure(OperatorStyleCreator creator)
    {
        if (creator.getType() != null)
        {
            String name = creator.getType().getName();
            return "Structure".equalsIgnoreCase(name) || "FixedStructure".equalsIgnoreCase(name) //$NON-NLS-1$ //$NON-NLS-2$
                || "Структура".equalsIgnoreCase(name) || "ФиксированнаяСтруктура".equalsIgnoreCase(name); //$NON-NLS-1$ //$NON-NLS-2$
        }
        INode node = NodeModelUtils.getNode(creator);
        return node != null && node.getText().stripLeading()
            .matches("(?is)^(?:Новый|New)\\s+(?:Структура|ФиксированнаяСтруктура|Structure|FixedStructure)\\b.*"); //$NON-NLS-1$
    }
}
