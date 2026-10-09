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
 *     malikov-pro - port of the upstream issue #634 (std 644, APK 475)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.documentation.comment.BslCommentUtils;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.RegionPreprocessor;
import com._1c.g5.v8.dt.bsl.model.util.BslUtil;
import com._1c.g5.v8.dt.core.platform.IConfigurationProvider;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.Subsystem;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;
import com.google.inject.Inject;

/**
 * В комментарии внутри области «ДляВызоваИзДругихПодсистем» (англ.
 * «InterfaceImplementation») указана несуществующая подсистема.
 * <p>
 * Комментарий-отметка потребителя вида {@code // Подсистема1.Подсистема2}
 * должен называть подсистемы, существующие в конфигурации: путь
 * разрешается от верхнеуровневой подсистемы к вложенной (без учёта
 * регистра). Путь из нескольких сегментов, который не разрешается в
 * существующую подсистему, помечается замечанием на комментарий.
 * <p>
 * Упрощения против «идеального» алгоритма (апстрим-issue #634, АПК 475):
 * одиночное имя из одного сегмента, не совпавшее ни с одной подсистемой,
 * не помечается — такая строка не отличима от обычного текстового
 * комментария («// Устарела» и т.п.). Если конфигурацию проекта разрешить
 * не удалось или в ней нет ни одной подсистемы, проверка ничего не
 * сообщает (инвариант недоказуем). Закрывающие маркеры
 * «// Конец …»/«// End …» отметками не считаются.
 * <p>
 * Заимствованные (adopted) модули проектов-расширений не проверяются.
 *
 * @author malikov-pro
 */
public class DfioNonExistentSubsystemCheck
    extends BasicCheck
{

    /** Идентификатор проверки (апстрим-issue 1C-Company/v8-code-style#634). */
    public static final String CHECK_ID = "up-634-dfio-nonexistent-subsystem"; //$NON-NLS-1$

    private final IConfigurationProvider configurationProvider;

    /**
     * Instantiates a new check.
     *
     * @param configurationProvider the configuration provider service, cannot be {@code null}.
     */
    @Inject
    public DfioNonExistentSubsystemCheck(IConfigurationProvider configurationProvider)
    {
        super();
        this.configurationProvider = configurationProvider;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.DfioNonExistentSubsystemCheck_title)
            .description(Messages.DfioNonExistentSubsystemCheck_description)
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
        if (monitor.isCanceled())
        {
            return;
        }
        Module module = (Module)object;
        Configuration configuration = configurationProvider.getConfiguration(module);
        if (configuration == null || configuration.getSubsystems().isEmpty())
        {
            // Дерево подсистем недоступно или пусто — инвариант недоказуем.
            return;
        }

        for (RegionPreprocessor region : BslUtil.getAllRegionPreprocessors(module))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (!DfioRegionUtil.isDfioRegionName(region.getName()))
            {
                continue;
            }
            checkRegionComments(configuration, module, region, resultAceptor, monitor);
        }
    }

    private void checkRegionComments(Configuration configuration, Module module, RegionPreprocessor region,
        ResultAcceptor resultAceptor, IProgressMonitor monitor)
    {
        INode node = NodeModelUtils.findActualNodeFor(region);
        if (node == null)
        {
            return;
        }
        for (ILeafNode leaf : node.getLeafNodes())
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (!leaf.isHidden() || !BslCommentUtils.isCommentNode(leaf))
            {
                continue;
            }
            for (List<String> path : DfioRegionUtil.parseConsumerPaths(leaf.getText()))
            {
                if (resolves(configuration, path))
                {
                    continue;
                }
                String subsystemName = String.join(".", path); //$NON-NLS-1$
                String message = MessageFormat.format(Messages.DfioNonExistentSubsystemCheck_Subsystem_does_not_exist,
                    subsystemName);
                DirectLocation location =
                    new DirectLocation(leaf.getOffset(), leaf.getLength(), leaf.getStartLine(), module);
                resultAceptor.addIssue(new BslDirectLocationIssue(message, location));
                // Одно замечание на комментарий-отметку.
                break;
            }
        }
    }

    /**
     * Разрешается ли путь подсистемы в существующую подсистему
     * конфигурации. Путь из одного сегмента ищется во всём дереве
     * (верхнеуровневая или вложенная подсистема); путь из нескольких
     * сегментов разрешается иерархически от верхнеуровневой подсистемы.
     */
    private static boolean resolves(Configuration configuration, List<String> path)
    {
        if (path.size() == 1)
        {
            return findInTree(configuration.getSubsystems(), path.get(0));
        }
        Subsystem current = findByName(configuration.getSubsystems(), path.get(0));
        for (int i = 1; i < path.size() && current != null; i++)
        {
            current = findByName(current.getSubsystems(), path.get(i));
        }
        return current != null;
    }

    private static boolean findInTree(List<Subsystem> subsystems, String name)
    {
        for (Subsystem subsystem : subsystems)
        {
            if (name.equalsIgnoreCase(subsystem.getName()))
            {
                return true;
            }
            if (findInTree(subsystem.getSubsystems(), name))
            {
                return true;
            }
        }
        return false;
    }

    private static Subsystem findByName(List<Subsystem> subsystems, String name)
    {
        for (Subsystem subsystem : subsystems)
        {
            if (name.equalsIgnoreCase(subsystem.getName()))
            {
                return subsystem;
            }
        }
        return null;
    }
}
