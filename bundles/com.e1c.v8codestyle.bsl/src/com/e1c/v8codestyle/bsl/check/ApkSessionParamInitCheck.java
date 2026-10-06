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
 *     malikov-pro - port of the APK check 00074 (session parameters init)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.SIMPLE_STATEMENT;

import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
import com._1c.g5.v8.dt.bsl.model.SimpleStatement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: присваивание значений параметрам сеанса
 * («ПараметрыСеанса.&lt;Имя&gt; = …», англ. «SessionParameters.&lt;Name&gt; = …»)
 * допустимо только в модуле сеанса. В любом другом модуле присваивание
 * фиксируется. Чтение параметра сеанса не проверяется.
 * <p>
 * Перенос проверки АПК_00074 «Инициализацию параметров сеанса следует
 * выполнять в модуле сеанса». Тип модуля определяется по
 * {@link Module#getModuleType()} модуля, содержащего оператор присваивания
 * ({@code SESSION_MODULE} — единственное допустимое место).
 *
 * @author malikov-pro
 */
public class ApkSessionParamInitCheck
    extends BasicCheck
{

    /** Идентификатор проверки (код АПК + краткий слаг). */
    public static final String CHECK_ID = "apk-00074-session-params-init"; //$NON-NLS-1$

    private static final Set<String> SESSION_PARAMETER_COLLECTIONS = Set.of("параметрысеанса", "sessionparameters"); //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * Instantiates a new check.
     */
    public ApkSessionParamInitCheck()
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
        builder.title(Messages.ApkSessionParamInitCheck_title)
            .description(Messages.ApkSessionParamInitCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(SIMPLE_STATEMENT);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (monitor.isCanceled() || !(object instanceof SimpleStatement statement))
        {
            return;
        }

        Module module = EcoreUtil2.getContainerOfType(statement, Module.class);
        if (module == null || module.getModuleType() == ModuleType.SESSION_MODULE)
        {
            return;
        }

        if (!(statement.getLeft() instanceof DynamicFeatureAccess parameter)
            || !(parameter.getSource() instanceof StaticFeatureAccess collection))
        {
            return;
        }

        String name = collection.getName();
        if (name != null && SESSION_PARAMETER_COLLECTIONS.contains(name.toLowerCase(Locale.ROOT)))
        {
            resultAceptor.addIssue(Messages.ApkSessionParamInitCheck_Session_parameter_assignment, parameter);
        }
    }
}
