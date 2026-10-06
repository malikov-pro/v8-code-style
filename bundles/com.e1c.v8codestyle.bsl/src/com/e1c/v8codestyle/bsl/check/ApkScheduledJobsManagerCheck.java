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
 *     malikov-pro - port of the APK check 00184
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.STATIC_FEATURE_ACCESS;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.OptInCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: прямое обращение в программном коде к менеджеру регламентных
 * заданий («РегламентныеЗадания» / «ScheduledJobs»). Начало цепочки обращения
 * с таким именем фиксируется как прямое управление регламентными заданиями;
 * для управления заданиями используйте программный интерфейс БСП из модуля
 * «РегламентныеЗаданияСервер».
 * <p>
 * Перенос проверки АПК_00184 (статья 760 стандарта 1С, п. 3). Обращение
 * фиксируется по точному совпадению имени фича-доступа, поэтому обращения
 * вида «Метаданные.РегламентныеЗадания» и «РегламентныеЗаданияСервер.»
 * не фиксируются. Исключение АПК: общие модули с именем, начинающимся
 * на «РегламентныеЗадания», не проверяются.
 * <p>
 * Отклонение от алгоритма АПК: предусловие «в решении есть подсистема
 * ТехнологияСервиса/БСП» статически не воспроизводится, поэтому проверка
 * сделана аудит-классом — по умолчанию выключена (пользователь включает
 * её для решений на БСП).
 *
 * @author malikov-pro
 */
public class ApkScheduledJobsManagerCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00184-scheduled-jobs-manager"; //$NON-NLS-1$

    /** Имена менеджера регламентных заданий в глобальном контексте (рус/англ). */
    private static final Set<String> MANAGER_NAMES = Set.of("РегламентныеЗадания", "ScheduledJobs"); //$NON-NLS-1$ //$NON-NLS-2$

    /** Префикс имени общего модуля, в котором обращение не проверяется. */
    private static final String EXCLUDED_OWNER_PREFIX = "РегламентныеЗадания"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public ApkScheduledJobsManagerCheck()
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
        builder.title(Messages.ApkScheduledJobsManagerCheck_title)
            .description(Messages.ApkScheduledJobsManagerCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.WARNING)
            .extension(new OptInCheckExtension())
            .module()
            .checkedObjectType(STATIC_FEATURE_ACCESS);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        if (progressMonitor.isCanceled() || !(object instanceof StaticFeatureAccess featureAccess))
        {
            return;
        }
        String name = featureAccess.getName();
        if (name == null || !isManagerName(name) || isExcludedOwnerModule(featureAccess))
        {
            return;
        }
        resultAceptor.addIssue(MessageFormat.format(Messages.ApkScheduledJobsManagerCheck_Direct_manager_access, name),
            featureAccess);
    }

    private boolean isManagerName(String name)
    {
        for (String managerName : MANAGER_NAMES)
        {
            if (managerName.equalsIgnoreCase(name))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Исключение АПК: общие модули с именем, начинающимся на
     * «РегламентныеЗадания», не проверяются.
     */
    private boolean isExcludedOwnerModule(StaticFeatureAccess featureAccess)
    {
        Module module = EcoreUtil2.getContainerOfType(featureAccess, Module.class);
        if (module == null || module.getModuleType() != ModuleType.COMMON_MODULE)
        {
            return false;
        }
        if (module.getOwner() instanceof MdObject owner && owner.getName() != null)
        {
            return owner.getName().toLowerCase(Locale.ROOT)
                .startsWith(EXCLUDED_OWNER_PREFIX.toLowerCase(Locale.ROOT));
        }
        return false;
    }
}
