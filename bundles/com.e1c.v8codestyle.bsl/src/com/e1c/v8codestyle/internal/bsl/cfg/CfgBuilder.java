/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.internal.bsl.cfg;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com._1c.g5.v8.dt.bsl.model.Block;
import com._1c.g5.v8.dt.bsl.model.BreakStatement;
import com._1c.g5.v8.dt.bsl.model.ContinueStatement;
import com._1c.g5.v8.dt.bsl.model.DeclareStatement;
import com._1c.g5.v8.dt.bsl.model.GotoStatement;
import com._1c.g5.v8.dt.bsl.model.IfPreprocessor;
import com._1c.g5.v8.dt.bsl.model.IfPreprocessorDeclareStatement;
import com._1c.g5.v8.dt.bsl.model.IfPreprocessorStatement;
import com._1c.g5.v8.dt.bsl.model.IfStatement;
import com._1c.g5.v8.dt.bsl.model.Label;
import com._1c.g5.v8.dt.bsl.model.LabeledStatement;
import com._1c.g5.v8.dt.bsl.model.LoopStatement;
import com._1c.g5.v8.dt.bsl.model.PreprocessorConditional;
import com._1c.g5.v8.dt.bsl.model.PreprocessorItem;
import com._1c.g5.v8.dt.bsl.model.PreprocessorItemStatements;
import com._1c.g5.v8.dt.bsl.model.RaiseStatement;
import com._1c.g5.v8.dt.bsl.model.ReturnStatement;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com._1c.g5.v8.dt.bsl.model.TryExceptStatement;
import com._1c.g5.v8.dt.bsl.model.WhileStatement;

/**
 * Builds a {@link CfgGraph} for a method body or a module body. EMF port of the
 * pinned BSL Language Server {@code CfgBuildingParseTreeVisitor}: sequential
 * statements accumulate in basic blocks, terminal statements jump to their
 * targets (return/raise to the exit or the nearest exception handler, break and
 * continue to the enclosing loop, goto to the resolved label vertex), structures
 * are built recursively. After a terminal statement the next block stays without
 * incoming flow (it is still chained forward, so unreachable code keeps its place
 * in the graph). Zero-edge vertices are dropped at finalization.
 */
public final class CfgBuilder
{
    private final boolean loopIterations;
    private final CfgGraph graph = new CfgGraph();
    private final Map<Label, CfgGraph.LabelVertex> labels = new IdentityHashMap<>();

    private CfgBuilder(boolean loopIterations)
    {
        this.loopIterations = loopIterations;
    }

    /**
     * Builds the graph of a block (method or module body). Preprocessor-wrapped method bodies
     * are held in declare statements ({@code IfPreprocessorDeclareStatement}) in EDT, so both
     * statement lists are walked.
     *
     * @param block the block to analyze, cannot be {@code null}.
     * @param loopIterations whether loop body tails are chained back to the loop head.
     * @return the built graph, never {@code null}.
     */
    public static CfgGraph build(Block block, boolean loopIterations)
    {
        CfgBuilder builder = new CfgBuilder(loopIterations);
        List<org.eclipse.emf.ecore.EObject> flow = new ArrayList<>(block.getDeclareStatements());
        flow.addAll(block.getStatements());
        return builder.build(flow);
    }

    private CfgGraph build(List<? extends org.eclipse.emf.ecore.EObject> statements)
    {
        Walk walk = new Walk(statements, new Jumps(graph.getExit(), null, null, null));
        var removed = graph.connectTails(walk.tails, graph.getExit());
        graph.setEntry(removed.contains(walk.head) ? graph.getExit() : walk.head);
        removeEdgelessVertices();
        return graph;
    }

    private void removeEdgelessVertices()
    {
        for (CfgGraph.Vertex vertex : List.copyOf(graph.vertexSet()))
        {
            // blocks left by a terminal statement keep their place: they carry the
            // unreachable statements reported by the gap check (after break/continue/goto)
            if (vertex instanceof CfgGraph.BasicBlock block && block.orphanCause() != null
                && !block.statements().isEmpty())
            {
                continue;
            }
            if (vertex != graph.getExit() && graph.incomingEdgesOf(vertex).isEmpty()
                && graph.outgoingEdgesOf(vertex).isEmpty())
            {
                graph.removeVertex(vertex);
            }
        }
    }

    private CfgGraph.LabelVertex labelVertex(Label label)
    {
        return labels.computeIfAbsent(label, l -> {
            CfgGraph.LabelVertex vertex = new CfgGraph.LabelVertex(l);
            graph.addVertex(vertex);
            return vertex;
        });
    }

    private static final class Jumps
    {
        private final CfgGraph.Vertex returnTarget;
        private final CfgGraph.Vertex raiseTarget;
        private final CfgGraph.Vertex breakTarget;
        private final CfgGraph.Vertex continueTarget;

        Jumps(CfgGraph.Vertex returnTarget, CfgGraph.Vertex raiseTarget, CfgGraph.Vertex breakTarget,
            CfgGraph.Vertex continueTarget)
        {
            this.returnTarget = returnTarget;
            this.raiseTarget = raiseTarget;
            this.breakTarget = breakTarget;
            this.continueTarget = continueTarget;
        }

        Jumps withRaise(CfgGraph.Vertex target)
        {
            return new Jumps(returnTarget, target, breakTarget, continueTarget);
        }

        Jumps withLoop(CfgGraph.Vertex breakTo, CfgGraph.Vertex continueTo)
        {
            return new Jumps(returnTarget, raiseTarget, breakTo, continueTo);
        }
    }

    private final class Walk
    {
        CfgGraph.Vertex head;
        CfgGraph.BasicBlock current;
        final List<CfgGraph.Vertex> tails = new ArrayList<>();
        final Jumps jumps;

        Walk(List<? extends org.eclipse.emf.ecore.EObject> statements, Jumps jumps)
        {
            this.jumps = jumps;
            current = new CfgGraph.BasicBlock();
            graph.addVertex(current);
            head = current;
            tails.add(current);
            for (var statement : statements)
            {
                process(statement);
            }
        }

        private void process(org.eclipse.emf.ecore.EObject statement)
        {
            if (statement instanceof LabeledStatement labeled)
            {
                processLabeled(labeled);
                return;
            }
            if (statement instanceof DeclareStatement)
            {
                // plain variable declarations are flow neutral; the preprocessor
                // declare wrapper is handled below
                if (statement instanceof IfPreprocessorDeclareStatement preprocessor)
                {
                    processPreprocessor(preprocessor);
                }
                return;
            }
            if (statement instanceof IfStatement ifStatement)
            {
                List<Branch> branches = new ArrayList<>();
                branches.add(new Branch(ifStatement.getIfPart(), ifStatement.getIfPart().getStatements()));
                for (var elsif : ifStatement.getElsIfParts())
                {
                    branches.add(new Branch(elsif, elsif.getStatements()));
                }
                processConditional(branches, ifStatement.getElseStatements(), false);
            }
            else if (statement instanceof IfPreprocessorStatement preprocessor)
            {
                processPreprocessor(preprocessor);
            }
            else if (statement instanceof LoopStatement loop)
            {
                processLoop(loop);
            }
            else if (statement instanceof TryExceptStatement tryExcept)
            {
                processTryExcept(tryExcept);
            }
            else if (statement instanceof ReturnStatement returnStatement)
            {
                terminate(returnStatement, jumps.returnTarget, CfgGraph.OrphanCause.RETURN);
            }
            else if (statement instanceof RaiseStatement raiseStatement)
            {
                CfgGraph.Vertex target = jumps.raiseTarget == null ? graph.getExit() : jumps.raiseTarget;
                terminate(raiseStatement, target, CfgGraph.OrphanCause.RAISE);
            }
            else if (statement instanceof BreakStatement breakStatement)
            {
                if (jumps.breakTarget != null)
                {
                    terminate(breakStatement, jumps.breakTarget, CfgGraph.OrphanCause.BREAK);
                }
                else
                {
                    current.statements().add(statement);
                }
            }
            else if (statement instanceof ContinueStatement continueStatement)
            {
                if (jumps.continueTarget != null)
                {
                    terminate(continueStatement, jumps.continueTarget, CfgGraph.OrphanCause.CONTINUE);
                }
                else
                {
                    current.statements().add(statement);
                }
            }
            else if (statement instanceof GotoStatement gotoStatement)
            {
                current.statements().add(statement);
                Label label = gotoStatement.getLabel();
                if (label != null && !label.eIsProxy())
                {
                    graph.connectTails(List.of(current), labelVertex(label));
                }
                split(CfgGraph.OrphanCause.GOTO);
            }
            else
            {
                // EDT model inserts EmptyStatement nodes (e.g. statement terminators);
                // the pinned grammar does not produce them, they carry no control flow
                if (!(statement instanceof com._1c.g5.v8.dt.bsl.model.EmptyStatement))
                {
                    current.statements().add(statement);
                }
            }
        }

        private void processLabeled(LabeledStatement labeled)
        {
            for (Label label : labeled.getLabels())
            {
                CfgGraph.LabelVertex vertex = labelVertex(label);
                var removed = graph.connectTails(List.of(current), vertex);
                if (removed.contains(head))
                {
                    head = vertex;
                }
                CfgGraph.BasicBlock next = new CfgGraph.BasicBlock();
                graph.addVertex(next);
                graph.addEdge(vertex, next, CfgGraph.EdgeType.ORDINARY);
                current = next;
                tails.clear();
                tails.add(next);
            }
            if (labeled.getStatement() != null)
            {
                process(labeled.getStatement());
            }
        }

        private void terminate(Statement statement, CfgGraph.Vertex target, CfgGraph.OrphanCause cause)
        {
            current.statements().add(statement);
            graph.connectTails(List.of(current), target);
            split(cause);
        }

        private void split(CfgGraph.OrphanCause cause)
        {
            CfgGraph.BasicBlock next = new CfgGraph.BasicBlock();
            next.setOrphanCause(cause);
            graph.addVertex(next);
            current = next;
            tails.clear();
            tails.add(next);
        }

        private void processPreprocessor(IfPreprocessor ifPreprocessor)
        {
            List<Branch> branches = new ArrayList<>();
            branches.add(new Branch(ifPreprocessor.getIfPart(), itemStatements(ifPreprocessor.getIfPart())));
            for (var elsif : ifPreprocessor.getElsIfParts())
            {
                branches.add(new Branch(elsif, itemStatements(elsif)));
            }
            var elseElement = ifPreprocessor.getElseElement();
            processConditional(branches, elseElement == null ? List.of() : itemStatements(elseElement), true);
        }

        private void processConditional(List<Branch> branches, List<Statement> elseStatements,
            boolean preprocessor)
        {
            List<CfgGraph.Vertex> branchTails = new ArrayList<>();
            CfgGraph.Conditional previous = null;
            CfgGraph.Conditional first = null;
            for (Branch branch : branches)
            {
                CfgGraph.Conditional conditional = new CfgGraph.Conditional(branch.ast(), preprocessor);
                graph.addVertex(conditional);
                if (previous == null)
                {
                    first = conditional;
                    var removed = graph.connectTails(List.of(current), conditional);
                    if (removed.contains(head))
                    {
                        head = conditional;
                    }
                }
                else
                {
                    graph.addEdge(previous, conditional, CfgGraph.EdgeType.FALSE_BRANCH);
                }
                Walk body = new Walk(branch.statements(), jumps);
                graph.addEdge(conditional, body.head, CfgGraph.EdgeType.TRUE_BRANCH);
                branchTails.addAll(body.tails);
                previous = conditional;
            }
            CfgGraph.Conditional lastWithoutElse = elseStatements.isEmpty() ? previous : null;
            if (!elseStatements.isEmpty())
            {
                Walk elseBody = new Walk(elseStatements, jumps);
                branchTails.addAll(elseBody.tails);
                // the else body is entered from the last condition FALSE branch
                if (previous != null)
                {
                    graph.addEdge(previous, elseBody.head, CfgGraph.EdgeType.FALSE_BRANCH);
                }
            }
            CfgGraph.BasicBlock continuation = new CfgGraph.BasicBlock();
            graph.addVertex(continuation);
            if (lastWithoutElse != null)
            {
                graph.addEdge(lastWithoutElse, continuation, CfgGraph.EdgeType.FALSE_BRANCH);
            }
            graph.connectTails(branchTails, continuation);
            current = continuation;
            tails.clear();
            tails.add(continuation);
        }

        private void processLoop(LoopStatement loop)
        {
            WhileStatement whileStatement = loop instanceof WhileStatement ws ? ws : null;
            CfgGraph.Loop vertex = new CfgGraph.Loop(loop, whileStatement != null,
                whileStatement == null ? null : whileStatement.getPredicate());
            graph.addVertex(vertex);
            var removed = graph.connectTails(List.of(current), vertex);
            if (removed.contains(head))
            {
                head = vertex;
            }
            CfgGraph.BasicBlock after = new CfgGraph.BasicBlock();
            graph.addVertex(after);
            Walk body = new Walk(loop.getStatements(), jumps.withLoop(after, vertex));
            graph.addEdge(vertex, body.head, CfgGraph.EdgeType.TRUE_BRANCH);
            graph.addEdge(vertex, after, CfgGraph.EdgeType.FALSE_BRANCH);
            if (loopIterations)
            {
                for (CfgGraph.Vertex tail : body.tails)
                {
                    graph.addEdge(tail, vertex, CfgGraph.EdgeType.ITERATION);
                }
            }
            current = after;
            tails.clear();
            tails.add(after);
        }

        private void processTryExcept(TryExceptStatement tryExcept)
        {
            CfgGraph.TryExcept vertex = new CfgGraph.TryExcept(tryExcept);
            graph.addVertex(vertex);
            var removed = graph.connectTails(List.of(current), vertex);
            if (removed.contains(head))
            {
                head = vertex;
            }
            Walk handler = new Walk(tryExcept.getExceptStatements(), jumps);
            Walk body = new Walk(tryExcept.getTryStatements(), jumps.withRaise(handler.head));
            graph.addEdge(vertex, body.head, CfgGraph.EdgeType.TRUE_BRANCH);
            graph.addEdge(vertex, handler.head, CfgGraph.EdgeType.FALSE_BRANCH);
            CfgGraph.BasicBlock continuation = new CfgGraph.BasicBlock();
            graph.addVertex(continuation);
            List<CfgGraph.Vertex> branchTails = new ArrayList<>(body.tails);
            branchTails.addAll(handler.tails);
            graph.connectTails(branchTails, continuation);
            current = continuation;
            tails.clear();
            tails.add(continuation);
        }
    }

    private record Branch(org.eclipse.emf.ecore.EObject ast, List<Statement> statements)
    {
        // structure branch pair
    }

    private static List<Statement> itemStatements(PreprocessorConditional conditional)
    {
        PreprocessorItem item = conditional.getItem();
        if (item instanceof PreprocessorItemStatements statements && !statements.getStatements().isEmpty())
        {
            return statements.getStatements();
        }
        return List.of();
    }
}
