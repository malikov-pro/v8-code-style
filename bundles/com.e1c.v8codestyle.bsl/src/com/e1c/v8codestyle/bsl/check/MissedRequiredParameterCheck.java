/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslIndexPackage.Literals.MODULE_CONTEXT_DEF_INDEX;
import static com._1c.g5.v8.dt.bsl.model.BslIndexPackage.Literals.MODULE_CONTEXT_DEF_INDEX__METHODS;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;
import static org.eclipse.xtext.resource.impl.ResourceDescriptionsProvider.PERSISTED_DESCRIPTIONS;
import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.FEATURE_ACCESS__NAME;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.IResourceDescriptionsProvider;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.EmptyExpression;
import com._1c.g5.v8.dt.bsl.model.FeatureEntry;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleContextDefIndex;
import com._1c.g5.v8.dt.bsl.model.SourceObjectLinkProvider;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.resource.DynamicFeatureAccessComputer;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.mcore.Environmental;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckDefinition;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;
import com.google.inject.Inject;

/** Complements native arity validation with explicitly empty required arguments. */
public class MissedRequiredParameterCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "missed-required-parameter"; //$NON-NLS-1$
    private final DynamicFeatureAccessComputer dynamicComputer;
    private final IBmModelManager modelManager;
    private final IResourceDescriptionsProvider descriptionsProvider;

    @Inject
    public MissedRequiredParameterCheck(DynamicFeatureAccessComputer dynamicComputer, IBmModelManager modelManager,
        IResourceDescriptionsProvider descriptionsProvider)
    {
        this.dynamicComputer = dynamicComputer;
        this.modelManager = modelManager;
        this.descriptionsProvider = descriptionsProvider;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.MissedRequiredParameterCheck_title)
            .description(Messages.MissedRequiredParameterCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE);
    }

    @Override
    protected void postConfigureContextCollector(ICheckDefinition definition)
    {
        // ExportMethodProvider replaces the method list rather than mutating individual method records.
        // Native caller invalidation does not cover changes to optional argument positions.
        definition.addGenericModelFeatureChangeContextCollector((object, feature, event, session) -> {
            if (feature != MODULE_CONTEXT_DEF_INDEX__METHODS
                || !(object instanceof ModuleContextDefIndex moduleIndex) || moduleIndex.getContextDef() == null)
            {
                return;
            }
            var changed = moduleIndex.getContextDef().getModuleUri();
            if (changed == null || !changed.isPlatformResource())
            {
                return;
            }
            var resources = new ResourceSetImpl();
            resources.getLoadOptions().put(PERSISTED_DESCRIPTIONS, Boolean.TRUE);
            var index = descriptionsProvider.getResourceDescriptions(resources);
            var scheduled = new HashSet<String>();
            for (var description : index.getAllResourceDescriptions())
            {
                var resource = description.getURI();
                if (!resource.isPlatformResource() || !resource.segment(1).equals(changed.segment(1))
                    || resource.trimFragment().equals(changed.trimFragment()))
                {
                    continue;
                }
                for (var reference : description.getReferenceDescriptions())
                {
                    var source = reference.getSourceEObjectUri();
                    if (reference.getTargetEObjectUri().trimFragment().equals(changed.trimFragment())
                        && source.isPlatformResource() && !source.trimFragment().equals(changed.trimFragment()))
                    {
                        String consumer = source.trimFragment().toPlatformString(true);
                        if (scheduled.add(consumer))
                        {
                            session.addLanguageCheck(consumer);
                        }
                    }
                }
            }
        }, MODULE_CONTEXT_DEF_INDEX);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        for (Invocation call : EcoreUtil2.getAllContentsOfType((Module)object, Invocation.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            checkCall(call, acceptor);
        }
    }

    private void checkCall(Invocation call, ResultAcceptor acceptor)
    {
        if (call.getParams().stream().noneMatch(EmptyExpression.class::isInstance))
        {
            return;
        }
        Method method = resolveMethod(call);
        if (method == null || call.getParams().size() > method.getFormalParams().size())
        {
            return;
        }
        var formals = method.getFormalParams();
        // A missing trailing required argument is already an EDT arity issue.
        for (int i = call.getParams().size(); i < formals.size(); i++)
        {
            if (formals.get(i).getDefaultValue() == null)
            {
                return;
            }
        }
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < call.getParams().size(); i++)
        {
            if (call.getParams().get(i) instanceof EmptyExpression && formals.get(i).getDefaultValue() == null)
            {
                missing.add(formals.get(i).getName());
            }
        }
        if (missing.isEmpty())
        {
            return;
        }
        var node = NodeModelUtils.getNode(call);
        var names = NodeModelUtils.findNodesForFeature(call.getMethodAccess(), FEATURE_ACCESS__NAME);
        if (node == null || names.isEmpty())
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
        var name = names.get(0);
        acceptor.addIssue(new BslDirectLocationIssue(MessageFormat.format(Messages.MissedRequiredParameterCheck_message,
            String.join(", ", missing)), //$NON-NLS-1$
            new DirectLocation(name.getOffset(), node.getEndOffset() - name.getOffset(), name.getStartLine(), call)));
    }

    private Method resolveMethod(Invocation call)
    {
        List<FeatureEntry> entries;
        if (call.getMethodAccess() instanceof StaticFeatureAccess access)
        {
            entries = access.getFeatureEntries();
        }
        else if (call.getMethodAccess() instanceof DynamicFeatureAccess access)
        {
            Environmental environment = EcoreUtil2.getContainerOfType(call, Environmental.class);
            if (environment == null)
            {
                return null;
            }
            entries = dynamicComputer.resolveObject(access, environment.environments());
        }
        else
        {
            return null;
        }
        Method resolved = null;
        for (FeatureEntry entry : entries)
        {
            if (entry.getFeature() == null)
            {
                return null;
            }
            var feature = EcoreUtil.resolve(entry.getFeature(), call);
            if (feature instanceof SourceObjectLinkProvider link)
            {
                if (link.getSourceUri() == null)
                {
                    return null;
                }
                // Read through the current BM transaction, not a cached external resource snapshot.
                feature = modelManager.getModel(call).getEngine().getCurrentTransaction()
                    .getExternalObjectByUri(link.getSourceUri());
            }
            if (!(feature instanceof Method candidate) || candidate.eIsProxy())
            {
                return null;
            }
            if (resolved != null && resolved != candidate)
            {
                return null;
            }
            resolved = candidate;
        }
        return resolved;
    }
}
