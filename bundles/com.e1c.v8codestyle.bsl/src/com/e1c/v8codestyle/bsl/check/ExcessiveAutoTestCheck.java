/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.IfStatement;
import com._1c.g5.v8.dt.bsl.model.EmptyStatement;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
import com._1c.g5.v8.dt.bsl.model.ReturnStatement;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.OptInCheckExtension;

/** Opt-in audit of obsolete AutoTest guards, following the pinned LS contract. */
public class ExcessiveAutoTestCheck extends BasicCheck
{
    public static final String CHECK_ID = "excessive-auto-test-check"; //$NON-NLS-1$

    private static final Pattern CONDITION = Pattern.compile(
        "(\\.Свойство\\(\"АвтоТест\"\\)|=\"АвтоТест\"|\\.Property\\(\"AutoTest\"\\)|=\"AutoTest\")$", //$NON-NLS-1$
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.ExcessiveAutoTestCheck_title)
            .description(Messages.ExcessiveAutoTestCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new OptInCheckExtension())
            .module().checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Module module = (Module)object;
        ModuleType type = module.getModuleType();
        if (type != ModuleType.FORM_MODULE && type != ModuleType.OBJECT_MODULE
            && type != ModuleType.RECORDSET_MODULE && type != ModuleType.COMMON_MODULE)
        {
            return;
        }
        var contents = module.eAllContents();
        while (contents.hasNext() && !monitor.isCanceled())
        {
            if (!(contents.next() instanceof IfStatement statement) || statement.getIfPart() == null)
            {
                continue;
            }
            var branch = statement.getIfPart();
            var statements = branch.getStatements();
            // EDT exposes Return's terminator as one EmptyStatement; extra semicolons
            // remain real extra statements and must not be filtered out (LS parity).
            boolean singleReturn = !statements.isEmpty() && statements.get(0) instanceof ReturnStatement
                && (statements.size() == 1 || statements.size() == 2
                    && statements.get(1) instanceof EmptyStatement);
            if (!singleReturn
                || branch.getPredicate() == null)
            {
                continue;
            }
            // Leading standalone ';' is not retained in EDT's statements list.
            // Check the branch CST as well, rather than accepting '; Return;'.
            var branchNode = NodeModelUtils.getNode(branch);
            boolean afterThen = false;
            boolean extraPrefix = false;
            int returnOffset = NodeModelUtils.getNode(statements.get(0)).getOffset();
            for (var leaf : branchNode.getLeafNodes())
            {
                if (leaf.getOffset() >= returnOffset)
                {
                    break;
                }
                if (!leaf.isHidden() && afterThen)
                {
                    extraPrefix = true;
                }
                if (leaf.getText().equalsIgnoreCase("Then") || leaf.getText().equalsIgnoreCase("Тогда")) //$NON-NLS-1$ //$NON-NLS-2$
                {
                    afterThen = true;
                }
            }
            if (extraPrefix)
            {
                continue;
            }
            var predicate = NodeModelUtils.findActualNodeFor(branch.getPredicate());
            if (predicate == null)
            {
                continue;
            }
            StringBuilder text = new StringBuilder();
            for (var leaf : predicate.getLeafNodes())
            {
                if (!leaf.isHidden())
                {
                    text.append(leaf.getText());
                }
            }
            if (CONDITION.matcher(text).find())
            {
                addIssue(statement, acceptor);
            }
        }
    }

    private boolean addIssue(IfStatement statement, ResultAcceptor acceptor)
    {
        var node = NodeModelUtils.getNode(statement);
        if (node == null)
        {
            return false;
        }
        ILeafNode first = null;
        ILeafNode end = null;
        for (var leaf : node.getLeafNodes())
        {
            if (leaf.getSyntaxErrorMessage() != null)
            {
                return false;
            }
            if (!leaf.isHidden())
            {
                if (first == null)
                {
                    first = leaf;
                }
                if (leaf.getText().equalsIgnoreCase("EndIf") || leaf.getText().equalsIgnoreCase("КонецЕсли")) //$NON-NLS-1$ //$NON-NLS-2$
                {
                    end = leaf;
                }
            }
        }
        if (first == null || end == null)
        {
            return false;
        }
        acceptor.addIssue(new BslDirectLocationIssue(Messages.ExcessiveAutoTestCheck_message,
            new DirectLocation(first.getOffset(), end.getEndOffset() - first.getOffset(), first.getStartLine(),
                statement)));
        return true;
    }
}
