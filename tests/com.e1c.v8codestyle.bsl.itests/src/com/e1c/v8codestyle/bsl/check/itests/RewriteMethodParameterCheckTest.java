/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: 1C-Soft LLC
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the BSL Language Server diagnostic RewriteMethodParameter
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.RewriteMethodParameterCheck;

/**
 * Tests for {@link RewriteMethodParameterCheck} — port of the BSL Language
 * Server diagnostic RewriteMethodParameter.
 *
 * @author malikov-pro
 */
public class RewriteMethodParameterCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "rewrite-method-parameter.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "rewrite-method-parameter-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public RewriteMethodParameterCheckTest()
    {
        super(RewriteMethodParameterCheck.class);
    }

    /**
     * Each by-value parameter rewritten before its first use is reported
     * (2 parameters in the module).
     */
    @Test
    public void testRewrittenParametersReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(2, markers.size());
    }

    /**
     * Rewrites after the first use and by-reference parameters are not
     * reported.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 0, markers.size());
    }

    /** Reads in all expression contexts precede a rewrite; a method name is not a variable read. */
    @Test
    public void testReadsBeforeRewrite() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "rewrite-method-parameter-reads.bsl"); //$NON-NLS-1$
        assertEquals(List.of(37), markerLines());
    }

    /** Identity assignments do not consume the original value, as in the LS corpus. */
    @Test
    public void testIdentityAssignments() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "rewrite-method-parameter-self-assignment.bsl"); //$NON-NLS-1$
        assertEquals(List.of(7, 13), markerLines());
    }

    /** Editing an existing call argument removes and restores a marker without clean. */
    @Test
    public void testIncrementalReadUpdates() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "rewrite-method-parameter-incremental-bad.bsl"); //$NON-NLS-1$
        assertEquals(List.of(3), markerLines());
        updateModule(FOLDER_RESOURCE + "rewrite-method-parameter-incremental-clean.bsl"); //$NON-NLS-1$
        assertEquals(List.of(), markerLines());
        updateModule(FOLDER_RESOURCE + "rewrite-method-parameter-incremental-bad.bsl"); //$NON-NLS-1$
        assertEquals(List.of(3), markerLines());
    }

    private List<Integer> markerLines()
    {
        for (INode leaf : NodeModelUtils.findActualNodeFor(getModule()).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        return getModuleMarkers().stream().map(marker -> {
            Integer line = marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE);
            return line;
        }).sorted().toList();
    }
}
