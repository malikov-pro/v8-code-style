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
package com.e1c.v8codestyle.ql.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.v8codestyle.internal.ql.CorePlugin;
import com.e1c.v8codestyle.ql.check.CompositeReferenceDereferenceCheck;

/** Product regression tests, independent of the API research assertions. */
public class CompositeReferenceDereferenceCheckTest
    extends AbstractQueryTestBase
{
    public CompositeReferenceDereferenceCheckTest()
    {
        super(CompositeReferenceDereferenceCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return "QlFullDemo"; //$NON-NLS-1$
    }

    /** @throws Exception if validation fails */
    @Test
    public void testConcreteBroadAndCleanReferences() throws Exception
    {
        var query = loadQueryAndValidate(FOLDER_RESOURCE + "ql-composite-references.ql"); //$NON-NLS-1$
        for (var leaf : NodeModelUtils.findActualNodeFor(query).getLeafNodes())
        {
            assertNull(leaf.getSyntaxErrorMessage());
        }
        var markers = getQueryMarkers();
        assertEquals(List.of(2, 3, 4, 5), markers.stream().map(marker -> marker.getLineNumber()).sorted().toList());
        String source = NodeModelUtils.findActualNodeFor(query).getRootNode().getText();
        for (var marker : markers)
        {
            assertEquals("Description", source.substring(marker.getOffset(), marker.getOffset() + marker.getLength())); //$NON-NLS-1$
        }
    }

    /** @throws Exception if validation fails */
    @Test
    public void testCleanAndUnknown() throws Exception
    {
        loadQueryAndValidate(FOLDER_RESOURCE + "ql-composite-clean.ql"); //$NON-NLS-1$
        assertEquals(0, getQueryMarkers().size());
    }

    /** @throws Exception if validation fails */
    @Test
    public void testVirtualTable() throws Exception
    {
        loadQueryAndValidate(FOLDER_RESOURCE + "ql-composite-virtual.ql"); //$NON-NLS-1$
        assertEquals(1, getQueryMarkers().size());
        assertEquals(2, getQueryMarkers().get(0).getLineNumber());
    }

    /** CAST only protects dereferencing after its argument, and overlapping types count once. */
    @Test
    public void testNestedCastAndOverlap() throws Exception
    {
        var query = loadQueryAndValidate(FOLDER_RESOURCE + "ql-composite-nested.ql"); //$NON-NLS-1$
        var markers = getQueryMarkers();
        assertEquals(List.of(2, 3), markers.stream().map(marker -> marker.getLineNumber()).sorted().toList());
        String source = NodeModelUtils.findActualNodeFor(query).getRootNode().getText();
        assertEquals(List.of("SingleTarget", "Description"), markers.stream().map(marker -> //$NON-NLS-1$ //$NON-NLS-2$
            source.substring(marker.getOffset(), marker.getOffset() + marker.getLength())).toList());
        assertTrue(markers.get(1).getMessage(), markers.get(1).getMessage().contains("3")); //$NON-NLS-1$
    }

    /** @throws Exception if validation fails */
    @Test
    public void testSubqueryProjection() throws Exception
    {
        loadQueryAndValidate(FOLDER_RESOURCE + "ql-composite-subquery.ql"); //$NON-NLS-1$
        assertEquals(1, getQueryMarkers().size());
        assertEquals(1, getQueryMarkers().get(0).getLineNumber());
    }

    /** A cast in a subquery restricts the type of the projected reference. */
    @Test
    public void testCastInSubquery() throws Exception
    {
        loadQueryAndValidate(FOLDER_RESOURCE + "ql-composite-subquery-clean.ql"); //$NON-NLS-1$
        assertEquals(0, getQueryMarkers().size());
    }

    /** @throws Exception if validation fails */
    @Test
    public void testThreshold() throws Exception
    {
        var project = getProject().getWorkspaceProject();
        var settings = checkRepository.getSettings(new CheckUid(CompositeReferenceDereferenceCheck.CHECK_ID,
            CorePlugin.PLUGIN_ID), project);
        var parameter = settings.getParameters().get(CompositeReferenceDereferenceCheck.PARAM_MIN_TYPES);
        String original = parameter.getValue();
        try
        {
            parameter.setValue("3"); //$NON-NLS-1$
            checkRepository.applyChanges(List.of(settings), project);
            waitForDD(getProject());
            loadQueryAndValidate(FOLDER_RESOURCE + "ql-composite-references.ql"); //$NON-NLS-1$
            assertEquals(List.of(3, 4, 5), getQueryMarkers().stream()
                .map(marker -> marker.getLineNumber()).sorted().toList());
        }
        finally
        {
            parameter.setValue(original);
            checkRepository.applyChanges(List.of(settings), project);
            waitForDD(getProject());
        }
    }
}
