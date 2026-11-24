/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;
import static com._1c.g5.v8.dt.mcore.McorePackage.Literals.NAMED_ELEMENT__NAME;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.RegionPreprocessor;
import com._1c.g5.v8.dt.bsl.model.util.BslUtil;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.bsl.ModuleStructureSection;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** One diagnostic per duplicated module-level region group; standard aliases follow pinned LS. */
public class DuplicateRegionCheck extends AbstractModuleStructureCheck
{
    public static final String CHECK_ID = "duplicate-region"; //$NON-NLS-1$

    private static final Map<String, String> STANDARD_NAMES = standardNames();

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.DuplicateRegionCheck_title)
            .description(Messages.DuplicateRegionCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.TRIVIAL)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module().checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Module module = (Module)object;
        Map<String, List<RegionPreprocessor>> groups = new LinkedHashMap<>();
        var regions = new ArrayList<>(BslUtil.getAllRegionPreprocessors(module));
        regions.removeIf(region -> NodeModelUtils.getNode(region) == null);
        regions.sort(Comparator.comparingInt(r -> NodeModelUtils.getNode(r).getOffset()));
        for (RegionPreprocessor region : regions)
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (isModuleLevelRegion(region) && region.getName() != null)
            {
                groups.computeIfAbsent(STANDARD_NAMES.getOrDefault(region.getName(), region.getName()),
                    key -> new ArrayList<>()).add(region);
            }
        }
        for (var group : groups.values())
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (group.size() > 1)
            {
                RegionPreprocessor first = group.get(0);
                INode name = NodeModelUtils.findNodesForFeature(first, NAMED_ELEMENT__NAME).stream()
                    .findFirst().orElse(null);
                INode node = NodeModelUtils.getNode(first);
                if (name != null && node != null)
                {
                    var keyword = java.util.stream.StreamSupport.stream(node.getLeafNodes().spliterator(), false)
                        .filter(leaf -> !leaf.isHidden()).findFirst().orElse(null);
                    if (keyword != null)
                    {
                        int start = keyword.getOffset();
                        if (keyword.getText().startsWith("#")) //$NON-NLS-1$
                        {
                            start++;
                        }
                        acceptor.addIssue(new BslDirectLocationIssue(MessageFormat.format(
                            Messages.DuplicateRegionCheck_message, first.getName()), new DirectLocation(
                                start, name.getEndOffset() - start,
                                keyword.getStartLine(), module)));
                    }
                }
            }
        }
    }

    private static Map<String, String> standardNames()
    {
        Map<String, String> names = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (ModuleStructureSection section : ModuleStructureSection.values())
        {
            // LS has neither Deprecated aliases nor a table-items prefix canonicalization.
            if (section != ModuleStructureSection.DEPRECATED_REGION && !section.isSuffixed())
            {
                String[] pair = section.getNames();
                names.put(pair[0], pair[0]);
                names.put(pair[1], pair[0]);
            }
        }
        return names;
    }
}
