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
 *     malikov-pro - port of the BSL Language Server diagnostic CognitiveComplexity
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.NAMED_ELEMENT__NAME;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.WrongParameterException;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка когнитивной сложности методов и тела модуля: структурная вложенность,
 * гибридные ветви, рекурсия, переходы и серии логических операций. CFG не требуется.
 * <p>
 * Перенос диагностики BSL Language Server CognitiveComplexity
 * (тип CODE_SMELL, серьёзность CRITICAL). Порог — параметр (по умолчанию 15,
 * как в LS).
 *
 * @author malikov-pro
 */
public class CognitiveComplexityCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "cognitive-complexity"; //$NON-NLS-1$

    private static final String PARAM_THRESHOLD = "complexityThreshold"; //$NON-NLS-1$

    private static final int DEFAULT_THRESHOLD = 15;

    /**
     * Instantiates a new check.
     */
    public CognitiveComplexityCheck()
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
        builder.title(Messages.CognitiveComplexityCheck_title)
            .description(Messages.CognitiveComplexityCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.WARNING)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE)
            .parameter(PARAM_THRESHOLD, Integer.class, String.valueOf(DEFAULT_THRESHOLD),
                Messages.CognitiveComplexityCheck_Complexity_threshold)
            .parameter("checkModuleBody", Boolean.class, "true", Messages.CognitiveComplexityCheck_Check_module_body); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Module module = (Module)object;
        int threshold = threshold(parameters);
        for (Method method : EcoreUtil2.getAllContentsOfType(module, Method.class))
        {
            if (progressMonitor.isCanceled())
            {
                return;
            }
            int complexity = LsComplexityComputer.method(method, true);
            if (complexity > threshold)
            {
                resultAceptor.addIssue(MessageFormat.format(Messages.CognitiveComplexityCheck_Cognitive_complexity,
                    Integer.valueOf(complexity), Integer.valueOf(threshold)), method, NAMED_ELEMENT__NAME);
            }
        }
        if (progressMonitor.isCanceled() || !parameters.getBoolean("checkModuleBody")) //$NON-NLS-1$
        {
            return;
        }
        int body = LsComplexityComputer.body(module, true);
        if (body > threshold && !module.allStatements().isEmpty())
        {
            var node = NodeModelUtils.findActualNodeFor(module.allStatements().get(0));
            if (node == null)
            {
                return;
            }
            for (ILeafNode leaf : node.getLeafNodes())
            {
                if (!leaf.isHidden())
                {
                    resultAceptor.addIssue(new BslDirectLocationIssue(
                        MessageFormat.format(Messages.CognitiveComplexityCheck_Cognitive_complexity,
                            Integer.valueOf(body), Integer.valueOf(threshold)),
                        new DirectLocation(leaf.getOffset(), leaf.getLength(), leaf.getStartLine(), module)));
                    break;
                }
            }
        }
    }

    private static int threshold(ICheckParameters parameters)
    {
        try
        {
            return parameters.getInt(PARAM_THRESHOLD);
        }
        catch (WrongParameterException e)
        {
            return DEFAULT_THRESHOLD;
        }
    }
}
