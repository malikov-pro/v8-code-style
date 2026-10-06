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
 *     malikov-pro - port of the АПК rule АПК_01195
 *******************************************************************************/
package com.e1c.v8codestyle.form.check.itests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Objects;

import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormAttribute;
import com._1c.g5.v8.dt.form.model.FormCommand;
import com._1c.g5.v8.dt.form.model.FormField;
import com._1c.g5.v8.dt.form.model.FormItem;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.form.check.FormItemFrenchQuotesCheck;

/**
 * Tests for {@link FormItemFrenchQuotesCheck} — the АПК_01195 rule port.
 * French quotes (guillemets) in the form title, in titles/tooltips of form
 * items, in titles of form attributes and in titles/tooltips of form commands
 * are reported; straight double quotes are clean.
 *
 * @author malikov-pro
 */
public class FormItemFrenchQuotesCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String CHECK_ID = "apk-01195-french-quotes-form"; //$NON-NLS-1$

    private static final String PROJECT_NAME = "FormFrenchQuotes"; //$NON-NLS-1$

    private static final String FQN_FORM = "CommonForm.FrenchQuotesForm.Form"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * The form title containing French quotes is reported.
     */
    @Test
    public void testFormTitleReported()
    {
        IBmObject object = getTopObjectByFqn(FQN_FORM, getProject());
        assertTrue(object instanceof Form);

        Marker marker = getFirstMarker(CHECK_ID, object, getProject());
        assertNotNull(marker);
    }

    /**
     * The form item title containing French quotes is reported, the clean item is not.
     */
    @Test
    public void testFormItemTitleReported()
    {
        Form form = form();

        FormField bad = fieldByName(form, "BadField"); //$NON-NLS-1$
        assertNotNull(getFirstMarker(CHECK_ID, bad, getProject()));

        FormField ok = fieldByName(form, "OkField"); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, ok, getProject()));
    }

    /**
     * The form item tooltip containing French quotes is reported.
     */
    @Test
    public void testFormItemTooltipReported()
    {
        Form form = form();

        FormField bad = fieldByName(form, "TooltipField"); //$NON-NLS-1$
        assertNotNull(getFirstMarker(CHECK_ID, bad, getProject()));
    }

    /**
     * The form attribute title containing French quotes is reported.
     */
    @Test
    public void testFormAttributeTitleReported()
    {
        Form form = form();

        FormAttribute bad = form.getAttributes()
            .stream()
            .filter(attribute -> Objects.equals("BadAttribute", attribute.getName())) //$NON-NLS-1$
            .findFirst()
            .orElseThrow();
        assertNotNull(getFirstMarker(CHECK_ID, bad, getProject()));
    }

    /**
     * The title and the tooltip of form commands containing French quotes are reported.
     */
    @Test
    public void testFormCommandReported()
    {
        Form form = form();

        FormCommand titled = form.getFormCommands()
            .stream()
            .filter(command -> Objects.equals("BadCommand", command.getName())) //$NON-NLS-1$
            .findFirst()
            .orElseThrow();
        assertNotNull(getFirstMarker(CHECK_ID, titled, getProject()));

        FormCommand withTooltip = form.getFormCommands()
            .stream()
            .filter(command -> Objects.equals("TooltipCommand", command.getName())) //$NON-NLS-1$
            .findFirst()
            .orElseThrow();
        assertNotNull(getFirstMarker(CHECK_ID, withTooltip, getProject()));
    }

    private Form form()
    {
        IBmObject object = getTopObjectByFqn(FQN_FORM, getProject());
        assertTrue(object instanceof Form);
        return (Form)object;
    }

    private FormField fieldByName(Form form, String name)
    {
        for (FormItem item : form.getItems())
        {
            if (item instanceof FormField field && Objects.equals(name, field.getName()))
            {
                return field;
            }
        }
        throw new IllegalStateException("Field not found: " + name); //$NON-NLS-1$
    }
}
