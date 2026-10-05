/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: 1C-Soft LLC
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the BSL Language Server diagnostic SpaceAtStartComment
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Module;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.Issue;
import com.e1c.g5.v8.dt.check.WrongParameterException;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: между «//» и текстом комментария должен быть пробел.
 * <p>
 * Перенос диагностики BSL Language Server SpaceAtStartComment
 * (тип CODE_SMELL, серьёзность INFO). Как и в LS: не фиксируются
 * комментарии-аннотации (параметр, по умолчанию «//@,//(c),//©») и
 * комментарии, похожие на закомментированный код. Quick fix: см.
 * {@link com.e1c.v8codestyle.bsl.qfix.SpaceAtStartCommentFix}.
 *
 * @author malikov-pro
 */
public class SpaceAtStartCommentCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "space-at-start-comment"; //$NON-NLS-1$

    private static final String DEFAULT_COMMENTS_ANNOTATION = "//@,//(c),//©"; //$NON-NLS-1$

    // строгий шаблон LS: «// текст», «//» или «//   » (после // только пробелы/табы)
    private static final Pattern GOOD_COMMENT_PATTERN =
        Pattern.compile("(?:(?://[ \t].*)|(?://[ \t]*))$", Pattern.UNICODE_CASE); //$NON-NLS-1$

    private static final String PARAM_COMMENTS_ANNOTATION = "commentsAnnotation"; //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public SpaceAtStartCommentCheck()
    {
        super();
    }

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.SpaceAtStartCommentCheck_title)
            .description(Messages.SpaceAtStartCommentCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.TRIVIAL)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE)
            .parameter(PARAM_COMMENTS_ANNOTATION, String.class, DEFAULT_COMMENTS_ANNOTATION,
                Messages.SpaceAtStartCommentCheck_Comments_annotation);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Module module = (Module)object;

        Pattern annotations = annotationsPattern(parameters);
        CommentCodeRecognizer recognizer = new CommentCodeRecognizer(0.9);

        for (ILeafNode leaf : NodeModelUtils.findActualNodeFor(module).getLeafNodes())
        {
            if (!leaf.isHidden() || leaf.getSyntaxErrorMessage() != null)
            {
                continue;
            }
            // лист комментария включает завершающий перевод строки — проверяем по обрезанному тексту
            String text = leaf.getText().strip();
            if (!text.startsWith("//")) //$NON-NLS-1$
            {
                continue;
            }
            if (GOOD_COMMENT_PATTERN.matcher(text).matches() || annotations.matcher(text).matches()
                || recognizer.meetsCondition(text))
            {
                continue;
            }
            DirectLocation location =
                new DirectLocation(leaf.getOffset(), leaf.getLength(), leaf.getStartLine(), module);

            Issue issue = new BslDirectLocationIssue(Messages.SpaceAtStartCommentCheck_Space_at_comment_start,
                location);
            resultAceptor.addIssue(issue);
        }
    }

    private static Pattern annotationsPattern(ICheckParameters parameters)
    {
        String value;
        try
        {
            value = parameters.getString(PARAM_COMMENTS_ANNOTATION);
        }
        catch (WrongParameterException e)
        {
            value = DEFAULT_COMMENTS_ANNOTATION;
        }
        if (value == null || value.isBlank())
        {
            value = DEFAULT_COMMENTS_ANNOTATION;
        }
        StringBuilder builder = new StringBuilder("(?:^"); //$NON-NLS-1$
        for (String part : value.split(",")) //$NON-NLS-1$
        {
            if (builder.length() > 4)
            {
                builder.append('|');
            }
            builder.append(Pattern.quote(part.trim()));
        }
        builder.append(").*"); //$NON-NLS-1$
        return Pattern.compile(builder.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }
}
