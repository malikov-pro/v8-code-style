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
 *     malikov-pro - port of the upstream issue 1C-Company/v8-code-style#765 (standard 667)
 *******************************************************************************/
package com.e1c.v8codestyle.form.check.itests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Objects;

import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.form.model.Button;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormField;
import com._1c.g5.v8.dt.form.model.FormGroup;
import com._1c.g5.v8.dt.form.model.FormItem;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.form.check.FormItemExplicitColorCheck;

/**
 * Tests for {@link FormItemExplicitColorCheck} — concrete color values of
 * form items are reported (standard 667): button text color, item title text
 * color, field ext info back color and group ext info back color. A web color
 * reference (ColorRef) and an item without colors are clean.
 *
 * @author malikov-pro
 */
public class FormItemExplicitColorCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String CHECK_ID = "up-765-explicit-color"; //$NON-NLS-1$

    private static final String PROJECT_NAME = "FormItemColor"; //$NON-NLS-1$

    private static final String FQN_FORM = "CommonForm.ColorForm.Form"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * A button with a concrete text color value is reported.
     */
    @Test
    public void testButtonTextColorReported()
    {
        Form form = form();

        Button button = itemByName(form, "BadButton", Button.class); //$NON-NLS-1$
        assertNotNull(getFirstMarker(CHECK_ID, button, getProject()));
    }

    /**
     * A field with a concrete title text color value is reported.
     */
    @Test
    public void testTitleTextColorReported()
    {
        Form form = form();

        FormField bad = itemByName(form, "BadTitleField", FormField.class); //$NON-NLS-1$
        assertNotNull(getFirstMarker(CHECK_ID, bad, getProject()));
    }

    /**
     * A field whose ext info has a concrete back color value is reported on
     * the ext info; the field with a web color reference and the field without
     * colors are clean.
     */
    @Test
    public void testExtInfoBackColorReported()
    {
        Form form = form();

        FormField bad = itemByName(form, "BadField", FormField.class); //$NON-NLS-1$
        assertNotNull(bad.getExtInfo());
        assertNotNull(getFirstMarker(CHECK_ID, bad.getExtInfo(), getProject()));

        FormField web = itemByName(form, "WebField", FormField.class); //$NON-NLS-1$
        assertNotNull(web.getExtInfo());
        assertNull(getFirstMarker(CHECK_ID, web.getExtInfo(), getProject()));

        FormField ok = itemByName(form, "OkField", FormField.class); //$NON-NLS-1$
        assertNull(getFirstMarker(CHECK_ID, ok, getProject()));
    }

    /**
     * A group whose ext info has a concrete back color value is reported on
     * the ext info.
     */
    @Test
    public void testGroupBackColorReported()
    {
        Form form = form();

        FormGroup bad = itemByName(form, "BadGroup", FormGroup.class); //$NON-NLS-1$
        assertNotNull(bad.getExtInfo());
        assertNotNull(getFirstMarker(CHECK_ID, bad.getExtInfo(), getProject()));
    }

    private Form form()
    {
        IBmObject object = getTopObjectByFqn(FQN_FORM, getProject());
        assertTrue(object instanceof Form);
        return (Form)object;
    }

    private <T extends FormItem> T itemByName(Form form, String name, Class<T> type)
    {
        for (FormItem item : form.getItems())
        {
            if (type.isInstance(item) && Objects.equals(name, item.getName()))
            {
                return type.cast(item);
            }
        }
        throw new IllegalStateException("Item not found: " + name); //$NON-NLS-1$
    }
}
