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
 *     malikov-pro - port of the BSL Language Server diagnostic MissingSpace
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Module;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.Issue;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: отсутствие пробелов вокруг знаков операций и после запятой
 * и точки с запятой.
 * <p>
 * Перенос диагностики BSL Language Server MissingSpace
 * (тип CODE_SMELL, серьёзность INFO). Упрощение относительно LS: знаки
 * «= < > <= >= <> %» проверяются с обеих сторон, «+ - * /» — только слева
 * (правая сторона унарного знака в LS по умолчанию не проверяется),
 * «, ;» — только справа. Комментарии и строковые литералы не анализируются.
 *
 * @author malikov-pro
 */
public class MissingSpaceCheck
    extends BasicCheck
{

    /** Идентификатор проверки (совпадает с ключом диагностики BSL LS). */
    public static final String CHECK_ID = "missing-space"; //$NON-NLS-1$

    private static final Set<String> BOTH_SIDES = Set.of("=", "<>", "<=", ">=", "<", ">", "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    private static final Set<String> LEFT_ONLY = Set.of("+", "-", "*", "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    private static final Set<String> RIGHT_ONLY = Set.of(",", ";"); //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * Instantiates a new check.
     */
    public MissingSpaceCheck()
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
        builder.title(Messages.MissingSpaceCheck_title)
            .description(Messages.MissingSpaceCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.TRIVIAL)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Module module = (Module)object;

        List<ILeafNode> leaves = new java.util.ArrayList<>();
        NodeModelUtils.findActualNodeFor(module).getLeafNodes().forEach(leaves::add);

        for (int i = 0; i < leaves.size(); i++)
        {
            ILeafNode leaf = leaves.get(i);
            if (leaf.getSyntaxErrorMessage() != null)
            {
                continue;
            }
            String text = leaf.getText().strip();
            boolean checkBoth = BOTH_SIDES.contains(text);
            boolean checkLeft = checkBoth || LEFT_ONLY.contains(text);
            boolean checkRight = checkBoth || RIGHT_ONLY.contains(text);
            if (!checkLeft && !checkRight)
            {
                continue;
            }

            if (checkLeft && !hasWhitespaceBefore(leaves, i))
            {
                addIssue(resultAceptor, module, leaf, true);
            }
            if (checkRight && !hasWhitespaceAfter(leaves, i))
            {
                addIssue(resultAceptor, module, leaf, false);
            }
        }
    }

    /**
     * Между знаком и предыдущим листом есть пробел (либо знак в начале строки).
     */
    private static boolean hasWhitespaceBefore(List<ILeafNode> leaves, int index)
    {
        if (index == 0)
        {
            return true;
        }
        ILeafNode previous = leaves.get(index - 1);
        String previousText = previous.getText();
        if (previousText.isBlank())
        {
            return true;
        }
        // предыдущий непробельный лист заканчивает строку — пробел слева не нужен
        return previousText.endsWith("\n") || previousText.endsWith("\r"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * После знака есть пробел (либо знак в конце строки).
     */
    private static boolean hasWhitespaceAfter(List<ILeafNode> leaves, int index)
    {
        if (index >= leaves.size() - 1)
        {
            return true;
        }
        ILeafNode next = leaves.get(index + 1);
        String nextText = next.getText();
        if (nextText.isBlank())
        {
            return true;
        }
        // следующий непробельный лист начинается с новой строки — пробел справа не нужен
        return nextText.startsWith("\n") || nextText.startsWith("\r"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static void addIssue(ResultAcceptor resultAceptor, Module module, ILeafNode leaf, boolean left)
    {
        String side = left ? Messages.MissingSpaceCheck_Left : Messages.MissingSpaceCheck_Right;
        String message =
            MessageFormat.format(Messages.MissingSpaceCheck_Missing_space, side, leaf.getText().strip());
        DirectLocation location =
            new DirectLocation(leaf.getOffset(), leaf.getLength(), leaf.getStartLine(), module);
        Issue issue = new BslDirectLocationIssue(message, location);
        resultAceptor.addIssue(issue);
    }
}
