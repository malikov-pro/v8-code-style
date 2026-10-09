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
package com.e1c.v8codestyle.form.check;

import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.ADDITION_EXT_INFO;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.BUTTON__BACK_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.BUTTON__TEXT_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.CHECK_BOX_FIELD_EXT_INFO__BACK_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.CHECK_BOX_FIELD_EXT_INFO__TEXT_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.COLUMN_GROUP_EXT_INFO__TITLE_BACK_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.DATA_ITEM;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.DATA_ITEM__TITLE_BACK_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.DECORATION;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.DECORATION__TEXT_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.DECORATION_EXT_INFO;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.FIELD_EXT_INFO;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.FORM;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.FORMATTED_DOC_FIELD_EXT_INFO__BACK_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.FORMATTED_DOC_FIELD_EXT_INFO__TEXT_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.FORM_FIELD__FOOTER_BACK_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.FORM_FIELD__FOOTER_TEXT_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.GROUP;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.GROUP_EXT_INFO;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.IMAGE_FIELD_EXT_INFO__TEXT_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.INPUT_FIELD_EXT_INFO__BACK_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.INPUT_FIELD_EXT_INFO__MULTIPLE_VALUES_BACK_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.INPUT_FIELD_EXT_INFO__MULTIPLE_VALUES_TEXT_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.INPUT_FIELD_EXT_INFO__TEXT_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.LABEL_DECORATION_EXT_INFO__BACK_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.LABEL_FIELD_EXT_INFO__BACK_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.LABEL_FIELD_EXT_INFO__TEXT_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.PAGE_GROUP_EXT_INFO__BACK_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.POPUP_GROUP_EXT_INFO__BACK_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.RADIO_BUTTONS_FIELD_EXT_INFO__BACK_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.RADIO_BUTTONS_FIELD_EXT_INFO__TEXT_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.SEARCH_CONTROL_ADDITION_EXT_INFO__BACK_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.SEARCH_CONTROL_ADDITION_EXT_INFO__TEXT_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.SEARCH_STRING_ADDITION_EXT_INFO__BACK_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.SEARCH_STRING_ADDITION_EXT_INFO__TEXT_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.TABLE__BACK_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.TABLE__TEXT_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.TEXT_DOC_FIELD_EXT_INFO__BACK_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.TEXT_DOC_FIELD_EXT_INFO__TEXT_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.TITLE_STYLE__TITLE_TEXT_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.USUAL_GROUP_EXT_INFO__BACK_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.USUAL_GROUP_EXT_INFO__HIDDEN_STATE_TITLE_BACK_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.VIEW_STATUS_ADDITION_EXT_INFO__BACK_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.VIEW_STATUS_ADDITION_EXT_INFO__TEXT_COLOR;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.VIEW_STATUS_ADDITION_EXT_INFO__TITLE_TEXT_COLOR;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

import com._1c.g5.v8.dt.mcore.AutoColor;
import com._1c.g5.v8.dt.mcore.ColorDef;
import com._1c.g5.v8.dt.mcore.NamedElement;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.form.CorePlugin;

/**
 * Проверка: для оформления элементов управления формы задано конкретное
 * значение цвета (цвет текста, цвет фона и т.п.), а не элемент стиля.
 * <p>
 * Для изменения оформления следует использовать элементы стиля, а не задавать
 * конкретные значения непосредственно в элементах управления — это позволяет
 * аналогичным элементам управления выглядеть одинаково во всех формах
 * (статья 667 стандарта). Замечание ставится на каждое свойство цвета
 * (текст/фон), которому присвоено конкретное значение цвета (RGB); ссылка
 * на цвет стиля, веб-цвет, цвет палитры и «авто» не помечаются.
 * <p>
 * Проверяются свойства цвета элементов формы и их расширений (ext info):
 * цвет текста/фона элемента, цвет текста/фона заголовка, цвет текста/фона
 * подвала поля, цвет текста/фона нескольких значений поля ввода, цвет фона
 * заголовка свернутой группы, цвета дополнений таблиц (поиск, состояние
 * просмотра, управление поиском). Конкретное значение цвета — модельный
 * объект {@link ColorDef}, не являющийся авто-цветом ({@link AutoColor}).
 * Цвета границ, цвет картинки и цвет кнопки дополнения-состояния не
 * проверяются.
 *
 * @author malikov-pro
 */
public class FormItemExplicitColorCheck
    extends BasicCheck
{

    /** Идентификатор проверки (порт апстрим-issue #765, стандарт 667). */
    public static final String CHECK_ID = "up-765-explicit-color"; //$NON-NLS-1$

    /**
     * Проверяемые свойства цвета (цвет текста/фона и их разновидности)
     * элементов формы и их расширений.
     */
    private static final List<EStructuralFeature> COLOR_FEATURES = List.of(
        // элементы: цвет текста/фона заголовка (DataItem: поля, кнопки, таблицы; группы)
        TITLE_STYLE__TITLE_TEXT_COLOR,
        DATA_ITEM__TITLE_BACK_COLOR,
        // кнопка: цвет текста и фона
        BUTTON__TEXT_COLOR,
        BUTTON__BACK_COLOR,
        // поле: цвет текста/фона подвала
        FORM_FIELD__FOOTER_TEXT_COLOR,
        FORM_FIELD__FOOTER_BACK_COLOR,
        // таблица: цвет текста и фона
        TABLE__TEXT_COLOR,
        TABLE__BACK_COLOR,
        // декорация (в т.ч. расширенная подсказка): цвет текста
        DECORATION__TEXT_COLOR,
        // расширения полей: цвет текста/фона
        INPUT_FIELD_EXT_INFO__TEXT_COLOR,
        INPUT_FIELD_EXT_INFO__BACK_COLOR,
        INPUT_FIELD_EXT_INFO__MULTIPLE_VALUES_TEXT_COLOR,
        INPUT_FIELD_EXT_INFO__MULTIPLE_VALUES_BACK_COLOR,
        LABEL_FIELD_EXT_INFO__TEXT_COLOR,
        LABEL_FIELD_EXT_INFO__BACK_COLOR,
        CHECK_BOX_FIELD_EXT_INFO__TEXT_COLOR,
        CHECK_BOX_FIELD_EXT_INFO__BACK_COLOR,
        IMAGE_FIELD_EXT_INFO__TEXT_COLOR,
        RADIO_BUTTONS_FIELD_EXT_INFO__TEXT_COLOR,
        RADIO_BUTTONS_FIELD_EXT_INFO__BACK_COLOR,
        TEXT_DOC_FIELD_EXT_INFO__TEXT_COLOR,
        TEXT_DOC_FIELD_EXT_INFO__BACK_COLOR,
        FORMATTED_DOC_FIELD_EXT_INFO__TEXT_COLOR,
        FORMATTED_DOC_FIELD_EXT_INFO__BACK_COLOR,
        // расширение декорации: цвет фона
        LABEL_DECORATION_EXT_INFO__BACK_COLOR,
        // расширения групп: цвет фона
        POPUP_GROUP_EXT_INFO__BACK_COLOR,
        PAGE_GROUP_EXT_INFO__BACK_COLOR,
        USUAL_GROUP_EXT_INFO__BACK_COLOR,
        USUAL_GROUP_EXT_INFO__HIDDEN_STATE_TITLE_BACK_COLOR,
        COLUMN_GROUP_EXT_INFO__TITLE_BACK_COLOR,
        // дополнения таблиц: цвет текста/фона
        SEARCH_STRING_ADDITION_EXT_INFO__TEXT_COLOR,
        SEARCH_STRING_ADDITION_EXT_INFO__BACK_COLOR,
        VIEW_STATUS_ADDITION_EXT_INFO__TEXT_COLOR,
        VIEW_STATUS_ADDITION_EXT_INFO__BACK_COLOR,
        VIEW_STATUS_ADDITION_EXT_INFO__TITLE_TEXT_COLOR,
        SEARCH_CONTROL_ADDITION_EXT_INFO__TEXT_COLOR,
        SEARCH_CONTROL_ADDITION_EXT_INFO__BACK_COLOR);

    /**
     * Instantiates a new check.
     */
    public FormItemExplicitColorCheck()
    {
        super();
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.extension(new SkipBaseFormExtension())
            .title(Messages.FormItemExplicitColorCheck_title)
            .description(Messages.FormItemExplicitColorCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.UI_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .topObject(FORM)
            .containment(DATA_ITEM)
            .features(TITLE_STYLE__TITLE_TEXT_COLOR, DATA_ITEM__TITLE_BACK_COLOR, BUTTON__TEXT_COLOR,
                BUTTON__BACK_COLOR, FORM_FIELD__FOOTER_TEXT_COLOR, FORM_FIELD__FOOTER_BACK_COLOR, TABLE__TEXT_COLOR,
                TABLE__BACK_COLOR)
            .containment(GROUP)
            .features(TITLE_STYLE__TITLE_TEXT_COLOR)
            .containment(DECORATION)
            .features(DECORATION__TEXT_COLOR)
            .containment(FIELD_EXT_INFO)
            .features(INPUT_FIELD_EXT_INFO__TEXT_COLOR, INPUT_FIELD_EXT_INFO__BACK_COLOR,
                INPUT_FIELD_EXT_INFO__MULTIPLE_VALUES_TEXT_COLOR, INPUT_FIELD_EXT_INFO__MULTIPLE_VALUES_BACK_COLOR,
                LABEL_FIELD_EXT_INFO__TEXT_COLOR, LABEL_FIELD_EXT_INFO__BACK_COLOR,
                CHECK_BOX_FIELD_EXT_INFO__TEXT_COLOR, CHECK_BOX_FIELD_EXT_INFO__BACK_COLOR,
                IMAGE_FIELD_EXT_INFO__TEXT_COLOR, RADIO_BUTTONS_FIELD_EXT_INFO__TEXT_COLOR,
                RADIO_BUTTONS_FIELD_EXT_INFO__BACK_COLOR, TEXT_DOC_FIELD_EXT_INFO__TEXT_COLOR,
                TEXT_DOC_FIELD_EXT_INFO__BACK_COLOR, FORMATTED_DOC_FIELD_EXT_INFO__TEXT_COLOR,
                FORMATTED_DOC_FIELD_EXT_INFO__BACK_COLOR)
            .containment(GROUP_EXT_INFO)
            .features(POPUP_GROUP_EXT_INFO__BACK_COLOR, PAGE_GROUP_EXT_INFO__BACK_COLOR,
                USUAL_GROUP_EXT_INFO__BACK_COLOR, USUAL_GROUP_EXT_INFO__HIDDEN_STATE_TITLE_BACK_COLOR,
                COLUMN_GROUP_EXT_INFO__TITLE_BACK_COLOR)
            .containment(DECORATION_EXT_INFO)
            .features(LABEL_DECORATION_EXT_INFO__BACK_COLOR)
            .containment(ADDITION_EXT_INFO)
            .features(SEARCH_STRING_ADDITION_EXT_INFO__TEXT_COLOR, SEARCH_STRING_ADDITION_EXT_INFO__BACK_COLOR,
                VIEW_STATUS_ADDITION_EXT_INFO__TEXT_COLOR, VIEW_STATUS_ADDITION_EXT_INFO__BACK_COLOR,
                VIEW_STATUS_ADDITION_EXT_INFO__TITLE_TEXT_COLOR, SEARCH_CONTROL_ADDITION_EXT_INFO__TEXT_COLOR,
                SEARCH_CONTROL_ADDITION_EXT_INFO__BACK_COLOR);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (monitor.isCanceled() || !(object instanceof EObject eObject))
        {
            return;
        }
        for (EStructuralFeature feature : COLOR_FEATURES)
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (!feature.getEContainingClass().isInstance(object))
            {
                continue;
            }
            Object value = eObject.eGet(feature);
            if (!(value instanceof ColorDef color) || color instanceof AutoColor)
            {
                continue;
            }
            String colorText = colorText(color);
            String itemName = itemName(eObject);
            resultAceptor.addIssue(
                MessageFormat.format(Messages.FormItemExplicitColorCheck_message, colorText, itemName,
                    feature.getName()),
                eObject, feature);
        }
    }

    private static String colorText(ColorDef color)
    {
        return MessageFormat.format("#{0}{1}{2}", toHex(color.getRed()), toHex(color.getGreen()), //$NON-NLS-1$
            toHex(color.getBlue()));
    }

    private static String toHex(int value)
    {
        String hex = Integer.toHexString(Math.max(0, Math.min(255, value))).toUpperCase();
        return hex.length() < 2 ? "0" + hex : hex; //$NON-NLS-1$
    }

    private static String itemName(EObject object)
    {
        if (object instanceof NamedElement named)
        {
            return named.getName();
        }
        EObject container = object.eContainer();
        if (container instanceof NamedElement named)
        {
            return named.getName();
        }
        return ""; //$NON-NLS-1$
    }
}
