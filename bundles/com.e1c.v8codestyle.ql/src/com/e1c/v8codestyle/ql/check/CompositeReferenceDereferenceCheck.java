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
 *     malikov-pro - initial API and implementation
 *******************************************************************************/
package com.e1c.v8codestyle.ql.check;

import static com._1c.g5.v8.dt.ql.model.QlPackage.Literals.COMMON_EXPRESSION__CONTENT;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.mcore.TypeSet;
import com._1c.g5.v8.dt.metadata.dbview.DbViewElement;
import com._1c.g5.v8.dt.metadata.dbview.DbViewFieldDef;
import com._1c.g5.v8.dt.ql.model.MultiPartCommonExpression;
import com._1c.g5.v8.dt.ql.typesystem.IDynamicDbViewFieldComputer;
import com._1c.g5.v8.dt.ql.typesystem.IExpressionTypeChecker;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.WrongParameterException;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.g5.v8.dt.ql.check.QlBasicDelegateCheck;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.ql.CorePlugin;
import com.google.inject.Inject;

/** Checks proven DB-backed reference alternatives before each query dereference. */
public class CompositeReferenceDereferenceCheck
    extends QlBasicDelegateCheck
{
    /** Identifier of the standards-oriented query check. */
    public static final String CHECK_ID = "ql-composite-reference-dereference"; //$NON-NLS-1$

    /** Lower bound of distinct DB-backed reference alternatives. */
    public static final String PARAM_MIN_TYPES = "minReferenceTypes"; //$NON-NLS-1$

    private static final Set<String> DB_REFERENCE_PREFIXES = Set.of(
        "CatalogRef.", "DocumentRef.", "ExchangePlanRef.", "ChartOfAccountsRef.", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        "ChartOfCharacteristicTypesRef.", "ChartOfCalculationTypesRef.", "BusinessProcessRef.", "TaskRef."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    private final IDynamicDbViewFieldComputer computer;

    private final IExpressionTypeChecker typeChecker;

    /**
     * @param computer field model computer
     * @param typeChecker expression type computer for query projections
     */
    @Inject
    public CompositeReferenceDereferenceCheck(IDynamicDbViewFieldComputer computer, IExpressionTypeChecker typeChecker)
    {
        this.computer = computer;
        this.typeChecker = typeChecker;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.CompositeReferenceDereferenceCheck_title)
            .description(Messages.CompositeReferenceDereferenceCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.PERFORMANCE)
            .extension(new CommonSenseCheckExtension(CHECK_ID, CorePlugin.PLUGIN_ID))
            .delegate(MultiPartCommonExpression.class);
        builder.parameter(PARAM_MIN_TYPES, Integer.class, "2", Messages.CompositeReferenceDereferenceCheck_min_types); //$NON-NLS-1$
    }

    @Override
    protected void checkQlObject(EObject object, QueryOwner owner, IQlResultAcceptor resultAcceptor,
        ICheckParameters parameters, IProgressMonitor monitor)
    {
        MultiPartCommonExpression expression = (MultiPartCommonExpression)object;
        if (monitor.isCanceled() || expression.getSourceTable() == null)
        {
            return;
        }
        INode node = NodeModelUtils.findActualNodeFor(expression);
        if (node == null)
        {
            return;
        }
        for (var leaf : node.getLeafNodes())
        {
            if (leaf.getSyntaxErrorMessage() != null)
            {
                return;
            }
        }
        try
        {
            DbViewElement source = computer.computeDbView(expression.getSourceTable());
            if (!(source instanceof DbViewFieldDef field) || field.eIsProxy() || field.getType() == null)
            {
                return;
            }
            Set<String> references = new HashSet<>();
            Set<TypeItem> visiting = Collections.newSetFromMap(new IdentityHashMap<>());
            List<TypeItem> types = field.getType().getTypes();
            if (types.isEmpty())
            {
                // Query projections have an empty DbView type until the expression type is computed.
                var checked = typeChecker.checkType(expression.getSourceTable());
                if (checked == null || !checked.isValid || checked.expressionTypeResult == null)
                {
                    return;
                }
                types = checked.expressionTypeResult.getTypes();
            }
            for (TypeItem item : types)
            {
                if (!collectReferences(item, expression.getSourceTable(), references, visiting, monitor))
                {
                    return;
                }
            }
            if (!monitor.isCanceled() && references.size() >= minimumTypes(parameters))
            {
                String message = MessageFormat.format(Messages.CompositeReferenceDereferenceCheck_message,
                    expression.getSourceTable().getFullContent(), Integer.valueOf(references.size()));
                resultAcceptor.addIssue(message, expression, COMMON_EXPRESSION__CONTENT);
            }
        }
        catch (Exception e)
        {
            // A failed type computation is not proof of a composite reference.
            CorePlugin.logError(e);
        }
    }

    private static boolean collectReferences(TypeItem item, EObject context, Set<String> references,
        Set<TypeItem> visiting, IProgressMonitor monitor)
    {
        if (item != null && item.eIsProxy())
        {
            EObject resolved = EcoreUtil.resolve(item, context);
            if (!(resolved instanceof TypeItem type))
            {
                return false;
            }
            item = type;
        }
        if (monitor.isCanceled() || item == null || item.eIsProxy() || visiting.size() >= 16 || !visiting.add(item))
        {
            return false;
        }
        try
        {
            if (item instanceof TypeSet set)
            {
                var expanded = set.types(context);
                if (expanded == null || expanded.isEmpty())
                {
                    return false;
                }
                for (TypeItem type : expanded)
                {
                    if (!collectReferences(type, context, references, visiting, monitor))
                    {
                        return false;
                    }
                }
                return true;
            }
            String name = item.getName();
            if (name == null || name.isBlank())
            {
                return false;
            }
            for (String prefix : DB_REFERENCE_PREFIXES)
            {
                if (name.startsWith(prefix) && name.length() > prefix.length())
                {
                    references.add(name);
                    break;
                }
            }
            return true;
        }
        finally
        {
            visiting.remove(item);
        }
    }

    private static int minimumTypes(ICheckParameters parameters)
    {
        try
        {
            return Math.max(2, parameters.getInt(PARAM_MIN_TYPES));
        }
        catch (WrongParameterException e)
        {
            return 2;
        }
    }
}
