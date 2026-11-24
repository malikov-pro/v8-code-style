/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the BSL Language Server diagnostic SeveralCompilerDirectives
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.DECLARE_STATEMENT;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.METHOD;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.NAMED_ELEMENT__NAME;

import org.eclipse.core.runtime.IProgressMonitor;

import com._1c.g5.v8.dt.bsl.model.DeclareStatement;
import com._1c.g5.v8.dt.bsl.model.ExplicitVariable;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Pragma;
import com._1c.g5.v8.dt.bsl.model.PragmaTarget;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Reports more than one compiler directive ({@code &НаКлиенте}, {@code &НаСервере}
 * and the like) on a module variable declaration or on a method.
 * <p>
 * Port of the BSL Language Server diagnostic SeveralCompilerDirectives. Only
 * compiler directives are counted: extension annotations ({@code &Вместо},
 * {@code &Перед}, …) carry a value in parentheses and are a different pragma
 * kind. Module variables and methods are checked separately, so the existing
 * module-var/module-method structure checks are not duplicated.
 */
public class SeveralCompilerDirectivesCheck
    extends BasicCheck
{
    private static final String CHECK_ID = "several-compiler-directives"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer configurer)
    {
        configurer.title(Messages.SeveralCompilerDirectivesCheck_title)
            .description(Messages.SeveralCompilerDirectivesCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(DECLARE_STATEMENT)
            .checkedObjectType(METHOD);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAcceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (monitor.isCanceled())
        {
            return;
        }
        if (object instanceof DeclareStatement declaration)
        {
            if (compilerDirectiveCount(declaration) > 1 && !declaration.getVariables().isEmpty())
            {
                ExplicitVariable variable = declaration.getVariables().get(0);
                resultAcceptor.addIssue(Messages.SeveralCompilerDirectivesCheck_message_variable, variable,
                    NAMED_ELEMENT__NAME);
            }
            return;
        }
        Method method = (Method)object;
        if (compilerDirectiveCount(method) > 1)
        {
            resultAcceptor.addIssue(Messages.SeveralCompilerDirectivesCheck_message_method, method,
                NAMED_ELEMENT__NAME);
        }
    }

    private static int compilerDirectiveCount(PragmaTarget target)
    {
        int count = 0;
        for (Pragma pragma : target.getPragmas())
        {
            if (!isExtensionAnnotation(pragma))
            {
                count++;
            }
        }
        return count;
    }

    private static boolean isExtensionAnnotation(Pragma pragma)
    {
        // Extension annotations take a method name in parentheses; compiler
        // directives have no value.
        return pragma.getValue() != null && !pragma.getValue().isBlank();
    }
}
