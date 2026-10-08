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
 *     malikov-pro - port of the BSL Language Server diagnostic CreateQueryInCycle
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.CreateQueryInCycleCheck;

/**
 * Tests for {@link CreateQueryInCycleCheck} check.
 *
 * @author malikov-pro
 */
public class CreateQueryInCycleCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "create-query-in-cycle.bsl"; //$NON-NLS-1$

    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "create-query-in-cycle-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public CreateQueryInCycleCheckTest()
    {
        super(CreateQueryInCycleCheck.class);
    }

    /**
     * Query or builder executed in a loop is reported: query created before the loop,
     * query created inside the loop, While loop, infinite While, While predicate,
     * query and report builders, iterator expression of a nested loop.
     */
    @Test
    public void testViolationsReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream()
            .map(m -> String.format("[%s] %s", StandardExtraInfo.TEXT_LINE.get(m), m.getMessage()))
            .collect(Collectors.joining("; ")), 8, markers.size());
    }

    /**
     * A compliant module (query executed outside loops, top-level loop iterator expression,
     * loop calling a method that executes a query) has no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 0, markers.size());
    }
}
