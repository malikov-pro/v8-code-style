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
 *     malikov-pro - checks for references in descriptions of deprecated methods
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
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;
import com.google.inject.Inject;

/**
 * Устаревшая процедура (функция) ссылается на несуществующую процедуру
 * (функцию) — issue #768 (АПК 1334), раздел 5.7 статьи 453 стандарта 1С.
 * <p>
 * В описании устаревшей процедуры (функции) — помеченной
 * «Устарела.»/«Deprecated.», размещённой в области
 * «УстаревшиеПроцедурыИФункции» либо имеющей признак устаревания в контексте
 * модуля — проверяются отсылки на замену «см. Модуль.ИмяМетода»
 * (англ. «see Module.Method»). Если общий модуль или целевая процедура
 * (функция) не найдены в конфигурации — замечание: отсылка ведёт в никуда
 * и вводит разработчиков в заблуждение.
 * <p>
 * Упрощения против исходной постановки: поиск цели ведётся по общим модулям
 * конфигурации (фильтр подсистем и областей модуля не воспроизводится);
 * проверяются только общие модули; ссылки на методы объектных модулей и
 * глобальный контекст не разрешаются. Ссылка на существующую, но устаревшую
 * цель — отдельная проверка (up-769-obsolete-refs-obsolete).
 *
 * @author malikov-pro
 */
public class DeprecatedMethodReferencesMissingCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс up-<issue> — задача апстрима). */
    public static final String CHECK_ID = "up-768-obsolete-refs-missing"; //$NON-NLS-1$

    private final IV8ProjectManager v8ProjectManager;

    /**
     * Instantiates a new check.
     *
     * @param v8ProjectManager the V8 project manager service, cannot be {@code null}
     */
    @Inject
    public DeprecatedMethodReferencesMissingCheck(IV8ProjectManager v8ProjectManager)
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
        builder.title(Messages.DeprecatedMethodReferencesMissingCheck_title)
            .description(Messages.DeprecatedMethodReferencesMissingCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .extension(ModuleTypeFilter.onlyTypes(ModuleType.COMMON_MODULE))
            .module()
            .checkedObjectType(METHOD);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        if (progressMonitor.isCanceled() || !(object instanceof Method method) || method.getName() == null)
        {
            return;
        }
        Module module = EcoreUtil2.getContainerOfType(method, Module.class);
        if (!DeprecatedMethodRefUtil.isDeprecated(module, method))
        {
            return;
        }
        String comment = OverridableModuleUtil.getPrecedingComment(module, method);
        List<DeprecatedMethodRefUtil.SeeReference> references = DeprecatedMethodRefUtil.findSeeReferences(comment);
        if (references.isEmpty())
        {
            return;
        }
        Configuration configuration = OverridableModuleUtil.getConfiguration(v8ProjectManager, method);
        if (configuration == null)
        {
            return;
        }
        Set<String> reported = new HashSet<>();
        for (DeprecatedMethodRefUtil.SeeReference reference : references)
        {
            CommonModule targetModule = OverridableModuleUtil.findCommonModule(configuration, reference.moduleName);
            Method targetMethod = targetModule == null
                ? null
                : OverridableModuleUtil.findMethodInModule(OverridableModuleUtil.getModuleOf(targetModule),
                    reference.methodName);
            if (targetMethod != null)
            {
                continue;
            }
            if (!reported.add(cacheKey(reference)))
            {
                continue;
            }
            resultAceptor.addIssue(MessageFormat.format(
                Messages.DeprecatedMethodReferencesMissingCheck_Deprecated_references_missing, method.getName(),
                reference.qualifiedName()), method, NAMED_ELEMENT__NAME);
        }
    }

    private static String cacheKey(DeprecatedMethodRefUtil.SeeReference reference)
    {
        return reference.moduleName.toLowerCase(Locale.ROOT) + "." //$NON-NLS-1$
            + reference.methodName.toLowerCase(Locale.ROOT);
    }
}
