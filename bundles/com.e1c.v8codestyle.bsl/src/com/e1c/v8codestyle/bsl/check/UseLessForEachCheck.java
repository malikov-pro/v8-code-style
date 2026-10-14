/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * This program and the accompanying materials are made available under the
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 * Contributors: malikov-pro - initial API and implementation
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FEATURE_ACCESS__NAME;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;

import com._1c.g5.v8.dt.bsl.model.ExplicitVariable;
import com._1c.g5.v8.dt.bsl.model.ForEachStatement;
import com._1c.g5.v8.dt.bsl.model.ForStatement;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Reports For Each iterators unused in the loop body. */
public class UseLessForEachCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "use-less-for-each"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.UseLessForEachCheck_title)
            .description(Messages.UseLessForEachCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.CRITICAL)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Module module = (Module)object;
        for (ForEachStatement loop : EcoreUtil2.getAllContentsOfType(module, ForEachStatement.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            StaticFeatureAccess iterator = loop.getVariableAccess();
            if (iterator == null || isModuleOrGlobal(iterator, module))
            {
                continue;
            }
            boolean used = false;
            for (Statement statement : loop.getStatements())
            {
                List<StaticFeatureAccess> accesses = EcoreUtil2.getAllContentsOfType(statement, StaticFeatureAccess.class);
                for (StaticFeatureAccess access : accesses)
                {
                    if (access.eContainer() instanceof Invocation invocation && invocation.getMethodAccess() == access
                        || access.eContainer() instanceof ForStatement nested && nested.getVariableAccess() == access)
                    {
                        continue;
                    }
                    if (sameVariable(iterator, access))
                    {
                        used = true;
                        break;
                    }
                }
                if (used)
                {
                    break;
                }
            }
            if (!used)
            {
                acceptor.addIssue(Messages.UseLessForEachCheck_message, iterator, FEATURE_ACCESS__NAME);
            }
        }
    }

    private static EObject variable(StaticFeatureAccess access)
    {
        if (!access.getFeatureEntries().isEmpty() && access.getFeatureEntries().get(0).getFeature() instanceof Variable)
        {
            return access.getFeatureEntries().get(0).getFeature();
        }
        return access.getImplicitVariable();
    }

    private static boolean sameVariable(StaticFeatureAccess iterator, StaticFeatureAccess access)
    {
        if (!iterator.getName().equalsIgnoreCase(access.getName()))
        {
            return false;
        }
        EObject iteratorVariable = variable(iterator);
        EObject accessVariable = variable(access);
        return iteratorVariable == null || accessVariable == null || iteratorVariable == accessVariable;
    }

    private static boolean isModuleOrGlobal(StaticFeatureAccess iterator, Module module)
    {
        EObject resolved = variable(iterator);
        if (resolved instanceof ExplicitVariable && EcoreUtil2.getContainerOfType(resolved, Method.class) == null)
        {
            return true;
        }
        // Declarations are checked too: resolution may still be incomplete at validation time.
        return module.allDeclareStatements().stream().flatMap(declaration -> declaration.getVariables().stream())
            .anyMatch(variable -> variable.getName().equalsIgnoreCase(iterator.getName()));
    }
}
