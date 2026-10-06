/*******************************************************************************
 * Copyright (C) 2022, 1C-Soft LLC and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     1C-Soft LLC - initial API and implementation
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Collections;

import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com.e1c.g5.v8.dt.testing.check.CheckTestBase;
import com.e1c.v8codestyle.internal.md.CorePlugin;
import com.e1c.v8codestyle.md.check.MdObjectNameUnallowedLetterCheck;

/**
 * Tests for {@link MdObjectNameUnallowedLetterCheck} check
 *
 * @author OlgaBozhko
 *
 */
public class MdObjectNameUnallowedLetterCheckTest
    extends CheckTestBase
{
    private static final String CHECK_ID = "mdo-ru-name-unallowed-letter"; //$NON-NLS-1$
    private static final String PROJECT_NAME = "MdObjectNameUnallowedLetter";

    /**
     * The check is disabled by default since 0.8.1: the invariant is
     * superseded by the wider {@code apk-00126-md-no-yo-letter} check.
     * Tests enable it explicitly and re-run validation.
     */
    private void enableCheck(IDtProject dtProject)
    {
        CheckUid checkUid = new CheckUid(CHECK_ID, CorePlugin.PLUGIN_ID);
        ICheckSettings settings = checkRepository.getSettings(checkUid,
            dtProject.getWorkspaceProject());
        if (!settings.isEnabled())
        {
            settings.setEnabled(true);
            checkRepository.applyChanges(Collections.singleton(settings),
                dtProject.getWorkspaceProject());
            waitForDD(dtProject);
        }
    }

    /**
     * Test that md object name, synonym and comment do not contain unallowed letter "ё" (Ru locale)
     *
     * @throws Exception the exception
     */
    @Test
    public void testMdObjectNameNoUnallowedLetter() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertNotNull(dtProject);

        long id = getTopObjectIdByFqn("Catalog.ТестовыйКаталог", dtProject);
        Marker marker = getFirstMarker(CHECK_ID, id, dtProject);
        assertNull(marker);
    }

    /**
     * Test that md object name contains unallowed letter "ё" (Ru locale)
     *
     * @throws Exception the exception
     */
    @Test
    public void testMdObjectNameHasUnallowedLetter() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertNotNull(dtProject);
        enableCheck(dtProject);

        long id = getTopObjectIdByFqn("Catalog.ТестовыйКаталог_ё_имя", dtProject);
        Marker marker = getFirstMarker(CHECK_ID, id, dtProject);
        assertNotNull(marker);
    }

    /**
     * Test that md object synonym contains unallowed letter "ё" (Ru locale)
     *
     * @throws Exception the exception
     */
    @Test
    public void testMdObjectSynonymHasUnallowedLetter() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertNotNull(dtProject);
        enableCheck(dtProject);

        long id = getTopObjectIdByFqn("Catalog.ТестовыйКаталог_синоним", dtProject);
        Marker marker = getFirstMarker(CHECK_ID, id, dtProject);
        assertNotNull(marker);
    }

    /**
     * Test that md object comment contains unallowed letter "ё" (Ru locale)
     *
     * @throws Exception the exception
     */
    @Test
    public void testMdObjectCommentHasUnallowedLetter() throws Exception
    {
        IDtProject dtProject = openProjectAndWaitForValidationFinish(PROJECT_NAME);
        assertNotNull(dtProject);
        enableCheck(dtProject);

        long id = getTopObjectIdByFqn("Catalog.ТестовыйКаталог_комментарий", dtProject);
        Marker marker = getFirstMarker(CHECK_ID, id, dtProject);
        assertNotNull(marker);
    }
}
