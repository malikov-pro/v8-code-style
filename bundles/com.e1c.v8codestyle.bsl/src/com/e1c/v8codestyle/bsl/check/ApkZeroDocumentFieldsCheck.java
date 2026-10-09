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
 *     malikov-pro - port of the APK rule АПК_00576
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
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
 * Проверка: у табличного документа не устанавливаются нулевые поля
 * (ПоляСлева/Справа/Сверху/Снизу = 0) — при печати с нулевыми полями
 * содержимое документа обрезается.
 * <p>
 * Перенос проверки АПК_00576 (статья 548 стандарта 1С). Реализация текстовая —
 * по алгоритму АПК: по полному тексту модуля (включая комментарии и строковые
 * литералы) без учёта регистра ищутся присваивания нуля свойствам полей
 * табличного документа:
 * <ul>
 * <li>русские имена: «.ПолеСлева», «.ПолеСправа», «.ПолеСверху», «.ПолеСнизу»;</li>
 * <li>английские имена: «.LeftMargin», «.RightMargin», «.TopMargin»,
 * «.BottomMargin».</li>
 * </ul>
 * Замечание — на каждое вхождение.
 * <p>
 * Отличия от алгоритма АПК: допускаются пробелы вокруг знака присваивания
 * («ПолеСлева = 0»), а также английские имена свойств (алгоритм ищет точную
 * подстроку только для русских имён). Как и в алгоритме АПК, присваивание не
 * отличимо от сравнения, поэтому вхождение вида «Если ТД.ПолеСлева = 0» также
 * помечается.
 *
 * @author malikov-pro
 */
public class ApkZeroDocumentFieldsCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00576-zero-document-fields"; //$NON-NLS-1$

    /** Свойства полей табличного документа, рус/англ. */
    private static final List<String> FIELD_PROPERTY_NAMES = List.of(
        "ПолеСлева", "LeftMargin", //$NON-NLS-1$ //$NON-NLS-2$
        "ПолеСправа", "RightMargin", //$NON-NLS-1$ //$NON-NLS-2$
        "ПолеСверху", "TopMargin", //$NON-NLS-1$ //$NON-NLS-2$
        "ПолеСнизу", "BottomMargin"); //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * Набор искомых выражений: «.Имя\s*=\s*0\b» для рус и англ имён свойств.
     */
    private static final List<Pattern> SEARCH_PATTERNS = buildSearchPatterns();

    /**
     * Instantiates a new check.
     */
    public ApkZeroDocumentFieldsCheck()
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
        builder.title(Messages.ApkZeroDocumentFieldsCheck_title)
            .description(Messages.ApkZeroDocumentFieldsCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MAJOR)
            .issueType(IssueType.ERROR)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Module module = (Module)object;
        INode moduleNode = NodeModelUtils.findActualNodeFor(module);
        if (progressMonitor.isCanceled() || moduleNode == null)
        {
            return;
        }

        // Полный текст модуля: конкатенация листьев (код, комментарии, строковые литералы, скрытое) —
        // поиск в комментариях и литералах выполняется, как в алгоритме АПК
        List<ILeafNode> leaves = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        for (ILeafNode leaf : moduleNode.getLeafNodes())
        {
            leaves.add(leaf);
            text.append(leaf.getText());
        }

        int[] leafStarts = new int[leaves.size()];
        int offset = 0;
        for (int i = 0; i < leaves.size(); i++)
        {
            leafStarts[i] = offset;
            offset += leaves.get(i).getText().length();
        }

        // Базовая строка — реальная строка первого листа (перед модулем могут быть ведущие комментарии)
        int startLine = leaves.isEmpty() ? moduleNode.getTotalStartLine() : leaves.get(0).getTotalStartLine();

        for (Pattern pattern : SEARCH_PATTERNS)
        {
            if (progressMonitor.isCanceled())
            {
                return;
            }
            Matcher matcher = pattern.matcher(text);
            while (matcher.find())
            {
                int position = matcher.start();
                int line = startLine + newLinesBefore(text, position);
                int leafIndex = leafIndexByPosition(leafStarts, position);
                ILeafNode leaf = leaves.get(leafIndex);
                DirectLocation location =
                    new DirectLocation(leaf.getOffset() + position - leafStarts[leafIndex],
                        matcher.end() - position, line, module);
                Issue issue = new BslDirectLocationIssue(
                    MessageFormat.format(Messages.ApkZeroDocumentFieldsCheck_Zero_document_field, matcher.group(), line),
                    location);
                resultAceptor.addIssue(issue);
            }
        }
    }

    private static List<Pattern> buildSearchPatterns()
    {
        List<Pattern> patterns = new ArrayList<>();
        for (String name : FIELD_PROPERTY_NAMES)
        {
            patterns.add(Pattern.compile("\\." + name + "\\s*=\\s*0\\b", //$NON-NLS-1$ //$NON-NLS-2$
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS));
        }
        return List.copyOf(patterns);
    }

    private static int newLinesBefore(CharSequence text, int position)
    {
        int count = 0;
        for (int i = 0; i < position; i++)
        {
            if (text.charAt(i) == '\n')
            {
                count++;
            }
        }
        return count;
    }

    private static int leafIndexByPosition(int[] leafStarts, int position)
    {
        int low = 0;
        int high = leafStarts.length - 1;
        int result = 0;
        while (low <= high)
        {
            int mid = (low + high) >>> 1;
            if (leafStarts[mid] <= position)
            {
                result = mid;
                low = mid + 1;
            }
            else
            {
                high = mid - 1;
            }
        }
        return result;
    }
}
