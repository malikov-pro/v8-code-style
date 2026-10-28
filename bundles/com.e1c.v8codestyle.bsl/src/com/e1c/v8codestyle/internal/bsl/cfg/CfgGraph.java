/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.internal.bsl.cfg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.Label;

/**
 * Minimal control flow graph over the BSL EMF model. Port of the graph part of the
 * pinned BSL Language Server {@code cfg} package: vertices for basic blocks,
 * conditions, loops, try/except, labels and the method exit; edge types for
 * condition branches and loop iterations. Empty basic blocks are transparent:
 * connecting an empty block moves its incoming edges to the target instead of
 * chaining through it (this is what makes an else-less conditional point its
 * FALSE branch directly to the follower).
 */
public final class CfgGraph
{
    /** Edge kind. */
    public enum EdgeType
    {
        ORDINARY, TRUE_BRANCH, FALSE_BRANCH, ITERATION
    }

    /** Why a basic block was left unconnected after a terminal statement. */
    public enum OrphanCause
    {
        RETURN, RAISE, BREAK, CONTINUE, GOTO
    }

    /** Base vertex. */
    public static abstract class Vertex
    {
        Vertex()
        {
            // only nested types
        }
    }

    /** Sequential statements block. */
    public static final class BasicBlock extends Vertex
    {
        private final List<EObject> statements = new ArrayList<>();
        private OrphanCause orphanCause;

        /** @return ordered statements of the block, never {@code null}. */
        public List<EObject> statements()
        {
            return statements;
        }

        /** @return why the block has no incoming flow, or {@code null} for a connected block. */
        public OrphanCause orphanCause()
        {
            return orphanCause;
        }

        void setOrphanCause(OrphanCause cause)
        {
            orphanCause = cause;
        }
    }

    /** Condition of an If branch or a preprocessor branch. */
    public static final class Conditional extends Vertex
    {
        private final EObject ast;
        private final boolean preprocessor;

        Conditional(EObject ast, boolean preprocessor)
        {
            this.ast = ast;
            this.preprocessor = preprocessor;
        }

        /** @return the conditional AST node (mcore Conditional or preprocessor conditional). */
        public EObject ast()
        {
            return ast;
        }

        /** @return whether this is a preprocessor condition. */
        public boolean preprocessor()
        {
            return preprocessor;
        }
    }

    /** Loop head vertex (While, For, ForEach). */
    public static final class Loop extends Vertex
    {
        private final EObject ast;
        private final boolean whileLoop;
        private final Expression predicate;

        Loop(EObject ast, boolean whileLoop, Expression predicate)
        {
            this.ast = ast;
            this.whileLoop = whileLoop;
            this.predicate = predicate;
        }

        /** @return loop statement AST. */
        public EObject ast()
        {
            return ast;
        }

        /** @return whether this is a While loop with the predicate available. */
        public boolean whileLoop()
        {
            return whileLoop;
        }

        /** @return loop predicate, may be {@code null}. */
        public Expression predicate()
        {
            return predicate;
        }
    }

    /** Try/Except vertex: TRUE branch enters try body, FALSE branch enters handler. */
    public static final class TryExcept extends Vertex
    {
        private final EObject ast;

        TryExcept(EObject ast)
        {
            this.ast = ast;
        }

        /** @return try statement AST. */
        public EObject ast()
        {
            return ast;
        }
    }

    /** Method or module body exit. */
    public static final class ExitVertex extends Vertex
    {
        ExitVertex()
        {
            // builder only
        }
    }

    /** Label vertex; the goto reference is resolved in the model, no name lookup. */
    public static final class LabelVertex extends Vertex
    {
        private final Label label;

        LabelVertex(Label label)
        {
            this.label = label;
        }

        /** @return the label AST node. */
        public Label label()
        {
            return label;
        }
    }

    /** Directed edge. */
    public static final class Edge
    {
        private final Vertex source;
        private final Vertex target;
        private final EdgeType type;

        Edge(Vertex source, Vertex target, EdgeType type)
        {
            this.source = source;
            this.target = target;
            this.type = type;
        }

        /** @return edge source vertex. */
        public Vertex source()
        {
            return source;
        }

        /** @return edge target vertex. */
        public Vertex target()
        {
            return target;
        }

        /** @return edge type. */
        public EdgeType type()
        {
            return type;
        }
    }

    private final Map<Vertex, List<Edge>> outgoing = new LinkedHashMap<>();
    private final Map<Vertex, List<Edge>> incoming = new LinkedHashMap<>();
    private final List<Vertex> vertices = new ArrayList<>();
    private Vertex entry;
    private final ExitVertex exit = new ExitVertex();

    CfgGraph()
    {
        addVertex(exit);
    }

    /** @return the exit vertex of the graph. */
    public ExitVertex getExit()
    {
        return exit;
    }

    /** @return the entry vertex of the graph. */
    public Vertex getEntry()
    {
        return entry;
    }

    void setEntry(Vertex entry)
    {
        this.entry = entry;
    }

    /** @return unmodifiable list of all vertices. */
    public List<Vertex> vertexSet()
    {
        return Collections.unmodifiableList(vertices);
    }

    void addVertex(Vertex vertex)
    {
        if (!outgoing.containsKey(vertex))
        {
            outgoing.put(vertex, new ArrayList<>());
            incoming.put(vertex, new ArrayList<>());
            vertices.add(vertex);
        }
    }

    void addEdge(Vertex from, Vertex to, EdgeType type)
    {
        addVertex(from);
        addVertex(to);
        Edge edge = new Edge(from, to, type);
        outgoing.get(from).add(edge);
        incoming.get(to).add(edge);
    }

    /** @return unmodifiable list of incoming edges of the vertex. */
    public List<Edge> incomingEdgesOf(Vertex vertex)
    {
        return Collections.unmodifiableList(incoming.getOrDefault(vertex, List.of()));
    }

    /** @return unmodifiable list of outgoing edges of the vertex. */
    public List<Edge> outgoingEdgesOf(Vertex vertex)
    {
        return Collections.unmodifiableList(outgoing.getOrDefault(vertex, List.of()));
    }

    /** @return whether a direct edge of the given type exists. */
    public boolean hasEdge(Vertex from, Vertex to, EdgeType type)
    {
        for (Edge edge : outgoingEdgesOf(from))
        {
            if (edge.target() == to && edge.type() == type)
            {
                return true;
            }
        }
        return false;
    }

    void removeVertex(Vertex vertex)
    {
        for (Edge edge : List.copyOf(outgoing.getOrDefault(vertex, List.of())))
        {
            removeEdge(edge);
        }
        for (Edge edge : List.copyOf(incoming.getOrDefault(vertex, List.of())))
        {
            removeEdge(edge);
        }
        outgoing.remove(vertex);
        incoming.remove(vertex);
        vertices.remove(vertex);
    }

    void removeEdge(Edge edge)
    {
        outgoing.getOrDefault(edge.source(), List.of()).remove(edge);
        incoming.getOrDefault(edge.target(), List.of()).remove(edge);
    }

    /**
     * Connects the given tail vertices to the target. An empty basic block tail is
     * transparent: its incoming edges are moved to the target and the block is removed.
     *
     * @param tails tail vertices to connect, cannot be {@code null}.
     * @param target follower vertex, cannot be {@code null}.
     * @return vertices that were removed by transparency.
     */
    Collection<Vertex> connectTails(Collection<Vertex> tails, Vertex target)
    {
        List<Vertex> removed = new ArrayList<>();
        for (Vertex tail : List.copyOf(tails))
        {
            if (tail == target)
            {
                continue;
            }
            if (tail instanceof BasicBlock block && block.statements.isEmpty())
            {
                for (Edge edge : List.copyOf(incomingEdgesOf(tail)))
                {
                    if (edge.source() != tail)
                    {
                        addEdge(edge.source(), target, edge.type());
                    }
                    removeEdge(edge);
                }
                removeVertex(tail);
                removed.add(tail);
            }
            else
            {
                addEdge(tail, target, EdgeType.ORDINARY);
            }
        }
        return removed;
    }
}
