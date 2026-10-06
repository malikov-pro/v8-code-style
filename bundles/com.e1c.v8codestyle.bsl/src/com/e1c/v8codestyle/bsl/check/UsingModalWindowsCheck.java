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
 *     malikov-pro - port of the BSL Language Server diagnostic UsingModalWindows
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;

import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: использование модальных окон (Вопрос, ОткрытьФормуМодально,
 * ОткрытьЗначение, Предупреждение, ВвестиДату/Значение/Строку/Число).
 * Модальные вызовы не работают в веб-клиенте — используйте асинхронные
 * аналоги (ПоказатьВопрос, ОткрытьФорму, ПоказатьЗначение и т.д.).
 * <p>
 * Перенос диагностики BSL Language Server UsingModalWindows
 * (тип CODE_SMELL, серьёзность MAJOR). Проверяются глобальные вызовы,
 * как в LS. Quick fix не предусмотрен: асинхронные аналоги меняют
 * структуру кода (обработчик оповещения).
 *
 * @author malikov-pro
 */
public class UsingModalWindowsCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "using-modal-windows"; //$NON-NLS-1$

    private static final Set<String> MODAL_METHODS = Set.of(
        "вопрос", "querybox", //$NON-NLS-1$ //$NON-NLS-2$
        "открытьформумодально", "openformmodal", //$NON-NLS-1$ //$NON-NLS-2$
        "открытьзначение", "openvalue", //$NON-NLS-1$ //$NON-NLS-2$
        "предупреждение", "domessagebox", //$NON-NLS-1$ //$NON-NLS-2$
        "ввестидату", "inputdate", //$NON-NLS-1$ //$NON-NLS-2$
        "ввестизначение", "inputvalue", //$NON-NLS-1$ //$NON-NLS-2$
        "ввестистроку", "inputstring", //$NON-NLS-1$ //$NON-NLS-2$
        "ввестичисло", "inputnumber"); //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * Instantiates a new check.
     */
    public UsingModalWindowsCheck()
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
        builder.title(Messages.UsingModalWindowsCheck_title)
            .description(Messages.UsingModalWindowsCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(INVOCATION);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Invocation invocation = (Invocation)object;
        if (!(invocation.getMethodAccess() instanceof StaticFeatureAccess methodAccess))
        {
            return;
        }
        String name = methodAccess.getName().toLowerCase(Locale.ROOT);
        if (MODAL_METHODS.contains(name))
        {
            resultAceptor.addIssue(Messages.UsingModalWindowsCheck_Do_not_use_modal_windows, methodAccess);
        }
    }
}
