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
 *     malikov-pro - port of the upstream issue #632 (std 644, APK 473)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.NAMED_ELEMENT__NAME;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.RegionPreprocessor;
import com._1c.g5.v8.dt.bsl.model.util.BslUtil;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Область «ДляВызоваИзДругихПодсистем» (англ. «InterfaceImplementation»)
 * не входит в область «ПрограммныйИнтерфейс» (англ. «Public»).
 * <p>
 * По статье 644 стандарта 1С (п. 2.2) область
 * «ДляВызоваИзДругихПодсистем» размещается внутри области
 * «ПрограммныйИнтерфейс». Верхнеуровневая область или область, вложенная
 * в любую другую область (кроме «ПрограммныйИнтерфейс»), помечается
 * замечанием на имя области. Проверяются все типы модулей; заимствованные
 * (adopted) модули проектов-расширений не проверяются.
 *
 * @author malikov-pro
 */
public class DfioNotInPublicRegionCheck
    extends AbstractModuleStructureCheck
{

    /** Идентификатор проверки (апстрим-issue 1C-Company/v8-code-style#632). */
    public static final String CHECK_ID = "up-632-dfio-not-in-public"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public DfioNotInPublicRegionCheck()
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
        builder.title(Messages.DfioNotInPublicRegionCheck_title)
            .description(Messages.DfioNotInPublicRegionCheck_description)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new SkipAdoptedInExtensionModuleOwnerExtension())
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        Module module = (Module)object;
        List<RegionPreprocessor> regions = BslUtil.getAllRegionPreprocessors(module);
        for (RegionPreprocessor region : regions)
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (!DfioRegionUtil.isDfioRegionName(region.getName()))
            {
                continue;
            }
            // Область должна быть вложена в «ПрограммныйИнтерфейс»:
            // верхнеуровневая область и вложенность в любую другую область — нарушение.
            Optional<RegionPreprocessor> topRegion = getTopParentRegion(region);
            if (topRegion.isPresent() && DfioRegionUtil.isPublicRegionName(topRegion.get().getName()))
            {
                continue;
            }
            resultAceptor.addIssue(
                MessageFormat.format(Messages.DfioNotInPublicRegionCheck_Region_is_not_inside_public,
                    region.getName()),
                region, NAMED_ELEMENT__NAME);
        }
    }
}
