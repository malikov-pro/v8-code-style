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
 *     malikov-pro - port of the APK check АПК_00460
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.NAMED_ELEMENT__NAME;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.RegionPreprocessor;
import com._1c.g5.v8.dt.bsl.model.util.BslUtil;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.bsl.ModuleStructureSection;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * В переопределяемом общем модуле не должно быть внешних (верхнеуровневых)
 * областей, кроме «ПрограммныйИнтерфейс» (подпункт 7 правила АПК_00460,
 * статья 554 стандарта 1С).
 * <p>
 * Переопределяемый общий модуль (в имени содержится
 * «Переопределяемый»/«Override») целиком размещается в области
 * «ПрограммныйИнтерфейс»/«Public»; любая другая область верхнего уровня
 * модуля не допускается. Вложенные области (внутри других областей)
 * разрешены. Каждая недопустимая область помечается отдельным замечанием.
 * <p>
 * Заимствованные (adopted) общие модули проектов-расширений не проверяются
 * — как в исходном алгоритме АПК.
 *
 * @author malikov-pro
 */
public class OverridableModuleTopRegionCheck
    extends AbstractModuleStructureCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00460-top-region"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public OverridableModuleTopRegionCheck()
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
        builder.title(Messages.OverridableModuleTopRegionCheck_title)
            .description(Messages.OverridableModuleTopRegionCheck_description)
            .issueType(IssueType.CODE_STYLE)
            .severity(IssueSeverity.MINOR)
            .extension(new SkipAdoptedInExtensionModuleOwnerExtension())
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Module module = (Module)object;
        if (progressMonitor.isCanceled() || !OverridableModuleUtil.isOverridableCommonModule(module))
        {
            return;
        }

        List<RegionPreprocessor> regions = BslUtil.getAllRegionPreprocessors(module);
        for (RegionPreprocessor region : regions)
        {
            if (progressMonitor.isCanceled())
            {
                return;
            }
            // Проверяем только области верхнего уровня: вложенные области разрешены.
            if (getFirstParentRegion(region).isPresent())
            {
                continue;
            }
            String regionName = region.getName();
            if (regionName == null || isPublicRegionName(regionName))
            {
                continue;
            }
            resultAceptor.addIssue(
                MessageFormat.format(Messages.OverridableModuleTopRegionCheck_Region_is_not_allowed, regionName,
                    OverridableModuleUtil.getOwnerName(module)),
                region, NAMED_ELEMENT__NAME);
        }
    }

    private static boolean isPublicRegionName(String regionName)
    {
        for (String publicName : ModuleStructureSection.PUBLIC.getNames())
        {
            if (publicName.equalsIgnoreCase(regionName))
            {
                return true;
            }
        }
        return false;
    }
}
