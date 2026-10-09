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
 *     malikov-pro - port of the АПК rule АПК_00616
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BASIC_DB_OBJECT__MANAGER_MODULE;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.CATALOG;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.CATALOG__CHOICE_HISTORY_ON_INPUT;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.DOCUMENT;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.DOCUMENT__CHOICE_HISTORY_ON_INPUT;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

import com._1c.g5.v8.dt.mcore.McorePackage;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.Document;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.md.CorePlugin;

/**
 * The "History choice on input" (рус. «История выбора при вводе») property of a metadata object
 * must be set to "Don't use" (рус. «Не использовать»):
 * <ul>
 * <li>for every Document, unconditionally;</li>
 * <li>for every Catalog or Document whose manager module contains the data selection
 * processing handler (рус. «ОбработкаПолученияДанныхВыбора» / ObtainDataSelectionProcessing) —
 * the custom handler makes the input history incorrect for the user.</li>
 * </ul>
 * A value that is not set in the project is treated as "Auto", i.e. it is reported as well:
 * the only allowed value is "Don't use". One issue per object, as in the АПК algorithm.
 * <p>
 * Перенос проверки АПК_00616 (статья 744 стандарта 1С). Английские имена обработчика
 * и значения свойства берутся из англоязычного синонима платформы.
 *
 * @author malikov-pro
 */
public class MdObjectChoiceHistoryOnInputCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00616-input-history-dont-use"; //$NON-NLS-1$

    private static final String DONT_USE_LITERAL = "DontUse"; //$NON-NLS-1$

    private static final String HANDLER_NAME_RU = "ОбработкаПолученияДанныхВыбора"; //$NON-NLS-1$

    private static final String HANDLER_NAME_EN = "ObtainDataSelectionProcessing"; //$NON-NLS-1$

    /** Имя EClass метода в BSL-модели: конкретные узлы — Procedure/Function, наследующие абстрактный Method. */
    private static final String METHOD_ECLASS_NAME = "Method"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public MdObjectChoiceHistoryOnInputCheck()
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
        builder.title(Messages.MdObjectChoiceHistoryOnInputCheck_title)
            .description(Messages.MdObjectChoiceHistoryOnInputCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.UI_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .extension(new SkipAdoptedInExtensionMdObjectExtension())
            .topObject(DOCUMENT)
            .checkTop()
            .features(DOCUMENT__CHOICE_HISTORY_ON_INPUT)
            .topObject(CATALOG)
            .checkTop()
            .features(CATALOG__CHOICE_HISTORY_ON_INPUT);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (monitor.isCanceled())
        {
            return;
        }
        if (object instanceof Document document)
        {
            // У документа свойство должно быть «Не использовать» безусловно (как блок 1 алгоритма АПК).
            checkValue(document, DOCUMENT__CHOICE_HISTORY_ON_INPUT, false, resultAceptor);
        }
        else if (object instanceof Catalog catalog)
        {
            // У справочника — только при наличии обработчика «ОбработкаПолученияДанныхВыбора» (блок 2 алгоритма).
            checkValue(catalog, CATALOG__CHOICE_HISTORY_ON_INPUT, true, resultAceptor);
        }
    }

    private void checkValue(EObject mdObject, EAttribute feature, boolean requireHandler, ResultAcceptor resultAceptor)
    {
        if (isDontUse(mdObject, feature))
        {
            return;
        }
        if (requireHandler && !hasDataSelectionProcessingHandler(mdObject))
        {
            return;
        }
        Object value = mdObject.eGet(feature);
        String actual = value == null ? "" : String.valueOf(value); //$NON-NLS-1$
        String message = requireHandler
            ? MessageFormat.format(Messages.MdObjectChoiceHistoryOnInputCheck_Handler_requires_dont_use, actual)
            : MessageFormat.format(Messages.MdObjectChoiceHistoryOnInputCheck_History_is_not_dont_use, actual);
        resultAceptor.addIssue(message, mdObject, feature);
    }

    /**
     * Значение свойства равно «Не использовать». Значение, не заданное в проекте, приравнивается
     * к «Авто», т.е. нарушением; единственное допустимое значение — «Не использовать».
     */
    private boolean isDontUse(EObject mdObject, EAttribute feature)
    {
        Object value = mdObject.eGet(feature);
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

    /**
     * Проверяет наличие в менеджер-модуле объекта метода «ОбработкаПолученияДанныхВыбора»
     * (англ. ObtainDataSelectionProcessing). Модуль и метод читаются из модели метаданных без
     * зависимости от bsl.model (в бандле md пакет bsl.model не импортирован): ссылка на модуль
     * берётся по фиче {@code BasicDbObject.managerModule}, имя метода — по mcore {@code NamedElement.name}.
     * Модуль, который не удалось получить (отсутствует или не разрешается), трактуется как
     * «обработчика нет» — только пропуск замечания, без ложных срабатываний.
     */
    private boolean hasDataSelectionProcessingHandler(EObject mdObject)
    {
        Object moduleObject = mdObject.eGet(BASIC_DB_OBJECT__MANAGER_MODULE);
        if (!(moduleObject instanceof EObject module))
        {
            return false;
        }
        if (module.eIsProxy())
        {
            module = EcoreUtil.resolve(module, mdObject);
            if (module.eIsProxy())
            {
                return false;
            }
        }
        for (EObject child : module.eContents())
        {
            if (!isMethodEClass(child.eClass()))
            {
                continue;
            }
            Object name = child.eGet(McorePackage.Literals.NAMED_ELEMENT__NAME);
            if (HANDLER_NAME_RU.equalsIgnoreCase(String.valueOf(name))
                || HANDLER_NAME_EN.equalsIgnoreCase(String.valueOf(name)))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * EClass метода BSL-модели: сам {@code Method} (на случай изменения модели) или наследники —
     * конкретные узлы {@code Procedure}/{@code Function}. Проверка по имени супертипа, без
     * зависимости бандла md от пакета bsl.model.
     */
    private boolean isMethodEClass(EClass eClass)
    {
        if (METHOD_ECLASS_NAME.equals(eClass.getName()))
        {
            return true;
        }
        for (EClass superType : eClass.getEAllSuperTypes())
        {
            if (METHOD_ECLASS_NAME.equals(superType.getName()))
            {
                return true;
            }
        }
        return false;
    }
}
