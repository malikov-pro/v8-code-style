/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.DeprecatedTypeManagedFormCheck;

/** Pinned LS corpus: legacy type name in Type() calls only. */
public class DeprecatedTypeManagedFormCheckTest
    extends AbstractSingleModuleTestBase
{
    public DeprecatedTypeManagedFormCheckTest()
    {
        super(DeprecatedTypeManagedFormCheck.class);
    }

    @Test
    public void testCorpus() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "deprecated-type-managed-form.bsl"); //$NON-NLS-1$
        INode node = NodeModelUtils.findActualNodeFor(getModule());
        assertEquals(2, getModuleMarkers().size());
    }

    @Test
    public void testIncrementalUpdates() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "deprecated-type-managed-form-clean.bsl"); //$NON-NLS-1$
        assertEquals(0, getModuleMarkers().size());
        updateModule(FOLDER_RESOURCE + "deprecated-type-managed-form.bsl"); //$NON-NLS-1$
        assertEquals(2, getModuleMarkers().size());
        updateModule(FOLDER_RESOURCE + "deprecated-type-managed-form-clean.bsl"); //$NON-NLS-1$
        assertEquals(0, getModuleMarkers().size());
    }
}
