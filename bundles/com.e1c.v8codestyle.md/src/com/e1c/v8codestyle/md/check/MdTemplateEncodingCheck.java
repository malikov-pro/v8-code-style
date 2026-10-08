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
 *     malikov-pro - port of the АПК rule АПК_00506
 *******************************************************************************/
package com.e1c.v8codestyle.md.check;

import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BASIC_TEMPLATE;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.BASIC_TEMPLATE__TEMPLATE_TYPE;
import static com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage.Literals.MD_OBJECT;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.dt.htmldocument.model.HtmlDocument;
import com._1c.g5.v8.dt.htmldocument.model.HtmlDocumentPage;
import com._1c.g5.v8.dt.metadata.mdclass.BasicTemplate;
import com._1c.g5.v8.dt.metadata.mdclass.TemplateType;
import com._1c.g5.v8.dt.textdocument.model.FileAwareTextDocument;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.md.CorePlugin;

/**
 * The content of a text document template and of an HTML template must use the
 * utf-8 encoding. A template whose content declares an encoding with the
 * "encoding=" or "charset=" attribute (case-insensitive) with a value other
 * than utf-8 is reported.
 * <p>
 * Templates whose content is empty or not accessible from the project model
 * are not checked.
 *
 * @author malikov-pro
 */
public class MdTemplateEncodingCheck
    extends BasicCheck
{

    /** The check id of the АПК_00506 rule port. */
    public static final String CHECK_ID = "apk-00506-template-encoding"; //$NON-NLS-1$

    /**
     * Matches the "encoding=..." or "charset=..." declaration with an optional
     * quote; the capture group is the encoding value. Declaration names and
     * encoding values are ASCII, so the content is scanned 8-bit safe.
     */
    private static final Pattern ENCODING_DECLARATION_PATTERN =
        Pattern.compile("(?i)(?:encoding|charset)\\s*=\\s*[\"']?([A-Za-z0-9_.\\-]+)"); //$NON-NLS-1$

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.MdTemplateEncodingCheck_title)
            .description(Messages.MdTemplateEncodingCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.PORTABILITY)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .extension(new SkipAdoptedInExtensionMdObjectExtension())
            .topObject(MD_OBJECT)
            .checkTop()
            .containment(BASIC_TEMPLATE)
            .features(BASIC_TEMPLATE__TEMPLATE_TYPE);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor monitor)
    {
        if (!(object instanceof BasicTemplate template) || monitor.isCanceled())
        {
            return;
        }

        TemplateType templateType = template.getTemplateType();
        if (templateType != TemplateType.TEXT_DOCUMENT && templateType != TemplateType.HTML_DOCUMENT)
        {
            return;
        }

        String name = template.getName();
        for (IFile file : contentFiles(template))
        {
            String nonUtf8Value = findNonUtf8Encoding(file);
            if (nonUtf8Value != null)
            {
                resultAceptor.addIssue(MessageFormat.format(Messages.MdTemplateEncodingCheck_Non_utf8_encoding,
                    name == null ? "" : name, nonUtf8Value)); //$NON-NLS-1$
                return;
            }
            if (monitor.isCanceled())
            {
                return;
            }
        }
    }

    private List<IFile> contentFiles(BasicTemplate template)
    {
        EObject content;
        try
        {
            content = template.getTemplate();
        }
        catch (RuntimeException e)
        {
            // The external content cannot be resolved in this context - the template is out of the scope.
            return List.of();
        }
        if (content instanceof FileAwareTextDocument textDocument)
        {
            IFile file = textDocument.getFile();
            return file == null ? List.of() : List.of(file);
        }
        if (content instanceof HtmlDocument htmlDocument)
        {
            List<IFile> files = new ArrayList<>();
            for (HtmlDocumentPage page : htmlDocument.getPages())
            {
                IFile file = page.getFile();
                if (file != null)
                {
                    files.add(file);
                }
            }
            return files;
        }
        // No content, a placeholder or another content type - the template is out of the scope.
        return List.of();
    }

    private String findNonUtf8Encoding(IFile file)
    {
        if (file == null || !file.isAccessible())
        {
            return null;
        }
        try (InputStream inputStream = file.getContents())
        {
            String text = new String(inputStream.readAllBytes(), StandardCharsets.ISO_8859_1);
            Matcher matcher = ENCODING_DECLARATION_PATTERN.matcher(text);
            while (matcher.find())
            {
                String value = matcher.group(1);
                if (!isUtf8(value))
                {
                    return value;
                }
            }
        }
        catch (CoreException | IOException e)
        {
            // The content is not readable - the template is out of the scope.
        }
        return null;
    }

    private static boolean isUtf8(String value)
    {
        String normalized = value.replace("-", "").replace("_", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        return normalized.equalsIgnoreCase("utf8"); //$NON-NLS-1$
    }
}
