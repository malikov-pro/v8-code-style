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
 *     malikov-pro - port of the BSL Language Server diagnostic MethodSize
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.NAMED_ELEMENT__NAME;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.WrongParameterException;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: размер метода (число строк тела) не превышает допустимый максимум
 * (по умолчанию 200 строк, настраивается параметром). Большой метод сложен
 * для восприятия и сопровождения.
 * <p>
 * Перенос диагностики BSL Language Server MethodSize
 * (тип CODE_SMELL, серьёзность MAJOR, параметр maxMethodSize = 200).
 *
 * @author malikov-pro
 */
public class MethodSizeCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "method-size"; //$NON-NLS-1$

    private static final String PARAM_MAX_METHOD_SIZE = "maxMethodSize"; //$NON-NLS-1$

    private static final Integer DEFAULT_MAX_METHOD_SIZE = Integer.valueOf(200);

    /**
     * Instantiates a new check.
     */
    public MethodSizeCheck()
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
        builder.title(Messages.MethodSizeCheck_title)
            .description(Messages.MethodSizeCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(METHOD)
            .parameter(PARAM_MAX_METHOD_SIZE, Integer.class, DEFAULT_MAX_METHOD_SIZE.toString(),
                Messages.MethodSizeCheck_Maximum_method_size);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Method method = (Method)object;
        List<Statement> statements = method.getStatements();
        if (statements.isEmpty())
        {
            return;
        }
        int maxMethodSize;
        try
        {
            maxMethodSize = parameters.getInt(PARAM_MAX_METHOD_SIZE);
        }
        catch (WrongParameterException e)
        {
            maxMethodSize = DEFAULT_MAX_METHOD_SIZE.intValue();
        }

        INode first = NodeModelUtils.findActualNodeFor(statements.get(0));
        INode last = NodeModelUtils.findActualNodeFor(statements.get(statements.size() - 1));
        if (first == null || last == null)
        {
            return;
        }
        int methodSize = last.getEndLine() - first.getStartLine() + 1;
        if (methodSize > maxMethodSize)
        {
            String message = MessageFormat.format(Messages.MethodSizeCheck_Method_is_too_large, method.getName(),
                methodSize, maxMethodSize);
            resultAceptor.addIssue(message, method, NAMED_ELEMENT__NAME);
        }
    }
}
