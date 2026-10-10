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
 *     malikov-pro - port of the APK check АПК_00136
 *******************************************************************************/
package com.e1c.v8codestyle.form.check.itests;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.junit.Test;

import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.form.model.CommandBarExtInfo;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormFactory;
import com._1c.g5.v8.dt.form.model.FormGroup;
import com._1c.g5.v8.dt.form.model.ManagedFormGroupType;
import com.e1c.g5.v8.dt.testing.check.CheckTestBase;
import com.e1c.v8codestyle.form.check.RedundantGroupTooltipCheck;

/** Real BM form validation of exact normalization and platform exceptions. */
public class RedundantGroupTooltipCheckTest
    extends CheckTestBase
{
    private static final AtomicInteger ITEM_ID = new AtomicInteger(2000);

    /** @throws Exception if validation fails */
    @Test
    public void testGroupHintsAndRemoval() throws Exception
    {
        var project = openProjectAndWaitForValidationFinish("FormFrenchQuotes"); //$NON-NLS-1$
        long id = getTopObjectIdByFqn("CommonForm.FrenchQuotesForm.Form", project); //$NON-NLS-1$
        bmModelManager.getModel(project).execute(new AbstractBmTask<Void>("Create tooltip matrix") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                Form form = (Form)transaction.getObjectById(id);
                form.getItems().add(group("ГруппаТовары", "Другой", "Товары!")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                form.getItems().add(group("ByTitle", "Сведения", "Сведения.")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                form.getItems().add(group("Informative", "Товары", "Выберите товары для заказа")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                form.getItems().add(group("Unicode", "Товары", "Товары—")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                form.getItems().add(group("EnglishWord", "Goods", "Group Goods")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                form.getItems().add(group("Blank", "", "  \t")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                FormGroup multilingual = group("Language", "Other", "Same"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                multilingual.getTitle().put("en", "Same"); //$NON-NLS-1$ //$NON-NLS-2$
                form.getItems().add(multilingual); // RU hint must not match EN title.
                FormGroup parent = group("Commands", "Commands", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                parent.setType(ManagedFormGroupType.COMMAND_BAR);
                var extInfo = FormFactory.eINSTANCE.createCommandBarExtInfo();
                extInfo.setCommandSource(form);
                parent.setExtInfo(extInfo);
                parent.getItems().add(popup("SkippedCommands", "Title", "Title")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                form.getItems().add(parent);
                form.getAutoCommandBar().setAutoFill(true);
                form.getAutoCommandBar().getItems().add(popup("SkippedAuto", "Title", "Title")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                return null;
            }
        });
        waitForDD(project);
        assertEquals("Test form must not contain an invalid child type", 0, //$NON-NLS-1$
            Arrays.stream(markerManager.getMarkers(project.getWorkspaceProject(), id))
                .filter(marker -> "Unsupported child element type".equals(marker.getMessage())).count()); //$NON-NLS-1$
        assertGroups(project, id, Set.of("ГруппаТовары", "ByTitle")); //$NON-NLS-1$ //$NON-NLS-2$
        bmModelManager.getModel(project).execute(new AbstractBmTask<Void>("Edit existing tooltip value") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                Form form = (Form)transaction.getObjectById(id);
                FormGroup group = (FormGroup)form.getItems().stream()
                    .filter(item -> "ByTitle".equals(item.getName())).findFirst().orElseThrow(); //$NON-NLS-1$
                group.getToolTip().put("ru", "Пояснение к документу"); //$NON-NLS-1$ //$NON-NLS-2$
                return null;
            }
        });
        waitForDD(project);
        assertGroups(project, id, Set.of("ГруппаТовары")); //$NON-NLS-1$
        bmModelManager.getModel(project).execute(new AbstractBmTask<Void>("Restore existing tooltip value") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                Form form = (Form)transaction.getObjectById(id);
                FormGroup group = (FormGroup)form.getItems().stream()
                    .filter(item -> "ByTitle".equals(item.getName())).findFirst().orElseThrow(); //$NON-NLS-1$
                group.getToolTip().put("ru", "Сведения."); //$NON-NLS-1$ //$NON-NLS-2$
                return null;
            }
        });
        waitForDD(project);
        assertGroups(project, id, Set.of("ГруппаТовары", "ByTitle")); //$NON-NLS-1$ //$NON-NLS-2$
        bmModelManager.getModel(project).execute(new AbstractBmTask<Void>("Edit existing title value") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                Form form = (Form)transaction.getObjectById(id);
                FormGroup group = (FormGroup)form.getItems().stream()
                    .filter(item -> "ByTitle".equals(item.getName())).findFirst().orElseThrow(); //$NON-NLS-1$
                group.getTitle().put("ru", "Другие сведения"); //$NON-NLS-1$ //$NON-NLS-2$
                return null;
            }
        });
        waitForDD(project);
        assertGroups(project, id, Set.of("ГруппаТовары")); //$NON-NLS-1$
        bmModelManager.getModel(project).execute(new AbstractBmTask<Void>("Restore existing title value") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                Form form = (Form)transaction.getObjectById(id);
                FormGroup group = (FormGroup)form.getItems().stream()
                    .filter(item -> "ByTitle".equals(item.getName())).findFirst().orElseThrow(); //$NON-NLS-1$
                group.getTitle().put("ru", "Сведения"); //$NON-NLS-1$ //$NON-NLS-2$
                return null;
            }
        });
        waitForDD(project);
        assertGroups(project, id, Set.of("ГруппаТовары", "ByTitle")); //$NON-NLS-1$ //$NON-NLS-2$
        bmModelManager.getModel(project).execute(new AbstractBmTask<Void>("Remove parent exemptions") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                Form form = (Form)transaction.getObjectById(id);
                form.getAutoCommandBar().setAutoFill(false);
                FormGroup parent = (FormGroup)form.getItems().stream()
                    .filter(item -> "Commands".equals(item.getName())).findFirst().orElseThrow(); //$NON-NLS-1$
                ((CommandBarExtInfo)parent.getExtInfo()).setCommandSource(null);
                return null;
            }
        });
        waitForDD(project);
        assertGroups(project, id, Set.of("ГруппаТовары", "ByTitle", "SkippedCommands", "SkippedAuto")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        bmModelManager.getModel(project).execute(new AbstractBmTask<Void>("Restore parent exemptions") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                Form form = (Form)transaction.getObjectById(id);
                form.getAutoCommandBar().setAutoFill(true);
                FormGroup parent = (FormGroup)form.getItems().stream()
                    .filter(item -> "Commands".equals(item.getName())).findFirst().orElseThrow(); //$NON-NLS-1$
                ((CommandBarExtInfo)parent.getExtInfo()).setCommandSource(form);
                return null;
            }
        });
        waitForDD(project);
        assertGroups(project, id, Set.of("ГруппаТовары", "ByTitle")); //$NON-NLS-1$ //$NON-NLS-2$
        bmModelManager.getModel(project).execute(new AbstractBmTask<Void>("Clear tooltips") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                Form form = (Form)transaction.getObjectById(id);
                for (var group : EcoreUtil2.getAllContentsOfType(form, FormGroup.class))
                {
                    group.getToolTip().clear();
                }
                return null;
            }
        });
        waitForDD(project);
        assertGroups(project, id, Set.of());
    }

    private void assertGroups(IDtProject project, long id, Set<String> names)
    {
        var markers = getMarkersByCheckIds(Set.of(RedundantGroupTooltipCheck.CHECK_ID), null, project,
            markerManager.getNestedMarkers(project.getWorkspaceProject(), id));
        assertEquals(names.size(), markers.size());
        for (String name : names)
        {
            assertEquals(name, 1, markers.stream().filter(marker -> marker.getMessage().contains(name)).count());
        }
    }

    /** Empty normalized text and English titles preserve the source comparison semantics. */
    @Test
    public void testNormalizationAndLanguages() throws Exception
    {
        var project = openProjectAndWaitForValidationFinish("FormFrenchQuotes"); //$NON-NLS-1$
        long id = getTopObjectIdByFqn("CommonForm.FrenchQuotesForm.Form", project); //$NON-NLS-1$
        bmModelManager.getModel(project).execute(new AbstractBmTask<Void>("Create language matrix") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, IProgressMonitor monitor)
            {
                Form form = (Form)transaction.getObjectById(id);
                form.getItems().add(group("EmptyNormalized", "", "Группа!?")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                form.getItems().add(group("Punctuation", "Товары", "Группа_то:ва;ры\t\n")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                form.getItems().add(group("Nbsp", "Товары", "Товары\u00a0")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                FormGroup english = group("EnglishMatched", "Другое", "Полезный текст"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                english.getTitle().put("en", "Details"); //$NON-NLS-1$ //$NON-NLS-2$
                english.getToolTip().put("en", "details!"); //$NON-NLS-1$ //$NON-NLS-2$
                form.getItems().add(english);
                return null;
            }
        });
        waitForDD(project);
        assertGroups(project, id, Set.of("EmptyNormalized", "Punctuation", "EnglishMatched")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    private static FormGroup group(String name, String title, String hint)
    {
        FormGroup group = FormFactory.eINSTANCE.createFormGroup();
        group.setId(ITEM_ID.getAndIncrement());
        group.setName(name);
        group.setType(ManagedFormGroupType.USUAL_GROUP);
        group.setExtInfo(FormFactory.eINSTANCE.createUsualGroupExtInfo());
        group.getTitle().put("ru", title); //$NON-NLS-1$
        group.getToolTip().put("ru", hint); //$NON-NLS-1$
        return group;
    }

    private static FormGroup popup(String name, String title, String hint)
    {
        FormGroup group = group(name, title, hint);
        group.setType(ManagedFormGroupType.POPUP);
        group.setExtInfo(FormFactory.eINSTANCE.createPopupGroupExtInfo());
        return group;
    }
}
