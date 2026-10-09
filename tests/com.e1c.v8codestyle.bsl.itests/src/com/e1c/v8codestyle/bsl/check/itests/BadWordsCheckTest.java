/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: malikov-pro
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the BSL Language Server diagnostic BadWords
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.StandardExtraInfo;
import com._1c.g5.v8.dt.core.platform.IDtProject;
import com.e1c.g5.v8.dt.check.settings.CheckUid;
import com.e1c.g5.v8.dt.check.settings.ICheckSettings;
import com.e1c.v8codestyle.bsl.check.BadWordsCheck;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Tests for {@link BadWordsCheck} check.
 *
 * @author malikov-pro
 */
public class BadWordsCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String RESOURCE_VIOLATIONS = FOLDER_RESOURCE + "bad-words.bsl"; //$NON-NLS-1$

    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "bad-words-clean.bsl"; //$NON-NLS-1$

    private static final String TEST_BAD_WORDS = "лотус|шмотус"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public BadWordsCheckTest()
    {
        super(BadWordsCheck.class);
    }

    /**
     * Prohibited words are reported in a comment, in a string literal (each line of the query text
     * is a separate line) and inside identifiers; every occurrence on a line is reported separately.
     */
    @Test
    public void testViolationsReported() throws Exception
    {
        setBadWordsParameter(TEST_BAD_WORDS);
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream()
            .map(m -> String.format("[%s] %s", StandardExtraInfo.TEXT_LINE.get(m), m.getMessage()))
            .collect(Collectors.joining("; ")), 5, markers.size());
    }

    /**
     * A compliant module has no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {
        setBadWordsParameter(TEST_BAD_WORDS);
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 0, markers.size());
    }

    /**
     * With the default empty word list the check finds nothing, as in BSL LS.
     */
    @Test
    public void testEmptyWordListFindsNothing() throws Exception
    {
        setBadWordsParameter(""); //$NON-NLS-1$
        updateModule(RESOURCE_VIOLATIONS);

        List<Marker> markers = getModuleMarkers();
        assertEquals(markers.stream().map(Marker::getMessage).collect(Collectors.joining("; ")), 0, markers.size());
    }

    private void setBadWordsParameter(String value)
    {
        IDtProject dtProject = getProject();
        IProject project = dtProject.getWorkspaceProject();
        ICheckSettings settings =
            checkRepository.getSettings(new CheckUid(getCheckId(), BslPlugin.PLUGIN_ID), project);
        settings.getParameters().get(BadWordsCheck.PARAM_BAD_WORDS).setValue(value);
        checkRepository.applyChanges(Collections.singleton(settings), project);
        waitForDD(dtProject);
    }
}
