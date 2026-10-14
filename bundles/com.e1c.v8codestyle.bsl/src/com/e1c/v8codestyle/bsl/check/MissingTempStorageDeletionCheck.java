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
 *     malikov-pro - port of the BSL Language Server diagnostic MissingTempStorageDeletion
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;

import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: получение данных из временного хранилища без последующего
 * удаления. Данные остаются в серверной памяти до завершения сеанса.
 * <p>
 * Перенос диагностики BSL Language Server MissingTempStorageDeletion
 * (тип CODE_SMELL, серьёзность CRITICAL). Упрощение относительно LS
 * Анализируются последующие по строкам операторы метода, включая вложенные.
 * Удаление должно иметь те же аргументы; пути выполнения не анализируются.
 * В отличие от LS, тело модуля не проверяется, прежний default включён сохранён.
 * Quick fix не предусмотрен: момент удаления определяется логикой метода.
 *
 * @author malikov-pro
 */
public class MissingTempStorageDeletionCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "missing-temp-storage-deletion"; //$NON-NLS-1$

    private static final String GET_RU = "получитьизвременногохранилища"; //$NON-NLS-1$
    private static final String GET_EN = "getfromtempstorage"; //$NON-NLS-1$
    private static final String DELETE_RU = "удалитьизвременногохранилища"; //$NON-NLS-1$
    private static final String DELETE_EN = "deletefromtempstorage"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public MissingTempStorageDeletionCheck()
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
        builder.title(Messages.MissingTempStorageDeletionCheck_title)
            .description(Messages.MissingTempStorageDeletionCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(METHOD);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Method method = (Method)object;

        List<Invocation> calls = EcoreUtil2.getAllContentsOfType(method, Invocation.class);
        List<Invocation> deletions = calls.stream().filter(call -> isCall(call, DELETE_RU, DELETE_EN)).toList();
        for (Invocation invocation : calls)
        {
            if (progressMonitor.isCanceled())
            {
                return;
            }
            if (isCall(invocation, GET_RU, GET_EN)
                && deletions.stream().noneMatch(deletion -> isMatchingLaterDeletion(invocation, deletion)))
            {
                resultAceptor.addIssue(
                    Messages.MissingTempStorageDeletionCheck_Missing_temp_storage_deletion, invocation);
            }
        }
    }

    private static boolean isCall(Invocation call, String ru, String en)
    {
        return call.getMethodAccess() instanceof StaticFeatureAccess access
            && (ru.equalsIgnoreCase(access.getName()) || en.equalsIgnoreCase(access.getName()));
    }

    private static boolean isMatchingLaterDeletion(Invocation get, Invocation delete)
    {
        INode getNode = NodeModelUtils.findActualNodeFor(get);
        Statement statement = EcoreUtil2.getContainerOfType(delete, Statement.class);
        INode deleteNode = NodeModelUtils.findActualNodeFor(statement);
        if (getNode == null || deleteNode == null || deleteNode.getStartLine() <= getNode.getEndLine()
            || get.getParams().size() != delete.getParams().size())
        {
            return false;
        }
        for (int i = 0; i < get.getParams().size(); i++)
        {
            Expression left = get.getParams().get(i);
            Expression right = delete.getParams().get(i);
            if (left.eClass() != right.eClass() || !tokens(left).equals(tokens(right)))
            {
                return false;
            }
        }
        return true;
    }

    private static List<String> tokens(Expression expression)
    {
        INode node = NodeModelUtils.findActualNodeFor(expression);
        if (node == null)
        {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(node.getLeafNodes().spliterator(), false)
            .filter(leaf -> !leaf.isHidden())
            .map(leaf -> leaf.getText().startsWith("\"") ? leaf.getText() //$NON-NLS-1$
                : leaf.getText().toLowerCase(Locale.ROOT))
            .toList();
    }
}
