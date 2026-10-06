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
 *     malikov-pro - port of the APK rule 00137
 *******************************************************************************/
package com.e1c.v8codestyle.md.check.itests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.md.check.DbObjectCodeLengthCheck;

/**
 * Tests for {@link DbObjectCodeLengthCheck} check — port of the APK rule 00137.
 *
 * @author malikov-pro
 */
public class DbObjectCodeLengthCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String CHECK_ID = DbObjectCodeLengthCheck.CHECK_ID;

    private static final String PROJECT_NAME = "MdCodeLength"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * Objects with code (number) length out of the allowed series 0, 3, 5, 9, 11 have an issue.
     */
    @Test
    public void testBadCodeLength()
    {
        IDtProject dtProject = getProject();
        assertNotNull(dtProject);

        long id = getTopObjectIdByFqn("Catalog.BadCatalog", dtProject); //$NON-NLS-1$
        Marker marker = getFirstMarker(CHECK_ID, id, dtProject);
        assertNotNull(marker);

        id = getTopObjectIdByFqn("Document.BadDocument", dtProject); //$NON-NLS-1$
        marker = getFirstMarker(CHECK_ID, id, dtProject);
        assertNotNull(marker);

        id = getTopObjectIdByFqn("BusinessProcess.BadProcess", dtProject); //$NON-NLS-1$
        marker = getFirstMarker(CHECK_ID, id, dtProject);
        assertNotNull(marker);
    }

    /**
     * Objects with allowed code (number) length have no issues.
     */
    @Test
    public void testAllowedCodeLength()
    {
        IDtProject dtProject = getProject();
        assertNotNull(dtProject);

        for (String fqn : List.of("Catalog.OkCatalog", "Catalog.NoCodeCatalog", "Document.OkDocument", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "BusinessProcess.OkProcess")) //$NON-NLS-1$
        {
            long id = getTopObjectIdByFqn(fqn, dtProject);
            Marker marker = getFirstMarker(CHECK_ID, id, dtProject);
            assertNull(fqn, marker);
        }
    }
}
