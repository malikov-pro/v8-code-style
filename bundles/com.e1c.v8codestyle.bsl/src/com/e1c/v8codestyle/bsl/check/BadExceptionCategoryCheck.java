/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.RAISE_STATEMENT;

import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.RaiseStatement;
import com._1c.g5.v8.dt.bsl.resource.DynamicFeatureAccessComputer;
import com._1c.g5.v8.dt.mcore.Environmental;
import com._1c.g5.v8.dt.mcore.Property;
import com._1c.g5.v8.dt.mcore.Type;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;
import com.google.inject.Inject;

/** Checks reserved system categories in the functional Raise statement. */
public class BadExceptionCategoryCheck extends BasicCheck
{
    public static final String CHECK_ID = "bad-exception-category"; //$NON-NLS-1$
    private static final Set<String> FORBIDDEN = forbiddenCategories();
    private final DynamicFeatureAccessComputer computer;

    @Inject
    public BadExceptionCategoryCheck(DynamicFeatureAccessComputer computer)
    {
        this.computer = computer;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.BadExceptionCategoryCheck_title)
            .description(Messages.BadExceptionCategoryCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.TRIVIAL)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(RAISE_STATEMENT);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        RaiseStatement statement = (RaiseStatement)object;
        if (monitor.isCanceled() || statement.getExpressions().size() < 2
            || !(statement.getExpressions().get(1) instanceof DynamicFeatureAccess access))
        {
            return;
        }
        var environment = EcoreUtil2.getContainerOfType(access, Environmental.class);
        var node = NodeModelUtils.getNode(access);
        if (environment == null || node == null || !FORBIDDEN.contains(access.getName()))
        {
            return;
        }
        ILeafNode first = null;
        ILeafNode last = null;
        // Parentheses are unassigned grammar nodes: the semantic access node alone omits them.
        for (var leaf : NodeModelUtils.findActualNodeFor(access).getLeafNodes())
        {
            if (leaf.getSyntaxErrorMessage() != null)
            {
                return;
            }
            if (!leaf.isHidden())
            {
                if (first == null)
                {
                    first = leaf;
                }
                last = leaf;
            }
        }
        if (first == null || last == null || !FORBIDDEN.contains(last.getText()))
        {
            return;
        }
        var entries = computer.resolveObject(access, environment.environments());
        if (entries.isEmpty())
        {
            return;
        }
        for (var entry : entries)
        {
            if (entry.getFeature() == null)
            {
                return;
            }
            var feature = EcoreUtil.resolve(entry.getFeature(), access);
            var owner = EcoreUtil2.getContainerOfType(feature, Type.class);
            if (!(feature instanceof Property property) || property.eIsProxy() || owner == null || !owner.isSysEnum()
                || !("EnumErrorCategory".equalsIgnoreCase(owner.getName()) //$NON-NLS-1$
                    || "ПеречислениеКатегорияОшибки".equalsIgnoreCase(owner.getNameRu())) //$NON-NLS-1$
                || !(FORBIDDEN.contains(property.getName()) || FORBIDDEN.contains(property.getNameRu())))
            {
                return;
            }
        }
        acceptor.addIssue(new BslDirectLocationIssue(Messages.BadExceptionCategoryCheck_message,
            new DirectLocation(first.getOffset(), last.getEndOffset() - first.getOffset(), first.getStartLine(), access)));
    }

    private static Set<String> forbiddenCategories()
    {
        var result = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        result.addAll(Set.of("ВсеОшибки", "AllErrors", "ПрочаяОшибка", "OtherError", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            "ОшибкаКомпиляцииВстроенногоЯзыка", "ScriptCompileError", //$NON-NLS-1$ //$NON-NLS-2$
            "ОшибкаВоВремяВыполненияВстроенногоЯзыка", "ScriptRuntimeError", //$NON-NLS-1$ //$NON-NLS-2$
            "ИсключениеВызванноеИзВстроенногоЯзыка", "ScriptRaisedException")); //$NON-NLS-1$ //$NON-NLS-2$
        return java.util.Collections.unmodifiableSet(result);
    }
}
