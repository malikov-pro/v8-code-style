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
 *     malikov-pro - port of the upstream issue 1C-Company/v8-code-style#1353
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.metadata.mdclass.AbstractForm;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.bsl.check.FormModuleParametersParameterCheck;

/**
 * Tests for {@link FormModuleParametersParameterCheck} — a method of a form
 * module shall not declare a parameter named «Параметры»/"Parameters".
 *
 * @author malikov-pro
 */
public class FormModuleParametersParameterCheckTest
    extends AbstractSingleModuleTestBase
{

    private static final String PROJECT_NAME = "CommonForm"; //$NON-NLS-1$

    private static final String FQN = "CommonForm.Form.Form"; //$NON-NLS-1$

    private static final String COMMON_FORM_FILE_NAME = "/src/CommonForms/Form/Module.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public FormModuleParametersParameterCheckTest()
    {
        super(FormModuleParametersParameterCheck.class);
    }

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected String getModuleFileName()
    {
        return COMMON_FORM_FILE_NAME;
    }

    @Override
    protected Module getModule()
    {
        IBmObject mdObject = getTopObjectByFqn(FQN, getProject());
        assertTrue(mdObject instanceof AbstractForm);
        Module module = ((AbstractForm)mdObject).getModule();
        assertNotNull(module);

        return module;
    }

    /**
     * Method parameters named "Parameters" (English and Russian, any case)
     * are reported — one issue per parameter.
     */
    @Test
    public void testParameterNamedParametersIsReported() throws Exception
    {
        updateAndGetModule(FOLDER_RESOURCE + "up-1353-form-parameters-param.bsl"); //$NON-NLS-1$

        List<Marker> markers = getModuleMarkers();
        assertEquals(2, markers.size());
    }

    /**
     * Parameters with other names (including names starting with
     * "Parameters"/«Параметры») are not reported.
     */
    @Test
    public void testOtherParameterNamesAreClean() throws Exception
    {
        updateAndGetModule(FOLDER_RESOURCE + "up-1353-form-parameters-param-clean.bsl"); //$NON-NLS-1$

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }
}
