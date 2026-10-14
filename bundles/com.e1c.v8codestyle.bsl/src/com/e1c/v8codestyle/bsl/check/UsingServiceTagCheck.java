/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * Eclipse Public License 2.0: https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Module;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/** Checks service tags at comment prefixes, not arbitrary words in source text. */
public class UsingServiceTagCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "using-service-tag"; //$NON-NLS-1$
    public static final String PARAM_SERVICE_TAGS = "serviceTags"; //$NON-NLS-1$
    private static final String DEFAULT_TAGS = "todo|fixme|!!|mrg|@|отладка|debug|для\\s*отладки" //$NON-NLS-1$
        + "|(\\{\\{|\\}\\})КОНСТРУКТОР_|(\\{\\{|\\}\\})MRG" //$NON-NLS-1$
        + "|Вставить\\s*содержимое\\s*обработчика" //$NON-NLS-1$
        + "|Paste\\s*handler\\s*content|Insert\\s*handler\\s*code" //$NON-NLS-1$
        + "|Insert\\s*handler\\s*content|Insert\\s*handler\\s*contents"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.UsingServiceTagCheck_title)
            .description(Messages.UsingServiceTagCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.TRIVIAL)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE)
            .parameter(PARAM_SERVICE_TAGS, String.class, DEFAULT_TAGS, Messages.UsingServiceTagCheck_parameter);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Module module = (Module)object;
        Pattern pattern;
        try
        {
            pattern = Pattern.compile("//\\s*+(" + parameters.getString(PARAM_SERVICE_TAGS) + ")", //$NON-NLS-1$ //$NON-NLS-2$
                Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);
        }
        catch (PatternSyntaxException e)
        {
            BslPlugin.log(BslPlugin.createWarningStatus("Check " + CHECK_ID + ": invalid serviceTags regex: " //$NON-NLS-1$ //$NON-NLS-2$
                + e.getMessage()));
            return;
        }
        for (ILeafNode leaf : NodeModelUtils.findActualNodeFor(module).getLeafNodes())
        {
            if (monitor.isCanceled())
            {
                return;
            }
            String text = leaf.getText();
            if (!leaf.isHidden() || !text.startsWith("//") || !pattern.matcher(text).find()) //$NON-NLS-1$
            {
                continue;
            }
            int length = text.length();
            while (length > 0 && (text.charAt(length - 1) == '\n' || text.charAt(length - 1) == '\r'))
            {
                length--;
            }
            acceptor.addIssue(new BslDirectLocationIssue(Messages.UsingServiceTagCheck_message,
                new DirectLocation(leaf.getOffset(), length, leaf.getStartLine(), module)));
        }
    }
}
