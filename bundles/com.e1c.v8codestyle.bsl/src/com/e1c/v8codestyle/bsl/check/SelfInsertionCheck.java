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
 *     malikov-pro - port of the BSL Language Server diagnostic SelfInsertion
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: вставка коллекции в саму себя приводит к возникновению
 * циклических ссылок (например, «Список.Добавить(Список)»).
 * <p>
 * Перенос диагностики BSL Language Server SelfInsertion
 * (тип ERROR, серьёзность MAJOR).
 *
 * @author malikov-pro
 */
public class SelfInsertionCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "self-insertion"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public SelfInsertionCheck()
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
        builder.title(Messages.SelfInsertionCheck_title)
            .description(Messages.SelfInsertionCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(INVOCATION);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Invocation invocation = (Invocation)object;
        if (!(invocation.getMethodAccess() instanceof DynamicFeatureAccess))
        {
            return; // глобальные методы (без объекта) не проверяются
        }
        DynamicFeatureAccess methodAccess = (DynamicFeatureAccess)invocation.getMethodAccess();
        String methodName = methodAccess.getName();
        boolean insertMethod = "Вставить".equalsIgnoreCase(methodName) //$NON-NLS-1$
            || "Добавить".equalsIgnoreCase(methodName) //$NON-NLS-1$
            || "Insert".equalsIgnoreCase(methodName) //$NON-NLS-1$
            || "Add".equalsIgnoreCase(methodName); //$NON-NLS-1$
        if (!insertMethod || methodAccess.getSource() == null)
        {
            return;
        }
        String identifier = NodeModelUtils.findActualNodeFor(methodAccess.getSource()).getText().trim();
        List<Expression> params = invocation.getParams();
        for (Expression param : params)
        {
            String paramText = NodeModelUtils.findActualNodeFor(param).getText().trim();
            if (paramText.equalsIgnoreCase(identifier))
            {
                resultAceptor.addIssue(Messages.SelfInsertionCheck_Remove_self_insertion, invocation);
            }
        }
    }
}
