/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.BinaryExpression;
import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.EmptyExpression;
import com._1c.g5.v8.dt.bsl.model.EmptyStatement;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.IndexAccess;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.RaiseStatement;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.TryExceptStatement;
import com._1c.g5.v8.dt.bsl.model.UnaryExpression;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Completeness of global event-log calls, preserving pinned LS message priority. */
public class UsageWriteLogEventCheck extends BasicCheck
{
    public static final String CHECK_ID = "usage-write-log-event"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.UsageWriteLogEventCheck_title)
            .description(Messages.UsageWriteLogEventCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.TRIVIAL)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module().checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        var contents = ((Module)object).eAllContents();
        while (contents.hasNext() && !monitor.isCanceled())
        {
            if (contents.next() instanceof Invocation call && isGlobal(call)
                && named(call, "ЗаписьЖурналаРегистрации", "WriteLogEvent")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                String message = message(call);
                if (message != null)
                {
                    var node = NodeModelUtils.getNode(call);
                    if (node != null)
                    {
                        var leaves = new ArrayList<org.eclipse.xtext.nodemodel.ILeafNode>();
                        boolean syntaxError = false;
                        for (var leaf : node.getLeafNodes())
                        {
                            syntaxError |= leaf.getSyntaxErrorMessage() != null;
                            if (!leaf.isHidden())
                            {
                                leaves.add(leaf);
                            }
                        }
                        if (!syntaxError && !leaves.isEmpty())
                        {
                            var first = leaves.get(0);
                            var last = leaves.get(leaves.size() - 1);
                            acceptor.addIssue(new BslDirectLocationIssue(message, new DirectLocation(first.getOffset(),
                                last.getEndOffset() - first.getOffset(), first.getStartLine(), call)));
                        }
                    }
                }
                // LS does not visit nested global calls inside an already handled log call.
                contents.prune();
            }
        }
    }

    private String message(Invocation call)
    {
        var params = call.getParams();
        if (params.size() < 5)
        {
            return Messages.UsageWriteLogEventCheck_arity;
        }
        if (params.get(1) instanceof EmptyExpression)
        {
            return Messages.UsageWriteLogEventCheck_level;
        }
        if (params.get(4) instanceof EmptyExpression)
        {
            return Messages.UsageWriteLogEventCheck_comment;
        }
        if (!insideExcept(call))
        {
            return null;
        }
        if (!errorLevel(params.get(1)))
        {
            return Messages.UsageWriteLogEventCheck_errorLevel;
        }
        var block = blockStatements(call);
        if (block.stream().anyMatch(RaiseStatement.class::isInstance))
        {
            return null;
        }
        return validComment(params.get(4), block, true) ? null : Messages.UsageWriteLogEventCheck_details;
    }

    private static boolean insideExcept(EObject object)
    {
        for (EObject child = object; child.eContainer() != null; child = child.eContainer())
        {
            if (child.eContainer() instanceof TryExceptStatement owner && owner.getExceptStatements().contains(child))
            {
                return true;
            }
        }
        return false;
    }

    private static List<Statement> blockStatements(EObject object)
    {
        for (EObject child = object; child.eContainer() != null; child = child.eContainer())
        {
            if (child instanceof Statement && child.eContainingFeature().isMany())
            {
                Object value = child.eContainer().eGet(child.eContainingFeature());
                if (value instanceof List<?> list)
                {
                    return list.stream().filter(Statement.class::isInstance).map(Statement.class::cast)
                        .filter(s -> !(s instanceof EmptyStatement)).toList();
                }
            }
        }
        return List.of();
    }

    private static boolean errorLevel(Expression expression)
    {
        List<String> parts = tokenList(expression);
        if (parts.isEmpty() || parts.get(0).equals("(")) //$NON-NLS-1$
        {
            return true;
        }
        // ANTLR's member keeps unary operators outside its complexIdentifier.
        Expression member = expression;
        while (member instanceof UnaryExpression unary)
        {
            member = unary.getOperand();
        }
        if (member instanceof BinaryExpression || member instanceof StaticFeatureAccess)
        {
            return true;
        }
        String text = tokens(member);
        if (member instanceof DynamicFeatureAccess || member instanceof IndexAccess
            || member instanceof Invocation call && !isGlobal(call))
        {
            return text.matches("(?iu)(УровеньЖурналаРегистрации|EventLogLevel)\\.(Ошибка|Error)"); //$NON-NLS-1$
        }
        return true;
    }

    private static boolean validComment(Expression expression, List<Statement> block, boolean checkAssignment)
    {
        List<Invocation> calls = new ArrayList<>();
        if (expression instanceof Invocation call)
        {
            calls.add(call);
        }
        calls.addAll(EcoreUtil2.getAllContentsOfType(expression, Invocation.class));
        if (calls.stream().anyMatch(call -> named(call, "ПодробноеПредставлениеОшибки", "DetailErrorDescription") //$NON-NLS-1$ //$NON-NLS-2$
            && acceptedHelper(call) && EcoreUtil2.getAllContentsOfType(call, Invocation.class).stream()
                .anyMatch(inner -> isGlobal(inner) && named(inner, "ИнформацияОбОшибке", "ErrorInfo")))) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return true;
        }
        if (calls.stream().anyMatch(call -> isGlobal(call) && named(call, "ОписаниеОшибки", "ErrorDescription") //$NON-NLS-1$ //$NON-NLS-2$
            || acceptedHelper(call) && named(call, "КраткоеПредставлениеОшибки", "BriefErrorDescription"))) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return false;
        }
        var parts = tokenList(expression);
        if (!parts.isEmpty() && parts.get(0).equals("(")) //$NON-NLS-1$
        {
            // LS does not descend from a parenthesized member to infer its value.
            return false;
        }
        if (expression instanceof BinaryExpression binary)
        {
            return validComment(binary.getLeft(), block, checkAssignment)
                && validComment(binary.getRight(), block, checkAssignment);
        }
        if (expression instanceof UnaryExpression unary)
        {
            return validComment(unary.getOperand(), block, checkAssignment);
        }
        if (!checkAssignment)
        {
            return false;
        }
        if (expression instanceof StaticFeatureAccess || expression instanceof DynamicFeatureAccess
            || expression instanceof Invocation || expression instanceof IndexAccess)
        {
            if (!block.isEmpty() && block.get(0) instanceof SimpleStatement assignment
                && assignment.getLeft() != null && assignment.getRight() != null
                && !hasLeadingEmptyStatement(assignment)
                && tokens(expression).equalsIgnoreCase(tokens(assignment.getLeft())))
            {
                return validComment(assignment.getRight(), block, false);
            }
            return true;
        }
        return false;
    }

    private static boolean hasLeadingEmptyStatement(Statement statement)
    {
        var node = NodeModelUtils.getNode(statement);
        var owner = NodeModelUtils.getNode(statement.eContainer());
        if (node == null || owner == null)
        {
            return false;
        }
        String previous = ""; //$NON-NLS-1$
        for (var leaf : owner.getLeafNodes())
        {
            if (leaf.getOffset() >= node.getOffset())
            {
                break;
            }
            if (!leaf.isHidden())
            {
                previous = leaf.getText();
            }
        }
        return previous.equals(";"); //$NON-NLS-1$
    }

    private static boolean acceptedHelper(Invocation call)
    {
        if (isGlobal(call))
        {
            return true;
        }
        if (call.getMethodAccess() instanceof DynamicFeatureAccess access)
        {
            String source = tokens(access.getSource());
            return source.equalsIgnoreCase("ОбработкаОшибок") || source.equalsIgnoreCase("ErrorProcessing"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return false;
    }

    private static boolean isGlobal(Invocation call)
    {
        return call.getMethodAccess() instanceof StaticFeatureAccess;
    }

    private static boolean named(Invocation call, String ru, String en)
    {
        var access = call.getMethodAccess();
        return access != null && (ru.equalsIgnoreCase(access.getName()) || en.equalsIgnoreCase(access.getName()));
    }

    private static String tokens(EObject object)
    {
        return String.join("", tokenList(object)); //$NON-NLS-1$
    }

    private static List<String> tokenList(EObject object)
    {
        var node = NodeModelUtils.findActualNodeFor(object);
        if (node == null)
        {
            return List.of();
        }
        List<String> text = new ArrayList<>();
        for (var leaf : node.getLeafNodes())
        {
            if (!leaf.isHidden())
            {
                text.add(leaf.getText());
            }
        }
        return text;
    }
}
