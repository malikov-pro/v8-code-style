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
 *     malikov-pro - port of the upstream issue #633 (std 644, APK 474)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.NAMED_ELEMENT__NAME;

import java.text.MessageFormat;
import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.documentation.comment.BslCommentUtils;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.RegionPreprocessor;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * В области «ДляВызоваИзДругихПодсистем» (англ. «InterfaceImplementation»)
 * у экспортного метода не указана подсистема-потребитель.
 * <p>
 * Каждый экспортный метод (процедура или функция), расположенный в области
 * «ДляВызоваИзДругихПодсистем» (в том числе во вложенной области),
 * должен иметь над объявлением комментарий-отметку потребителя — строку
 * вида {@code // Подсистема1.Подсистема2} (список подсистем через запятую,
 * опциональная завершающая точка). Метод без комментария или с
 * комментарием, не являющимся отметкой (обычный текст), помечается
 * замечанием на имя метода. Неэкспортные методы и методы вне этой области
 * не проверяются.
 * <p>
 * Заимствованные (adopted) модули проектов-расширений не проверяются.
 *
 * @author malikov-pro
 */
public class DfioNoConsumerCheck
    extends AbstractModuleStructureCheck
{

    /** Идентификатор проверки (апстрим-issue 1C-Company/v8-code-style#633). */
    public static final String CHECK_ID = "up-633-dfio-no-consumer"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public DfioNoConsumerCheck()
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
        builder.title(Messages.DfioNoConsumerCheck_title)
            .description(Messages.DfioNoConsumerCheck_description)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new SkipAdoptedInExtensionModuleOwnerExtension())
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(METHOD);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        Method method = (Method)object;
        if (monitor.isCanceled() || !method.isExport() || !isInsideDfioRegion(method))
        {
            return;
        }
        if (hasConsumerMarkingComment(method))
        {
            return;
        }
        String name = method.getName();
        if (name == null)
        {
            return;
        }
        resultAceptor.addIssue(
            MessageFormat.format(Messages.DfioNoConsumerCheck_No_consumer_comment, name),
            NAMED_ELEMENT__NAME);
    }

    /**
     * Метод расположен (на любом уровне вложенности областей) внутри
     * области «ДляВызоваИзДругихПодсистем».
     */
    private boolean isInsideDfioRegion(EObject object)
    {
        Optional<RegionPreprocessor> region = getFirstParentRegion(object);
        while (region.isPresent())
        {
            if (DfioRegionUtil.isDfioRegionName(region.get().getName()))
            {
                return true;
            }
            region = getFirstParentRegion(region.get());
        }
        return false;
    }

    /**
     * В скрытых листьях перед первым оператором метода есть
     * комментарий-отметка потребителя. Отсутствие узла трактуется как
     * «отметка есть» — недоказанное замечание не выдаётся.
     */
    private static boolean hasConsumerMarkingComment(Method method)
    {
        INode node = NodeModelUtils.findActualNodeFor(method);
        if (node == null)
        {
            return true;
        }
        for (ILeafNode leaf : node.getLeafNodes())
        {
            if (!leaf.isHidden())
            {
                // Дошли до первого оператора объявления метода.
                return false;
            }
            if (BslCommentUtils.isCommentNode(leaf) && DfioRegionUtil.isConsumerMarkingLine(leaf.getText()))
            {
                return true;
            }
        }
        return false;
    }
}
