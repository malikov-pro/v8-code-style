/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.text.MessageFormat;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.IndexAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.core.platform.IConfigurationProvider;
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

/** Lexical residual of the pinned 8.3.12 safety-net; native deleted enums stay native. */
public class DeprecatedAttributes8312Check extends BasicCheck
{
    public static final String CHECK_ID = "deprecated-attributes-8312"; //$NON-NLS-1$
    private static final Set<String> CHART_ROOTS = names("Chart", "Диаграмма", "GanttChart", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        "ДиаграммаГанта", "PivotChart", "СводнаяДиаграмма"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    private static final Set<String> PLOT_ROOTS = names("ChartPlotArea", "ОбластьПостроенияДиаграммы"); //$NON-NLS-1$ //$NON-NLS-2$
    private static final Set<String> CHART_PROPERTIES = names("ShowLegend", "ОтображатьЛегенду", //$NON-NLS-1$ //$NON-NLS-2$
        "ShowTitle", "ОтображатьЗаголовок", "GradientPaletteMaxColors", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        "МаксимальноеКоличествоЦветовГрадиентнойПалитры"); //$NON-NLS-1$
    private static final Set<String> PLOT_PROPERTIES = names("ShowScale", "ОтображатьШкалу", //$NON-NLS-1$ //$NON-NLS-2$
        "ЛинииШкалы", "ЦветШкалы", "ShowSeriesScaleLabels", "ОтображатьПодписиШкалыСерий", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        "ShowPointsScaleLabels", "ОтображатьПодписиШкалыТочек", "ShowValuesScaleLabels", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        "ОтображатьПодписиШкалыЗначений", "ShowScaleValueLines", "ОтображатьЛинииЗначенийШкалы", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        "ValueScaleFormat", "ФорматШкалыЗначений", "LabelsOrientation", "ОриентацияМеток"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    private final IConfigurationProvider configurations;

    @Inject
    public DeprecatedAttributes8312Check(IConfigurationProvider configurations)
    {
        this.configurations = configurations;
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.DeprecatedAttributes8312Check_title)
            .description(Messages.DeprecatedAttributes8312Check_description)
            .complexity(CheckComplexity.NORMAL).severity(IssueSeverity.TRIVIAL).issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module().checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Module module = (Module)object;
        var configuration = configurations.getConfiguration(module);
        if (configuration == null)
        {
            return;
        }
        var mode = configuration.getCompatibilityMode();
        if (mode != null)
        {
            String[] version = mode.getLiteral().split("\\."); //$NON-NLS-1$
            if (version.length != 3 || Integer.parseInt(version[0]) < 8
                || Integer.parseInt(version[0]) == 8 && (Integer.parseInt(version[1]) < 3
                    || Integer.parseInt(version[1]) == 3 && Integer.parseInt(version[2]) < 12))
            {
                return;
            }
        }
        for (var access : EcoreUtil2.getAllContentsOfType(module, DynamicFeatureAccess.class))
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (access.eContainer() instanceof Invocation invocation && invocation.getMethodAccess() == access)
            {
                continue;
            }
            String root = rootName(access.getSource());
            if (!(CHART_ROOTS.contains(root) && CHART_PROPERTIES.contains(access.getName())
                || PLOT_ROOTS.contains(root) && PLOT_PROPERTIES.contains(access.getName())))
            {
                continue;
            }
            var node = NodeModelUtils.getNode(access);
            var source = NodeModelUtils.getNode(access.getSource());
            if (node == null || source == null)
            {
                continue;
            }
            int dot = -1;
            int end = -1;
            int line = 0;
            for (var leaf : node.getLeafNodes())
            {
                if (leaf.getSyntaxErrorMessage() != null)
                {
                    dot = -1;
                    break;
                }
                if (leaf.isHidden() || leaf.getOffset() < source.getEndOffset())
                {
                    continue;
                }
                if (dot < 0 && ".".equals(leaf.getText())) //$NON-NLS-1$
                {
                    dot = leaf.getOffset();
                    line = leaf.getStartLine();
                }
                if (dot >= 0 && access.getName().equalsIgnoreCase(leaf.getText()))
                {
                    end = leaf.getEndOffset();
                    break;
                }
            }
            if (dot >= 0 && end > dot)
            {
                acceptor.addIssue(new BslDirectLocationIssue(MessageFormat.format(
                    Messages.DeprecatedAttributes8312Check_message, access.getName()),
                    new DirectLocation(dot, end - dot, line, access)));
            }
        }
    }

    private String rootName(Expression expression)
    {
        while (expression != null)
        {
            if (expression instanceof DynamicFeatureAccess access)
            {
                expression = access.getSource();
            }
            else if (expression instanceof IndexAccess access)
            {
                expression = access.getSource();
            }
            else if (expression instanceof Invocation invocation)
            {
                expression = invocation.getMethodAccess();
            }
            else
            {
                return expression instanceof StaticFeatureAccess access ? access.getName() : ""; //$NON-NLS-1$
            }
        }
        return ""; //$NON-NLS-1$
    }

    private static Set<String> names(String... values)
    {
        var result = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        result.addAll(java.util.List.of(values));
        return java.util.Collections.unmodifiableSet(result);
    }
}
