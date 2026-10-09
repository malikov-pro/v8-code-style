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
 *     malikov-pro - port of the АПК rule АПК_01146
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.testing.check.CheckTestBase;
import com.e1c.v8codestyle.md.check.CommonPictureDuplicateCheck;

/**
 * Tests for {@link CommonPictureDuplicateCheck} — the АПК_01146 rule port.
 * Two common pictures with the identical image file are reported (one issue
 * per picture, the group has two members); a picture with the unique
 * content is clean.
 *
 * @author malikov-pro
 */
public class CommonPictureDuplicateCheckTest
    extends CheckTestBase
{

    private static final String CHECK_ID = CommonPictureDuplicateCheck.CHECK_ID;

    private static final String PROJECT_NAME = "DuplicatePictures"; //$NON-NLS-1$

    /**
     * Two common pictures sharing the same image file content are both
     * reported.
     *
     * @throws Exception the exception
     */
    @Test
    public void testDuplicatePicturesAreReported() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertEquals(1, markers(dtProject, "CommonPicture.ДубльКартинка1").size()); //$NON-NLS-1$
        assertEquals(1, markers(dtProject, "CommonPicture.ДубльКартинка2").size()); //$NON-NLS-1$
    }

    /**
     * A common picture with the unique content is clean.
     *
     * @throws Exception the exception
     */
    @Test
    public void testUniquePictureIsClean() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertEquals(0, markers(dtProject, "CommonPicture.УникальнаяКартинка").size()); //$NON-NLS-1$
    }

    private List<Marker> markers(IDtProject dtProject, String fqn)
    {
        long id = getTopObjectIdByFqn(fqn, dtProject);
        return getMarkersByCheckIds(Set.of(CHECK_ID), id, dtProject);
    }
}
