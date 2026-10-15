/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import java.util.List;

import org.junit.Test;

import com.e1c.v8codestyle.bsl.check.TryNumberCheck;

public class TryNumberCheckTest
    extends AbstractLocalCallTestBase
{
    public TryNumberCheckTest()
    {
        super(TryNumberCheck.class);
    }

    @Test
    public void testNestedTryAndExceptScope() throws Exception
    {
        assertDefaults(com.e1c.g5.v8.dt.check.settings.IssueSeverity.MAJOR);
        updateModule(FOLDER_RESOURCE + "try-number-corpus.bsl"); //$NON-NLS-1$
        assertLines(List.of(3, 6, 8, 17));
    }

    @Test
    public void testIncrementalConversionChange() throws Exception
    {
        assertIncremental("try-number", 3); //$NON-NLS-1$
    }
}
