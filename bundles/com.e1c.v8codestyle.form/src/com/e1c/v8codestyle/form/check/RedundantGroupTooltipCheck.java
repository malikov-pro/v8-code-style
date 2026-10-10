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
package com.e1c.v8codestyle.form.check;

import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.FORM;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.FORM_GROUP;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.FORM_GROUP__TYPE;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.TITLED__TITLE;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.TOOLTIP_CONTAINER__TOOL_TIP;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.NAMED_ELEMENT__NAME;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.LOCAL_STRING_MAP_ENTRY;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.LOCAL_STRING_MAP_ENTRY__KEY;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.LOCAL_STRING_MAP_ENTRY__VALUE;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.dt.form.model.AutoCommandBar;
import com._1c.g5.v8.dt.form.model.ButtonGroupExtInfo;
import com._1c.g5.v8.dt.form.model.CommandBarExtInfo;
import com._1c.g5.v8.dt.form.model.ContextMenu;
import com._1c.g5.v8.dt.form.model.FormGroup;
import com._1c.g5.v8.dt.form.model.FormItemContainer;
import com._1c.g5.v8.dt.form.model.FormPackage;
import com._1c.g5.v8.dt.form.model.ManagedFormGroupType;
import com._1c.g5.v8.dt.form.model.PopupGroupExtInfo;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckDefinition;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.components.IBasicCheckExtension;
import com.e1c.g5.v8.dt.check.context.OnModelFeatureChangeContextCollector;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.form.CorePlugin;

/** APK group tooltip invariant; localized titles are compared only within the same language. */
public class RedundantGroupTooltipCheck
    extends BasicCheck
{
    /** Source rule identifier. */
    public static final String CHECK_ID = "apk-00136-redundant-group-tooltip"; //$NON-NLS-1$

    private static final String REMOVED_CHARACTERS = " ,.:;\"'!?%$#№@`~|&^\\/-_*()<>{}[]\t\n"; //$NON-NLS-1$

    private static final Set<ManagedFormGroupType> GROUP_TYPES = Set.of(ManagedFormGroupType.USUAL_GROUP,
        ManagedFormGroupType.PAGES, ManagedFormGroupType.PAGE, ManagedFormGroupType.COMMAND_BAR,
        ManagedFormGroupType.POPUP, ManagedFormGroupType.BUTTON_GROUP, ManagedFormGroupType.COLUMN_GROUP);

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.RedundantGroupTooltipCheck_title)
            .description(Messages.RedundantGroupTooltipCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.UI_STYLE)
            .extension(new CommonSenseCheckExtension(CHECK_ID, CorePlugin.PLUGIN_ID))
            .extension(new ParentSettingsChangeExtension())
            .extension(new LocalizedValueChangeExtension())
            .topObject(FORM)
            .containment(FORM_GROUP)
            .features(TOOLTIP_CONTAINER__TOOL_TIP, TITLED__TITLE, NAMED_ELEMENT__NAME, FORM_GROUP__TYPE);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAcceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        FormGroup group = (FormGroup)object;
        if (monitor.isCanceled() || group.getType() == null || !GROUP_TYPES.contains(group.getType())
            || skipParent(group.eContainer()))
        {
            return;
        }
        String name = normalize(group.getName());
        for (var hint : group.getToolTip().entrySet())
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (hint.getValue() == null || hint.getValue().isBlank())
            {
                continue;
            }
            String normalized = normalize(hint.getValue());
            // APK compares normalized empty values too, e.g. "Группа" vs an empty title.
            String title = normalize(group.getTitle().get(hint.getKey()));
            if (normalized.equals(name) || normalized.equals(title))
            {
                resultAcceptor.addIssue(MessageFormat.format(Messages.RedundantGroupTooltipCheck_message,
                    group.getName(), hint.getKey()), group, TOOLTIP_CONTAINER__TOOL_TIP);
                break;
            }
        }
    }

    private static boolean skipParent(EObject parent)
    {
        if (parent == null || parent instanceof AutoCommandBar bar && bar.isAutoFill()
            || parent instanceof ContextMenu menu && menu.isAutoFill())
        {
            return true;
        }
        if (parent instanceof FormGroup group)
        {
            return group.getExtInfo() instanceof CommandBarExtInfo bar && bar.getCommandSource() != null
                || group.getExtInfo() instanceof PopupGroupExtInfo popup && popup.getCommandSource() != null
                || group.getExtInfo() instanceof ButtonGroupExtInfo buttons && buttons.getCommandSource() != null;
        }
        return false;
    }

    private static String normalize(String text)
    {
        if (text == null)
        {
            return ""; //$NON-NLS-1$
        }
        String upper = text.toUpperCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < upper.length(); i++)
        {
            char character = upper.charAt(i);
            if (REMOVED_CHARACTERS.indexOf(character) < 0)
            {
                result.append(character);
            }
        }
        return result.toString().replace("ГРУППА", ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /** A parent's exemptions affect its immediate children without changing their own properties. */
    private static final class ParentSettingsChangeExtension
        implements IBasicCheckExtension
    {
        @Override
        public void configureContextCollector(ICheckDefinition definition)
        {
            OnModelFeatureChangeContextCollector collector = (object, feature, event, session) -> {
                if (feature != FormPackage.Literals.AUTO_COMMAND_BAR__AUTO_FILL
                    && feature != FormPackage.Literals.CONTEXT_MENU__AUTO_FILL
                    && feature != FormPackage.Literals.COMMAND_BAR_EXT_INFO__COMMAND_SOURCE
                    && feature != FormPackage.Literals.POPUP_GROUP_EXT_INFO__COMMAND_SOURCE
                    && feature != FormPackage.Literals.BUTTON_GROUP_EXT_INFO__COMMAND_SOURCE
                    && feature != FormPackage.Literals.FORM_GROUP__EXT_INFO
                    && feature != FormPackage.Literals.FORM_GROUP__TYPE)
                {
                    return;
                }
                EObject parent = object instanceof FormItemContainer ? object : object.eContainer();
                if (parent instanceof FormItemContainer container)
                {
                    for (var child : container.getItems())
                    {
                        if (child instanceof FormGroup group)
                        {
                            session.addModelCheck(group);
                        }
                    }
                }
            };
            definition.addGenericModelFeatureChangeContextCollector(collector,
                FormPackage.Literals.AUTO_COMMAND_BAR, FORM);
            definition.addGenericModelFeatureChangeContextCollector(collector, FormPackage.Literals.CONTEXT_MENU, FORM);
            definition.addGenericModelFeatureChangeContextCollector(collector, FORM_GROUP, FORM);
            definition.addGenericModelFeatureChangeContextCollector(collector,
                FormPackage.Literals.COMMAND_BAR_EXT_INFO, FORM);
            definition.addGenericModelFeatureChangeContextCollector(collector,
                FormPackage.Literals.POPUP_GROUP_EXT_INFO, FORM);
            definition.addGenericModelFeatureChangeContextCollector(collector,
                FormPackage.Literals.BUTTON_GROUP_EXT_INFO, FORM);
        }
    }

    /** Replacing an existing language value changes the map entry, not the containing map feature. */
    private static final class LocalizedValueChangeExtension
        implements IBasicCheckExtension
    {
        @Override
        public void configureContextCollector(ICheckDefinition definition)
        {
            OnModelFeatureChangeContextCollector collector = (object, feature, event, session) -> {
                if ((feature == LOCAL_STRING_MAP_ENTRY__VALUE || feature == LOCAL_STRING_MAP_ENTRY__KEY)
                    && object.eContainer() instanceof FormGroup group
                    && (object.eContainingFeature() == TOOLTIP_CONTAINER__TOOL_TIP
                        || object.eContainingFeature() == TITLED__TITLE))
                {
                    session.addModelCheck(group);
                }
            };
            definition.addGenericModelFeatureChangeContextCollector(collector, LOCAL_STRING_MAP_ENTRY, FORM);
        }
    }
}
