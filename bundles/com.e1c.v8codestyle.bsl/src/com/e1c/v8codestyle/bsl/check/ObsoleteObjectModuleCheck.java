/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: 1C-Soft LLC
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the APK check АПК_01179
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

import com._1c.g5.v8.dt.bsl.model.EmptyStatement;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка наличия кода в модуле устаревшего объекта метаданных.
 * <p>
 * У объекта метаданных, устаревшего по имени — имя самого объекта (для общего
 * модуля) либо имя родителя верхнего уровня начинается с «Удалить»
 * (англ. «Delete»/«Obsolete», в любом регистре) — все модули должны быть
 * пустыми: устаревший объект удаляется вместе со своим кодом, формами
 * и другими ставшими избыточными элементами. Каждый непустой модуль
 * помечается одним замечанием.
 * <p>
 * Модуль считается пустым, если не содержит ни одного исполнимого оператора:
 * комментарии и пустые строки допустимы. В отличие от исходного алгоритма АПК,
 * который считает модуль непустым по наличию любого текста, здесь модуль
 * из одних комментариев ошибкой не считается.
 * <p>
 * Перенос проверки АПК_01179 (статья 534 стандарта 1С).
 *
 * @author malikov-pro
 */
public class ObsoleteObjectModuleCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-01179-obsolete-object-module"; //$NON-NLS-1$

    private static final Set<String> OBSOLETE_NAME_PREFIXES = Set.of("УДАЛИТЬ", "DELETE", "OBSOLETE"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    /**
     * Instantiates a new check.
     */
    public ObsoleteObjectModuleCheck()
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
        builder.title(Messages.ObsoleteObjectModuleCheck_title)
            .description(Messages.ObsoleteObjectModuleCheck_description)
            .issueType(IssueType.ERROR)
            .severity(IssueSeverity.MAJOR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .extension(ModuleTypeFilter.onlyTypes(ModuleType.COMMON_MODULE, ModuleType.OBJECT_MODULE,
                ModuleType.MANAGER_MODULE, ModuleType.RECORDSET_MODULE, ModuleType.VALUE_MANAGER_MODULE))
            .module()
            .checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Module module = (Module)object;
        if (progressMonitor.isCanceled())
        {
            return;
        }

        MdObject owner = getTopOwnerObject(module);
        if (owner == null)
        {
            return;
        }
        String ownerName = owner.getName();
        if (ownerName == null || !isObsoleteName(ownerName))
        {
            return;
        }

        if (hasExecutableCode(module))
        {
            resultAceptor.addIssue(
                MessageFormat.format(Messages.ObsoleteObjectModuleCheck_Module_has_code, ownerName), module);
        }
    }

    /**
     * Проверяет, что имя объекта начинается с префикса устаревшего объекта
     * (рус/англ, без учёта регистра), как в алгоритме АПК.
     */
    private static boolean isObsoleteName(String name)
    {
        String upperName = name.toUpperCase(Locale.ROOT);
        return OBSOLETE_NAME_PREFIXES.stream().anyMatch(upperName::startsWith);
    }

    /**
     * Модуль пуст, если не содержит ни одного исполнимого оператора:
     * ни на уровне модуля, ни в методах. Комментарии и пустые строки
     * операторами не считаются (одиночная «;» — EmptyStatement — тоже).
     */
    private static boolean hasExecutableCode(Module module)
    {
        if (hasExecutableCode(module.allStatements()))
        {
            return true;
        }
        for (Method method : module.allMethods())
        {
            if (hasExecutableCode(method.allStatements()))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean hasExecutableCode(List<Statement> statements)
    {
        for (Statement statement : statements)
        {
            if (!(statement instanceof EmptyStatement))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Возвращает объект метаданных верхнего уровня — владельца модуля:
     * для общего модуля это сам модуль, для модулей объекта/менеджера/набора
     * записей — объект-владелец, для модуля формы — объект, которому
     * принадлежит форма.
     */
    private MdObject getTopOwnerObject(Module module)
    {
        EObject owner = module.getOwner();
        if (owner == null)
        {
            return null;
        }
        if (owner.eIsProxy())
        {
            owner = EcoreUtil.resolve(owner, module);
        }
        if (owner instanceof Form)
        {
            owner = ((Form)owner).getMdForm();
        }
        if (owner == null)
        {
            return null;
        }
        // Поднимаемся к объекту верхнего уровня (если модуль принадлежит
        // вложенному элементу), как в алгоритме АПК: имя родителя.
        while (owner.eContainer() instanceof MdObject)
        {
            owner = owner.eContainer();
        }
        return owner instanceof MdObject ? (MdObject)owner : null;
    }
}
