/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.DeclareStatement;
import com._1c.g5.v8.dt.bsl.model.ExplicitVariable;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.mcore.McorePackage;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Reports module variables without a description comment. A comment directly
 * above the declaration (annotations may stand between the comment and the
 * declaration, without a blank line) or on the declaration line itself counts
 * as a description; a blank line breaks the attachment.
 * <p>
 * Comments are not part of the BSL model, so the saved module text is read from
 * the workspace file and the declaration line is located by its node offset.
 * The model node text is not used: model nodes can be trimmed of leading trivia
 * while offsets stay absolute.
 */
public final class MissingVariablesDescriptionCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "missing-variables-description"; //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.MissingVariablesDescriptionCheck_title)
            .description(Messages.MissingVariablesDescriptionCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module().checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Module module = (Module)object;
        INode moduleNode = NodeModelUtils.findActualNodeFor(module);
        if (moduleNode == null)
        {
            return;
        }
        String document = documentText(module, moduleNode);
        if (document.startsWith("\uFEFF")) //$NON-NLS-1$
        {
            // A UTF-8 BOM is not whitespace for trim(); drop it before line analysis.
            document = document.substring(1);
        }
        String[] lines = document.replace("\r\n", "\n").split("\n", -1); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        for (DeclareStatement declare : module.allDeclareStatements())
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (EcoreUtil2.getContainerOfType(declare, Method.class) != null)
            {
                continue;
            }
            for (ExplicitVariable variable : declare.getVariables())
            {
                if (monitor.isCanceled())
                {
                    return;
                }
                INode node = NodeModelUtils.getNode(variable);
                if (node == null || described(lines, lineIndex(document, node.getOffset())))
                {
                    continue;
                }
                acceptor.addIssue(Messages.MissingVariablesDescriptionCheck_message, variable,
                    McorePackage.Literals.NAMED_ELEMENT__NAME);
            }
        }
    }

    private static String documentText(Module module, INode moduleNode)
    {
        if (module.eResource() != null)
        {
            try
            {
                String path = module.eResource().getURI().toPlatformString(true);
                if (path != null)
                {
                    IResource member = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(path));
                    if (member instanceof IFile file && file.exists())
                    {
                        try (InputStream input = file.getContents(true))
                        {
                            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
                        }
                    }
                }
            }
            catch (Exception ignored)
            {
                // Fall through to the node text.
            }
        }
        return moduleNode.getRootNode().getText();
    }

    private static int lineIndex(String text, int offset)
    {
        int end = Math.min(offset, text.length());
        int index = 0;
        for (int i = 0; i < end; i++)
        {
            if (text.charAt(i) == '\n')
            {
                index++;
            }
        }
        return index;
    }

    private static boolean described(String[] lines, int own)
    {
        if (own < 0 || own >= lines.length)
        {
            return false;
        }
        if (lines[own].contains("//")) //$NON-NLS-1$
        {
            return true;
        }
        for (int index = own - 1; index >= 0; index--)
        {
            String previous = lines[index].trim();
            if (previous.isEmpty())
            {
                return false;
            }
            if (previous.startsWith("&")) //$NON-NLS-1$
            {
                continue;
            }
            return previous.startsWith("//"); //$NON-NLS-1$
        }
        return false;
    }
}
