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
 *     malikov-pro - port of the upstream issue 1C-Company/v8-code-style#1354
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.NAMED_ELEMENT__NAME;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.FormalParam;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: параметр метода модуля объекта (справочника, документа и т.п.)
 * не должен совпадать по имени (без учёта регистра) с именем реквизита
 * или табличной части того же объекта. Такое совпадение перекрывает имя
 * реквизита/табличной части и может привести к неявному поведению или ошибке
 * при выполнении. Каждый совпавший параметр помечается одним замечанием.
 * <p>
 * Сравниваются только прикладные реквизиты и имена табличных частей
 * верхнего уровня объекта-владельца модуля: реквизиты табличных частей
 * и стандартные реквизиты (Код, Наименование, ЭтотОбъект и т.п.)
 * не сравниваются — перекрытие стандартного реквизита параметром метода
 * не создаёт конфликта областей видимости.
 * <p>
 * Перенос предложения апстрима 1C-Company/v8-code-style#1354. Имена
 * реквизитов и табличных частей читаются из модели метаданных рефлексивно
 * (по именам фич {@code attributes}/{@code tabularSections}), поэтому проверка
 * работает для всех видов объектов-владельцев модуля объекта.
 *
 * @author malikov-pro
 */
public class ObjectModuleParameterMatchesAttributeCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс up-N — номер issue апстрима). */
    public static final String CHECK_ID = "up-1354-param-matches-attribute"; //$NON-NLS-1$

    private static final String ATTRIBUTES_FEATURE = "attributes"; //$NON-NLS-1$

    private static final String TABULAR_SECTIONS_FEATURE = "tabularSections"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public ObjectModuleParameterMatchesAttributeCheck()
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
        builder.title(Messages.ObjectModuleParameterMatchesAttributeCheck_title)
            .description(Messages.ObjectModuleParameterMatchesAttributeCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .extension(ModuleTypeFilter.onlyTypes(ModuleType.OBJECT_MODULE))
            .module()
            .checkedObjectType(METHOD);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (monitor.isCanceled() || !(object instanceof Method method))
        {
            return;
        }

        Module module = EcoreUtil2.getContainerOfType(method, Module.class);
        if (module == null || module.getModuleType() != ModuleType.OBJECT_MODULE)
        {
            return;
        }

        EObject owner = getModuleOwner(module);
        if (owner == null)
        {
            return;
        }

        Set<String> names = collectAttributeAndTabularSectionNames(owner);
        if (names.isEmpty())
        {
            return;
        }

        for (FormalParam param : method.getFormalParams())
        {
            if (monitor.isCanceled())
            {
                return;
            }
            String name = param.getName();
            if (name != null && names.contains(name.toLowerCase(Locale.ROOT)))
            {
                resultAceptor.addIssue(MessageFormat.format(
                    Messages.ObjectModuleParameterMatchesAttributeCheck_Parameter_matches_attribute, name), param,
                    NAMED_ELEMENT__NAME);
            }
        }
    }

    /**
     * Возвращает объект метаданных — владелец модуля объекта. Модуль, который
     * не разрешается (отсутствует владелец), трактуется как «нечего проверять».
     */
    private EObject getModuleOwner(Module module)
    {
        EObject owner = module.getOwner();
        if (owner == null)
        {
            return null;
        }
        if (owner.eIsProxy())
        {
            owner = EcoreUtil.resolve(owner, module);
            if (owner.eIsProxy())
            {
                return null;
            }
        }
        return owner;
    }

    /**
     * Собирает имена (в нижнем регистре) прикладных реквизитов и табличных
     * частей верхнего уровня объекта-владельца модуля. Реквизиты табличных
     * частей не собираются. Объект без реквизитов и табличных частей даёт
     * пустой результат.
     */
    private Set<String> collectAttributeAndTabularSectionNames(EObject owner)
    {
        Set<String> names = new HashSet<>();
        collectChildNames(owner, ATTRIBUTES_FEATURE, names);
        collectChildNames(owner, TABULAR_SECTIONS_FEATURE, names);
        return names;
    }

    /**
     * Добавляет имена дочерних элементов объекта по имени фичи-коллекции
     * (рефлексивно, без зависимости от конкретного вида объекта метаданных).
     */
    private void collectChildNames(EObject owner, String featureName, Set<String> names)
    {
        EStructuralFeature feature = owner.eClass().getEStructuralFeature(featureName);
        if (feature == null)
        {
            return;
        }
        Object value = owner.eGet(feature);
        if (!(value instanceof List<?> children))
        {
            return;
        }
        for (Object child : children)
        {
            if (!(child instanceof MdObject element))
            {
                continue;
            }
            String name = element.getName();
            if (name != null && !name.isEmpty())
            {
                names.add(name.toLowerCase(Locale.ROOT));
            }
        }
    }
}
