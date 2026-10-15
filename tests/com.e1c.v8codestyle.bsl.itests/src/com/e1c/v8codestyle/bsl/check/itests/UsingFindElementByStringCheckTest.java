/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import java.util.List;

import org.junit.Test;

import com.e1c.v8codestyle.bsl.check.UsingFindElementByStringCheck;

public class UsingFindElementByStringCheckTest
    extends AbstractLocalCallTestBase
{
    public UsingFindElementByStringCheckTest()
    {
        super(UsingFindElementByStringCheck.class);
    }

    @Test
    public void testFirstArgumentTokensAndReceiver() throws Exception
    {
        assertDefaults(com.e1c.g5.v8.dt.check.settings.IssueSeverity.MAJOR);
        updateModule(FOLDER_RESOURCE + "find-literal-corpus.bsl"); //$NON-NLS-1$
        assertLines(List.of(2, 3, 4, 5, 9, 13));
    }

    @Test
    public void testIncrementalArgumentReplacement() throws Exception
    {
        assertIncremental("find-literal", 2); //$NON-NLS-1$
    }

    @Test
    public void testNestedReceiversAndArgumentTokens() throws Exception
    {
        updateModule(FOLDER_RESOURCE + "local-call-boundaries.bsl"); //$NON-NLS-1$
        assertLines(List.of(2, 5, 6, 7));
    }
}
