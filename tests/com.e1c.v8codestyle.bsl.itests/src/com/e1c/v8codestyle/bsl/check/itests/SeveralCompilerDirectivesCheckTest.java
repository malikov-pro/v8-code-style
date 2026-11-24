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
 *     malikov-pro - port of the BSL Language Server diagnostic SeveralCompilerDirectives
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com.e1c.v8codestyle.bsl.check.SeveralCompilerDirectivesCheck;

/**
 * Tests for {@link SeveralCompilerDirectivesCheck}: a second compiler directive
 * on a module variable or on a method is reported, extension annotations are not
 * counted.
 */
public class SeveralCompilerDirectivesCheckTest
    extends AbstractSingleModuleTestBase
{
    public SeveralCompilerDirectivesCheckTest()
    {
        super(SeveralCompilerDirectivesCheck.class);
    }

    @Test
    public void testSeveralDirectives() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "several-compiler-directives.bsl"); //$NON-NLS-1$

        IDtProject dtProject = getProject();
        String id = getModule().eResource().getURI().toPlatformString(true);
        Marker[] markers = markerManager.getMarkers(dtProject.getWorkspaceProject(), id);
        assertNotNull(markers);

        Set<Integer> reported = new TreeSet<>();
        for (Marker marker : markers)
        {
            if (!getCheckId().equals(getCheckIdFromMarker(marker, dtProject)))
            {
                continue;
            }
            reported.add(marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE));
        }
        assertEquals(Set.of(6, 15), reported);

        cleanUpProject(dtProject.getWorkspaceProject());
    }
}
