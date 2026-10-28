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
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.bsl.model.BinaryExpression;
import com._1c.g5.v8.dt.bsl.model.BinaryOperation;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.UnaryExpression;
import com._1c.g5.v8.dt.bsl.model.UnaryOperation;
import com.e1c.v8codestyle.bsl.check.IdenticalExpressionsCheck;

/** Records semantic AST and preserved syntax nodes for previously blocked negation checks. */
public class AstNegationResearchTest
    extends AbstractSingleModuleTestBase
{
    public AstNegationResearchTest()
    {
        super(IdenticalExpressionsCheck.class);
    }

    /** @throws Exception if the valid module cannot be loaded */
    @Test
    public void testValidNegationsWithoutParserRecovery() throws Exception
    {
        Module module = updateAndGetModule(FOLDER_RESOURCE + "ast-negation-valid.bsl"); //$NON-NLS-1$
        for (INode leaf : NodeModelUtils.findActualNodeFor(module).getLeafNodes())
        {
            assertNull("Unexpected syntax error at line " + leaf.getStartLine(), leaf.getSyntaxErrorMessage()); //$NON-NLS-1$
        }
        List<Integer> candidates = new ArrayList<>();
        boolean loadPredicateFound = false;
        for (UnaryExpression unary : EcoreUtil2.getAllContentsOfType(module, UnaryExpression.class))
        {
            if (unary.getOperation() != UnaryOperation.NOT)
            {
                continue;
            }
            assertNotNull("Valid NOT expression must have its operand", unary.getOperand()); //$NON-NLS-1$
            INode node = NodeModelUtils.findActualNodeFor(unary);
            if (unary.getOperand() instanceof BinaryExpression binary
                && binary.getOperation() == BinaryOperation.NE
                || unary.getOperand() instanceof UnaryExpression nested
                    && nested.getOperation() == UnaryOperation.NOT)
            {
                candidates.add(node.getStartLine());
            }
            if (node.getStartLine() == 11)
            {
                assertEquals("DynamicFeatureAccess", unary.getOperand().eClass().getName()); //$NON-NLS-1$
                loadPredicateFound = true;
            }
        }
        assertEquals(List.of(2, 3, 4, 7, 9, 14, 17), candidates);
        assertEquals(true, loadPredicateFound);
        System.out.println("AST-RESEARCH valid-source candidates: " + candidates); //$NON-NLS-1$
    }

    /** @throws Exception if the research module cannot be loaded */
    @Test
    public void testNegationRepresentations() throws Exception
    {
        Module module = updateAndGetModule(FOLDER_RESOURCE + "ast-negation-research.bsl"); //$NON-NLS-1$
        int expressions = 0;
        for (SimpleStatement statement : EcoreUtil2.getAllContentsOfType(module, SimpleStatement.class))
        {
            INode node = NodeModelUtils.findActualNodeFor(statement);
            assertNotNull(node);
            System.out.println("AST-RESEARCH statement: " + node.getText().strip()); //$NON-NLS-1$
            dumpSemantic(statement.getRight(), "  "); //$NON-NLS-1$
            dumpSyntax(node, "  "); //$NON-NLS-1$
            expressions++;
        }
        assertEquals(12, expressions);
        List<Integer> supportedViolations = new ArrayList<>();
        List<Integer> incompleteOperands = new ArrayList<>();
        for (UnaryExpression unary : EcoreUtil2.getAllContentsOfType(module, UnaryExpression.class))
        {
            if (unary.getOperation() != UnaryOperation.NOT)
            {
                continue;
            }
            INode node = NodeModelUtils.findActualNodeFor(unary);
            assertNotNull(node);
            if (unary.getOperand() == null)
            {
                incompleteOperands.add(node.getStartLine());
            }
            else if (unary.getOperand() instanceof BinaryExpression binary
                && binary.getOperation() == BinaryOperation.NE
                || unary.getOperand() instanceof UnaryExpression nested
                    && nested.getOperation() == UnaryOperation.NOT)
            {
                supportedViolations.add(node.getStartLine());
            }
        }
        assertEquals(List.of(2, 3, 5, 8, 11), supportedViolations);
        assertEquals(List.of(4, 10), incompleteOperands);
        System.out.println("AST-RESEARCH supported violations: " + supportedViolations); //$NON-NLS-1$
        System.out.println("AST-RESEARCH incomplete operands: " + incompleteOperands); //$NON-NLS-1$
        for (INode leaf : NodeModelUtils.findActualNodeFor(module).getLeafNodes())
        {
            if (leaf.getSyntaxErrorMessage() != null)
            {
                System.out.println("AST-RESEARCH syntax error line " + leaf.getStartLine() + ": " //$NON-NLS-1$ //$NON-NLS-2$
                    + leaf.getSyntaxErrorMessage().getMessage());
            }
        }
    }

    private static void dumpSemantic(EObject object, String indent)
    {
        if (object == null)
        {
            System.out.println(indent + "NULL"); //$NON-NLS-1$
            return;
        }
        String detail = ""; //$NON-NLS-1$
        if (object instanceof UnaryExpression unary)
        {
            detail = " operation=" + unary.getOperation() + " operand=" //$NON-NLS-1$ //$NON-NLS-2$
                + (unary.getOperand() == null ? "NULL" : unary.getOperand().eClass().getName()); //$NON-NLS-1$
        }
        else if (object instanceof BinaryExpression binary)
        {
            detail = " operation=" + binary.getOperation(); //$NON-NLS-1$
        }
        System.out.println(indent + object.eClass().getName() + detail);
        for (EObject child : object.eContents())
        {
            dumpSemantic(child, indent + "  "); //$NON-NLS-1$
        }
    }

    private static void dumpSyntax(INode node, String indent)
    {
        if (!(node instanceof ICompositeNode composite))
        {
            return;
        }
        EObject grammar = node.getGrammarElement();
        String rule = grammar == null ? "NULL" : grammar.toString(); //$NON-NLS-1$
        EObject semantic = node.getSemanticElement();
        System.out.println(indent + "CST " + rule + " semantic=" //$NON-NLS-1$ //$NON-NLS-2$
            + (semantic == null ? "NULL" : semantic.eClass().getName()) + " text=" //$NON-NLS-1$ //$NON-NLS-2$
            + node.getText().strip().replace('\n', ' '));
        for (INode child : composite.getChildren())
        {
            dumpSyntax(child, indent + "  "); //$NON-NLS-1$
        }
    }
}
