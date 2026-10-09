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
 *     malikov-pro - port of the АПК rule АПК_00617
 *******************************************************************************/
package com.e1c.v8codestyle.form.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormField;
import com._1c.g5.v8.dt.form.model.InputFieldExtInfo;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.g5.v8.dt.testing.check.SingleProjectReadOnlyCheckTestBase;
import com.e1c.v8codestyle.form.check.ApkChoiceHistoryFieldButtonsCheck;

/**
 * Tests for {@link ApkChoiceHistoryFieldButtonsCheck} — the АПК_00617 rule port.
 * A form input field referring to a metadata object with disabled choice history
 * on input must have the drop-down list button off, the choice button on and the
 * choice button representation "in input field".
 *
 * @author malikov-pro
 */
public class ApkChoiceHistoryFieldButtonsCheckTest
    extends SingleProjectReadOnlyCheckTestBase
{

    private static final String CHECK_ID = ApkChoiceHistoryFieldButtonsCheck.CHECK_ID;

    private static final String PROJECT_NAME = "InputHistoryForm"; //$NON-NLS-1$

    private static final String FQN_HISTORY_FORM = "CommonForm.HistoryForm.Form"; //$NON-NLS-1$

    private static final String FQN_ITEM_FORM = "Catalog.Goods.Form.ItemForm.Form"; //$NON-NLS-1$

    @Override
    protected String getTestConfigurationName()
    {
        return PROJECT_NAME;
    }

    /**
     * A field referring to an object with disabled history and default button
     * properties is reported once.
     */
    @Test
    public void testBadItemIsReported()
    {
        InputFieldExtInfo extInfo = fieldExtInfo(FQN_HISTORY_FORM, "BadItem"); //$NON-NLS-1$
        List<Marker> markers = markers(extInfo);
        assertEquals(1, markers.size());
    }

    /**
     * A field referring to an object with disabled history and all button
     * properties explicitly set is clean.
     */
    @Test
    public void testGoodItemIsClean()
    {
        InputFieldExtInfo extInfo = fieldExtInfo(FQN_HISTORY_FORM, "GoodItem"); //$NON-NLS-1$
        assertEquals(0, markers(extInfo).size());
    }

    /**
     * A field referring to an object with the history not disabled is clean,
     * whatever the button properties are.
     */
    @Test
    public void testAutoHistoryItemIsClean()
    {
        InputFieldExtInfo extInfo = fieldExtInfo(FQN_HISTORY_FORM, "AutoHistoryItem"); //$NON-NLS-1$
        assertEquals(0, markers(extInfo).size());
    }

    /**
     * A field referring to an object with quick choice is clean (the standard
     * exception).
     */
    @Test
    public void testQuickChoiceItemIsClean()
    {
        InputFieldExtInfo extInfo = fieldExtInfo(FQN_HISTORY_FORM, "QuickChoiceItem"); //$NON-NLS-1$
        assertEquals(0, markers(extInfo).size());
    }

    /**
     * A field with the list choice mode and a choice list filled in metadata
     * is clean (the standard exception).
     */
    @Test
    public void testListModeItemIsClean()
    {
        InputFieldExtInfo extInfo = fieldExtInfo(FQN_HISTORY_FORM, "ListModeItem"); //$NON-NLS-1$
        assertEquals(0, markers(extInfo).size());
    }

    /**
     * A read-only field referring to an object with disabled history is clean,
     * as the element is not available for the value selection.
     */
    @Test
    public void testReadOnlyItemIsClean()
    {
        InputFieldExtInfo extInfo = fieldExtInfo(FQN_HISTORY_FORM, "ReadOnlyItem"); //$NON-NLS-1$
        assertEquals(0, markers(extInfo).size());
    }

    /**
     * In an object form the field bound to the reference attribute of the object
     * (multi-segment data path) with disabled history and default buttons is
     * reported once.
     */
    @Test
    public void testItemFormRefFieldIsReported()
    {
        InputFieldExtInfo extInfo = fieldExtInfo(FQN_ITEM_FORM, "RefField"); //$NON-NLS-1$
        List<Marker> markers = markers(extInfo);
        assertEquals(1, markers.size());
    }

    /**
     * In an object form the field bound to an attribute referring to an object
     * with the history not disabled is clean.
     */
    @Test
    public void testItemFormPartnerFieldIsClean()
    {
        InputFieldExtInfo extInfo = fieldExtInfo(FQN_ITEM_FORM, "PartnerField"); //$NON-NLS-1$
        assertEquals(0, markers(extInfo).size());
    }

    private List<Marker> markers(InputFieldExtInfo extInfo)
    {
        return getMarkersByCheckIds(Set.of(CHECK_ID), extInfo, getProject());
    }

    private InputFieldExtInfo fieldExtInfo(String formFqn, String fieldName)
    {
        IBmObject object = getTopObjectByFqn(formFqn, getProject());
        assertTrue(object instanceof Form);
        FormField field = ((Form)object).getItems()
            .stream()
            .filter(FormField.class::isInstance)
            .map(FormField.class::cast)
            .filter(item -> Objects.equals(fieldName, item.getName()))
            .findFirst()
            .orElseThrow();
        assertNotNull(field.getExtInfo());
        assertTrue(field.getExtInfo() instanceof InputFieldExtInfo);
        return (InputFieldExtInfo)field.getExtInfo();
    }
}
