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

import java.util.Locale;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;

/**
 * Common helpers of the АПК_00460 checks: the overridable common module is
 * a common module whose name contains «Переопределяемый» (English
 * «Override»/«Overridable») in any case, as in the APK algorithm.
 *
 * @author malikov-pro
 */
final class OverridableModuleUtil
{

    private static final String RU_NAME_PART_UPPER = "ПЕРЕОПРЕДЕЛЯЕМЫЙ"; //$NON-NLS-1$
    private static final String EN_NAME_PART_UPPER = "OVERRIDABLE"; //$NON-NLS-1$
    private static final String EN_NAME_PART_ALT_UPPER = "OVERRIDE"; //$NON-NLS-1$

    private OverridableModuleUtil()
    {
        // Utility class, no instances
    }

    /**
     * Проверяет, что модуль — переопределяемый общий модуль: общий модуль,
     * в имени которого содержится «Переопределяемый»/«Override» (в любом
     * регистре), как в алгоритме АПК.
     *
     * @param module the module to test, may be {@code null}
     * @return true, if the module is an overridable common module
     */
    static boolean isOverridableCommonModule(Module module)
    {
        if (module == null || module.getModuleType() != ModuleType.COMMON_MODULE)
        {
            return false;
        }
        String name = getOwnerName(module);
        return isOverridableName(name);
    }

    /**
     * Проверяет, что имя содержит «Переопределяемый»/«Override»
     * (в любом регистре).
     */
    static boolean isOverridableName(String name)
    {
        if (name == null)
        {
            return false;
        }
        String upperName = name.toUpperCase(Locale.ROOT);
        return upperName.contains(RU_NAME_PART_UPPER) || upperName.contains(EN_NAME_PART_UPPER)
            || upperName.contains(EN_NAME_PART_ALT_UPPER);
    }

    /**
     * Возвращает имя объекта-владельца модуля (для общего модуля — имя самого
     * модуля); {@code null}, если владелец недоступен.
     */
    static String getOwnerName(Module module)
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
        return owner instanceof MdObject mdObject ? mdObject.getName() : null;
    }
}
