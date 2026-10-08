/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     malikov-pro - port of the APK check АПК_00157
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.ApkConstantWriteInTransactionCheck;

/**
 * Tests for {@link ApkConstantWriteInTransactionCheck} — port of the APK check АПК_00157
 * (standard 632: constants are written outside transactions).
 *
 * @author malikov-pro
 */
public class ApkConstantWriteInTransactionCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "apk-00157-constants-write-in-txn.bsl"; //$NON-NLS-1$

    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "apk-00157-constants-write-in-txn-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public ApkConstantWriteInTransactionCheckTest()
    {
        super(ApkConstantWriteInTransactionCheck.class);
    }

    /**
     * Constant writes between BeginTransaction and CommitTransaction of the same method are reported:
     * Set and Write in Russian syntax and Set in English syntax. A write outside a transaction is not reported.
     */
    @Test
    public void testConstantWriteInTransactionReported() throws Exception
    {
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 3, markers.size()); //$NON-NLS-1$

        Set<Integer> lines = markers.stream()
            .map(marker -> marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE))
            .filter(Integer.class::isInstance)
            .map(Integer.class::cast)
            .collect(Collectors.toSet());
        assertEquals(Set.of(7, 17, 25), lines);
    }

    /**
     * Constant reads inside a transaction, constant writes outside a transaction and writes
     * of other objects (record sets) inside a transaction are not reported.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 0, markers.size()); //$NON-NLS-1$
    }
}
