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
package com.e1c.v8codestyle.form.check;

import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.DATA_ITEM;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.DATA_PATH_REFERRED_OBJECT;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.FORM;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.INPUT_FIELD_EXT_INFO;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.INPUT_FIELD_EXT_INFO__CHOICE_BUTTON;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.INPUT_FIELD_EXT_INFO__CHOICE_BUTTON_REPRESENTATION;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.INPUT_FIELD_EXT_INFO__CHOICE_LIST;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.INPUT_FIELD_EXT_INFO__DROP_LIST_BUTTON;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.INPUT_FIELD_EXT_INFO__LIST_CHOICE_MODE;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.core.event.BmSubEvent;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckDefinition;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.components.IBasicCheckExtension;
import com.e1c.g5.v8.dt.check.context.CheckContextCollectingSession;
import com.e1c.g5.v8.dt.check.context.OnModelFeatureChangeContextCollector;
import com.e1c.g5.v8.dt.check.context.OnModelObjectAssociationContextCollector;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com._1c.g5.v8.dt.core.platform.IConfigurationProject;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.form.model.AbstractDataPath;
import com._1c.g5.v8.dt.form.model.ChoiceButtonRepresentation;
import com._1c.g5.v8.dt.form.model.DataPathReferredObject;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormAttribute;
import com._1c.g5.v8.dt.form.model.FormField;
import com._1c.g5.v8.dt.form.model.Group;
import com._1c.g5.v8.dt.form.model.InputFieldExtInfo;
import com._1c.g5.v8.dt.form.model.Table;
import com._1c.g5.v8.dt.mcore.TypeDescription;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.metadata.mdclass.BasicDbObject;
import com._1c.g5.v8.dt.metadata.mdclass.BasicFeature;
import com._1c.g5.v8.dt.metadata.mdclass.BasicForm;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.form.CorePlugin;
import com.google.inject.Inject;

/**
 * The check that a form input field referring to a metadata object with disabled
 * choice history on input (рус. «История выбора при вводе» = «Не использовать»)
 * has correct button properties: the drop-down list button is off, the choice
 * button is on and the choice button representation is "in input field".
 * Otherwise the user first gets into the drop-down menu with the history and
 * has to press "show all" every time before the actual selection starts.
 * <p>
 * Skips: fields with the list choice mode and a choice list filled in metadata;
 * objects with quick choice; fields (and their groups/tables) with the read-only
 * flag, as such elements are not available for the value selection. The form-level
 * read-only flag is a runtime concept and is not stored in the form model, so it
 * cannot be checked statically (the source АПК algorithm exits in this case).
 * <p>
 * Перенос проверки АПК_00617 (статья 744 стандарта 1С, п. 2.2). Одно замечание
 * на поле, как в алгоритме АПК; значение «Авто»/не задано трактуется как
 * нарушение (требуется явная установка свойств).
 *
 * @author malikov-pro
 */
public class ApkChoiceHistoryFieldButtonsCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00617-form-readonly-elements"; //$NON-NLS-1$

    private static final int KIND_CATALOG = 0;
    private static final int KIND_DOCUMENT = 1;
    private static final int KIND_CHART_OF_CHARACTERISTIC_TYPES = 2;
    private static final int KIND_EXCHANGE_PLAN = 3;
    private static final int KIND_CHART_OF_ACCOUNTS = 4;
    private static final int KIND_CHART_OF_CALCULATION_TYPES = 5;
    private static final int KIND_BUSINESS_PROCESS = 6;
    private static final int KIND_TASK = 7;
    private static final int KIND_ENUM = 8;

    /** Имя фичи «История выбора при вводе» у классов объектов метаданных. */
    private static final String CHOICE_HISTORY_FEATURE = "choiceHistoryOnInput"; //$NON-NLS-1$

    /** Имя фичи «Быстрый выбор» у классов объектов метаданных. */
    private static final String QUICK_CHOICE_FEATURE = "quickChoice"; //$NON-NLS-1$

    /** Литерал перечисления «Не использовать» (сравнение, как в apk-00616). */
    private static final String DONT_USE_LITERAL = "DontUse"; //$NON-NLS-1$

    /** Первый сегмент пути главного реквизита формы объекта (англ/рус вариант сценария). */
    private static final String OBJECT_SEGMENT_EN = "Object"; //$NON-NLS-1$

    /** См. {@link #OBJECT_SEGMENT_EN}. */
    private static final String OBJECT_SEGMENT_RU = "Объект"; //$NON-NLS-1$

    /** Имя стандартного атрибута «Ссылка» (англ/рус вариант сценария). */
    private static final String REF_ATTRIBUTE_EN = "Ref"; //$NON-NLS-1$

    /** См. {@link #REF_ATTRIBUTE_EN}. */
    private static final String REF_ATTRIBUTE_RU = "Ссылка"; //$NON-NLS-1$

    /** Имя фичи «обычные атрибуты» у классов объектов метаданных. */
    private static final String ATTRIBUTES_FEATURE = "attributes"; //$NON-NLS-1$

    // Префиксы имён ссылочных типов mcore: английские и русские имена
    // (объект-владелец выделяется отсечением префикса, как в текстовом анализе АПК).
    private static final List<String> REF_KIND_PREFIX_EN = List.of(
        "CatalogRef.", //$NON-NLS-1$
        "DocumentRef.", //$NON-NLS-1$
        "ChartOfCharacteristicTypesRef.", //$NON-NLS-1$
        "ExchangePlanRef.", //$NON-NLS-1$
        "ChartOfAccountsRef.", //$NON-NLS-1$
        "ChartOfCalculationTypesRef.", //$NON-NLS-1$
        "BusinessProcessRef.", //$NON-NLS-1$
        "TaskRef.", //$NON-NLS-1$
        "EnumRef."); //$NON-NLS-1$

    private static final List<String> REF_KIND_PREFIX_RU = List.of(
        "СправочникСсылка.", //$NON-NLS-1$
        "ДокументСсылка.", //$NON-NLS-1$
        "ПланВидовХарактеристикСсылка.", //$NON-NLS-1$
        "ПланОбменаСсылка.", //$NON-NLS-1$
        "ПланСчетовСсылка.", //$NON-NLS-1$
        "ПланВидовРасчетаСсылка.", //$NON-NLS-1$
        "БизнесПроцессСсылка.", //$NON-NLS-1$
        "ЗадачаСсылка.", //$NON-NLS-1$
        "ПеречислениеСсылка."); //$NON-NLS-1$

    private final IV8ProjectManager v8ProjectManager;

    /**
     * Instantiates a new check.
     *
     * @param v8ProjectManager the V8 project manager service, cannot be {@code null}.
     */
    @Inject
    public ApkChoiceHistoryFieldButtonsCheck(IV8ProjectManager v8ProjectManager)
    {
        super();
        this.v8ProjectManager = v8ProjectManager;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.ApkChoiceHistoryFieldButtonsCheck_title)
            .description(Messages.ApkChoiceHistoryFieldButtonsCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.UI_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .extension(new SkipBaseFormExtension())
            .extension(new DataPathChangeExtension());

        builder.topObject(FORM)
            .containment(INPUT_FIELD_EXT_INFO)
            .features(INPUT_FIELD_EXT_INFO__DROP_LIST_BUTTON, INPUT_FIELD_EXT_INFO__CHOICE_BUTTON,
                INPUT_FIELD_EXT_INFO__CHOICE_BUTTON_REPRESENTATION, INPUT_FIELD_EXT_INFO__LIST_CHOICE_MODE,
                INPUT_FIELD_EXT_INFO__CHOICE_LIST);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (!(object instanceof InputFieldExtInfo extInfo) || monitor.isCanceled())
        {
            return;
        }
        if (!(extInfo.eContainer() instanceof FormField field))
        {
            return;
        }
        Form form = EcoreUtil2.getContainerOfType(field, Form.class);
        if (form == null)
        {
            return;
        }
        AbstractDataPath dataPath = field.getDataPath();
        if (dataPath == null || dataPath.getSegments().isEmpty())
        {
            return;
        }

        // Объект-владелец значения поля: цель ссылочного типа значения поля.
        MdObject target = resolveValueTarget(form, dataPath);
        if (target == null || isQuickChoice(target) || !hasDisabledHistory(target))
        {
            return;
        }

        // Элемент, недоступный для выбора значения (ТолькоПросмотр поля, группы или таблицы), не проверяется.
        if (field.isReadOnly() || isReadOnlyContainer(field))
        {
            return;
        }

        // Исключение стандарта: режим выбора из списка со списком, заполненным в метаданных.
        if (extInfo.isListChoiceMode() && !extInfo.getChoiceList().isEmpty())
        {
            return;
        }

        EStructuralFeature violated = violatedFeature(extInfo);
        if (violated != null)
        {
            resultAceptor.addIssue(MessageFormat.format(Messages.ApkChoiceHistoryFieldButtonsCheck_message,
                field.getName(), target.getName()), violated);
        }
    }

    private EStructuralFeature violatedFeature(InputFieldExtInfo extInfo)
    {
        // «КнопкаВыпадающегоСписка» должна быть явно выключена (авто = включена).
        if (!Boolean.FALSE.equals(extInfo.getDropListButton()))
        {
            return INPUT_FIELD_EXT_INFO__DROP_LIST_BUTTON;
        }
        // «КнопкаВыбора» должна быть включена (авто = включена).
        if (Boolean.FALSE.equals(extInfo.getChoiceButton()))
        {
            return INPUT_FIELD_EXT_INFO__CHOICE_BUTTON;
        }
        // «ОтображениеКнопкиВыбора» должно быть «В поле ввода».
        if (extInfo.getChoiceButtonRepresentation() != ChoiceButtonRepresentation.SHOW_IN_INPUT_FIELD)
        {
            return INPUT_FIELD_EXT_INFO__CHOICE_BUTTON_REPRESENTATION;
        }
        return null;
    }

    /**
     * Возвращает объект метаданных — цель ссылочного типа значения поля ввода,
     * если такой тип резолвится; для нессылочных типов и составных типов без
     * ссылочной части возвращает {@code null}. Объекты пути просматриваются
     * от последнего сегмента к первому: тип значения поля — самый глубокий
     * известный (производные данные дорезолвируют сегменты постепенно).
     */
    private MdObject resolveValueTarget(Form form, AbstractDataPath dataPath)
    {
        Configuration configuration = configuration(form);
        if (configuration == null)
        {
            return null;
        }

        EList<DataPathReferredObject> objects = dataPath.getObjects();
        for (int i = objects.size() - 1; i >= 0; i--)
        {
            MdObject target = resolveSegmentTarget(form, configuration, objects.get(i).getValueType());
            if (target != null)
            {
                return target;
            }
        }

        // Путь ещё не разрешён производными данными: тип берётся из модели напрямую —
        // односегментный путь (реквизит формы) или путь «Объект.<Атрибут>» (форма объекта).
        if (!objects.isEmpty())
        {
            return null;
        }
        List<String> segments = dataPath.getSegments();
        if (segments.size() == 1)
        {
            String name = segments.get(0);
            for (FormAttribute attribute : form.getAttributes())
            {
                if (name.equals(attribute.getName()))
                {
                    return resolveSegmentTarget(form, configuration, attribute.getValueType());
                }
            }
            return null;
        }
        return resolveObjectPathTarget(form, configuration, segments);
    }

    /**
     * Резолвит путь вида «Объект.&lt;Атрибут&gt;» формы объекта: атрибут ищется
     * среди обычных атрибутов объекта-владельца формы; стандартный атрибут
     * «Ссылка» резолвится семантически — его цель всегда сам объект-владелец.
     */
    private MdObject resolveObjectPathTarget(Form form, Configuration configuration, List<String> segments)
    {
        if (segments.size() != 2 || (!OBJECT_SEGMENT_EN.equalsIgnoreCase(segments.get(0))
            && !OBJECT_SEGMENT_RU.equalsIgnoreCase(segments.get(0))))
        {
            return null;
        }
        BasicForm mdForm = form.getMdForm();
        if (mdForm == null)
        {
            return null;
        }
        if (mdForm.eIsProxy())
        {
            mdForm = (BasicForm)EcoreUtil.resolve(mdForm, form);
            if (mdForm.eIsProxy())
            {
                return null;
            }
        }
        BasicDbObject owner = EcoreUtil2.getContainerOfType(mdForm, BasicDbObject.class);
        if (owner == null)
        {
            return null;
        }
        String attributeName = segments.get(1);
        if (REF_ATTRIBUTE_EN.equalsIgnoreCase(attributeName) || REF_ATTRIBUTE_RU.equalsIgnoreCase(attributeName))
        {
            // Стандартный атрибут «Ссылка»: тип всегда ссылка на сам объект-владелец.
            return !isQuickChoice(owner) && hasDisabledHistory(owner) ? owner : null;
        }
        EStructuralFeature attributesFeature = owner.eClass().getEStructuralFeature(ATTRIBUTES_FEATURE);
        if (attributesFeature != null && owner.eGet(attributesFeature) instanceof EList<?> attributes)
        {
            for (Object attribute : attributes)
            {
                if (attribute instanceof BasicFeature feature && attributeName.equalsIgnoreCase(feature.getName()))
                {
                    return resolveSegmentTarget(form, configuration, feature.getType());
                }
            }
        }
        return null;
    }

    private Configuration configuration(Form form)
    {
        IV8Project project = v8ProjectManager.getProject(form);
        if (!(project instanceof IConfigurationProject configurationProject))
        {
            return null;
        }
        return configurationProject.getConfiguration();
    }

    /**
     * Ищет среди типов значения цель ссылочного типа с отключённой историей
     * выбора; цель с быстрым выбором исключает проверку поля ({@code null}).
     */
    private MdObject resolveSegmentTarget(Form form, Configuration configuration, TypeDescription valueType)
    {
        if (valueType == null)
        {
            return null;
        }
        MdObject disabled = null;
        for (TypeItem type : valueType.getTypes())
        {
            if (type.eIsProxy())
            {
                type = (TypeItem)EcoreUtil.resolve(type, form);
                if (type.eIsProxy())
                {
                    continue;
                }
            }
            MdObject target = findRefTarget(configuration, type);
            if (target == null)
            {
                continue;
            }
            if (isQuickChoice(target))
            {
                // Быстрый выбор у цели: пользователь выбирает из списка, меню истории не мешает.
                return null;
            }
            if (disabled == null && hasDisabledHistory(target))
            {
                disabled = target;
            }
        }
        return disabled;
    }

    /**
     * Находит объект метаданных по имени ссылочного типа (отсечение префикса
     * категории на английском или русском); для нессылочных типов — {@code null}.
     */
    private MdObject findRefTarget(Configuration configuration, TypeItem type)
    {
        String nameEn = type.getName();
        String nameRu = type.getNameRu();
        for (int i = 0; i < REF_KIND_PREFIX_EN.size(); i++)
        {
            if (nameEn != null && nameEn.startsWith(REF_KIND_PREFIX_EN.get(i)))
            {
                return findByName(configuration, i, nameEn.substring(REF_KIND_PREFIX_EN.get(i).length()));
            }
            if (nameRu != null && nameRu.startsWith(REF_KIND_PREFIX_RU.get(i)))
            {
                return findByName(configuration, i, nameRu.substring(REF_KIND_PREFIX_RU.get(i).length()));
            }
        }
        return null;
    }

    private MdObject findByName(Configuration configuration, int refKind, String name)
    {
        if (name == null || name.isBlank())
        {
            return null;
        }
        return switch (refKind)
        {
            case KIND_CATALOG -> findByName(configuration.getCatalogs(), name);
            case KIND_DOCUMENT -> findByName(configuration.getDocuments(), name);
            case KIND_CHART_OF_CHARACTERISTIC_TYPES -> findByName(configuration.getChartsOfCharacteristicTypes(), name);
            case KIND_EXCHANGE_PLAN -> findByName(configuration.getExchangePlans(), name);
            case KIND_CHART_OF_ACCOUNTS -> findByName(configuration.getChartsOfAccounts(), name);
            case KIND_CHART_OF_CALCULATION_TYPES -> findByName(configuration.getChartsOfCalculationTypes(), name);
            case KIND_BUSINESS_PROCESS -> findByName(configuration.getBusinessProcesses(), name);
            case KIND_TASK -> findByName(configuration.getTasks(), name);
            case KIND_ENUM -> findByName(configuration.getEnums(), name);
            default -> null;
        };
    }

    private <T extends MdObject> MdObject findByName(EList<T> objects, String name)
    {
        for (MdObject object : objects)
        {
            if (name.equalsIgnoreCase(object.getName()))
            {
                return object;
            }
        }
        return null;
    }

    /**
     * Значение свойства «История выбора при вводе» объекта метаданных равно
     * «Не использовать». Свойство читается по имени фичи динамически: пакет
     * com._1c.g5.v8.dt.metadata.common (тип перечисления) не входит в API
     * form-бандла; неявное сравнение через EEnum-литерал — как в проверке
     * apk-00616 md-канала.
     */
    private boolean hasDisabledHistory(MdObject object)
    {
        EStructuralFeature feature = object.eClass().getEStructuralFeature(CHOICE_HISTORY_FEATURE);
        if (feature == null)
        {
            return false;
        }
        Object value = object.eGet(feature);
        if (value == null)
        {
            return false;
        }
        if (feature.getEType() instanceof EEnum enumType)
        {
            EEnumLiteral dontUse = enumType.getEEnumLiteral(DONT_USE_LITERAL);
            return dontUse != null && dontUse.getInstance().equals(value);
        }
        return false;
    }

    private boolean isQuickChoice(MdObject object)
    {
        EStructuralFeature feature = object.eClass().getEStructuralFeature(QUICK_CHOICE_FEATURE);
        return feature != null && Boolean.TRUE.equals(object.eGet(feature));
    }

    private boolean isReadOnlyContainer(EObject object)
    {
        for (EObject container = object.eContainer(); container != null && !(container instanceof Form);
            container = container.eContainer())
        {
            if ((container instanceof Group group && group.isReadOnly())
                || (container instanceof Table table && table.isReadOnly()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Пересчёт при изменении пути данных или признака «ТолькоПросмотр» элемента
     * (оба признака живут на элементе формы, а проверка зарегистрирована на
     * расширении ext info), а также при ассоциации объектов пути данных:
     * производные данные дорезолвивают путь после того, как элемент формы уже
     * проверен, — без пересчёта проверка осталась бы с неполным типом значения
     * (как в апстримном DynamicListItemTitleCheck).
     */
    private static final class DataPathChangeExtension
        implements IBasicCheckExtension
    {
        @Override
        public void configureContextCollector(ICheckDefinition definition)
        {
            OnModelFeatureChangeContextCollector featureCollector =
                (IBmObject bmObject, EStructuralFeature feature, BmSubEvent bmEvent,
                    CheckContextCollectingSession contextSession) -> {
                    if (bmObject instanceof FormField field && field.getExtInfo() instanceof InputFieldExtInfo extInfo)
                    {
                        contextSession.addModelCheck(extInfo);
                    }
                };
            definition.addModelFeatureChangeContextCollector(featureCollector, DATA_ITEM);

            OnModelObjectAssociationContextCollector associationCollector =
                (IBmObject bmObject, BmSubEvent bmEvent, CheckContextCollectingSession contextSession) -> {
                    if (bmObject instanceof DataPathReferredObject referred
                        && referred.eContainer() instanceof AbstractDataPath dataPath
                        && dataPath.eContainer() instanceof FormField field
                        && field.getExtInfo() instanceof InputFieldExtInfo extInfo)
                    {
                        contextSession.addModelCheck(extInfo);
                    }
                };
            definition.addModelAssociationContextCollector(associationCollector, DATA_PATH_REFERRED_OBJECT);
        }
    }
}
