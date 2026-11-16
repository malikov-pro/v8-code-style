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
 *     malikov-pro - port of the BSL Language Server diagnostic WrongUseFunctionProceedWithCall
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.INVOCATION;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.common.Symbols;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Pragma;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.lcore.util.CaseInsensitiveString;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Reports {@code ПродолжитьВызов}/{@code ProceedWithCall} outside a method with
 * exactly one {@code &Вместо}/{@code &Around} annotation: the platform has no
 * original call point there, so the call fails at run time.
 * <p>
 * Port of the BSL Language Server diagnostic WrongUseFunctionProceedWithCall.
 * The pinned LS accepts only {@code &Вместо}; {@code &ИзменениеИКонтроль} is the
 * annotation for a method without {@code ПродолжитьВызов} (1C:Standard v8cs
 * change-and-validate-instead-of-around, platform 8.3.16+), so it is reported as
 * well. A call outside any method is not reported, as in the pinned LS.
 */
public class WrongUseFunctionProceedWithCallCheck
    extends BasicCheck
{
    private static final String CHECK_ID = "wrong-use-function-proceed-with-call"; //$NON-NLS-1$

    private static final String PROCEED_WITH_CALL = "ProceedWithCall"; //$NON-NLS-1$

    private static final String PROCEED_WITH_CALL_RUS = "ПродолжитьВызов"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer configurer)
    {
        configurer.title(Messages.WrongUseFunctionProceedWithCallCheck_title)
            .description(Messages.WrongUseFunctionProceedWithCallCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.BLOCKER)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(INVOCATION);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAcceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (monitor.isCanceled())
        {
            return;
        }
        Invocation invocation = (Invocation)object;
        FeatureAccess methodAccess = invocation.getMethodAccess();
        if (!(methodAccess instanceof StaticFeatureAccess))
        {
            // Only global calls; a call of an object method is not a continuation.
            return;
        }
        String name = methodAccess.getName();
        if (name == null
            || !(PROCEED_WITH_CALL.equalsIgnoreCase(name) || PROCEED_WITH_CALL_RUS.equalsIgnoreCase(name)))
        {
            return;
        }
        Method method = EcoreUtil2.getContainerOfType(invocation, Method.class);
        if (method == null)
        {
            return;
        }
        long aroundCount = 0;
        for (Pragma pragma : method.getPragmas())
        {
            if (Symbols.AROUND_ANNOTATION_SYMBOLS.contains(new CaseInsensitiveString(pragma.getSymbol())))
            {
                aroundCount++;
            }
        }
        if (aroundCount == 1)
        {
            return;
        }
        resultAcceptor.addIssue(Messages.WrongUseFunctionProceedWithCallCheck_message, methodAccess);
    }
}
