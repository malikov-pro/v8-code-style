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
 *     malikov-pro - port of the BSL Language Server diagnostic DeletingCollectionItem
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.DeletingCollectionItemCheck;

/**
 * Tests for {@link DeletingCollectionItemCheck} — port of the BSL Language
 * Server diagnostic DeletingCollectionItem (deleting items of a collection
 * while iterating it).
 *
 * @author malikov-pro
 */
public class DeletingCollectionItemCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_DELETE = FOLDER_RESOURCE + "deleting-collection-item.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "deleting-collection-item-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public DeletingCollectionItemCheckTest()
    {
        super(DeletingCollectionItemCheck.class);
    }

    /**
     * Deleting an item of the iterated collection (directly and via a nested
     * source) is reported; deleting from another collection is not.
     */
    @Test
    public void testDeleteWhileIteratingReported() throws Exception
    {
        updateModule(RESOURCE_DELETE);

        List<Marker> markers = getModuleMarkers();
        assertEquals(2, markers.size());
    }

    /**
     * Iterating without deleting from the iterated collection produces no
     * issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(0, markers.size());
    }
}
