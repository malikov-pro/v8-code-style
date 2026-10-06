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
 *     malikov-pro - port of the APK check 00150 (register self-sufficiency)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
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
 * Проверка самодостаточности регистра: в модуле набора записей регистра не должно быть
 * обращений к реквизиту «Регистратор» (Recorder) через точку. Обращение к полям регистратора
 * ломает логическую независимость регистра от регистраторов, порождает неявные соединения
 * с дополнительными таблицами, а в распределённой ИБ регистратора может и не быть.
 * <p>
 * Перенос проверки АПК_00150 (статья 477 стандарта 1С). Реализация текстовая — по алгоритму
 * АПК: ищется вхождение «.Регистратор.» / «.Recorder.» (без учёта регистра) в тексте модуля,
 * включая комментарии и строковые литералы — обращения к регистратору в текстах запросов
 * являются основным случаем нарушения. Допустимые обращения (исключения алгоритма АПК):
 * <ul>
 * <li>«Отбор.Регистратор» / «Filter.Recorder»;</li>
 * <li>«СтандартныеРеквизиты.Регистратор» / «StandardAttributes.Recorder»;</li>
 * <li>«Регистратор.ТипЗначения» / «Recorder.ValueType»;</li>
 * <li>«Регистратор.Метаданные()» / «Recorder.Metadata()»;</li>
 * <li>«Регистратор.ПолучитьОбъект()» / «Recorder.GetObject()»;</li>
 * <li>«Регистратор.МоментВремени()» / «Recorder.MomentOfTime()».</li>
 * </ul>
 * <p>
 * Отличия от алгоритма АПК (эквивалентный результат поиска): исключения не вырезаются
 * из текста, а маскируются символом-заполнителем той же длины — смещения вхождений
 * сохраняются для позиционирования замечаний; строка кода в тексте замечания не дублируется
 * (маркер и так позиционируется на строку).
 *
 * @author malikov-pro
 */
public class ApkRegistrarAccessCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-00150-registrar-access"; //$NON-NLS-1$

    private static final char MASK_CHAR = '\u0001';

    private static final String WORD_RU = "Регистратор"; //$NON-NLS-1$
    private static final String WORD_EN = "Recorder"; //$NON-NLS-1$

    private static final List<String> EXCLUDED_PREFIXES_RU = List.of("Отбор", "СтандартныеРеквизиты"); //$NON-NLS-1$ //$NON-NLS-2$
    private static final List<String> EXCLUDED_PREFIXES_EN = List.of("Filter", "StandardAttributes"); //$NON-NLS-1$ //$NON-NLS-2$

    private static final List<String> EXCLUDED_MEMBERS_RU =
        List.of("ТипЗначения", "Метаданные()", "ПолучитьОбъект()", "МоментВремени()"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    private static final List<String> EXCLUDED_MEMBERS_EN =
        List.of("ValueType", "Metadata()", "GetObject()", "MomentOfTime()"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    private static final List<String> SEARCH_PATTERNS =
        List.of('.' + upper(WORD_RU) + '.', '.' + upper(WORD_EN) + '.');

    private static final List<String> EXCLUSIONS = buildExclusions();

    /**
     * Instantiates a new check.
     */
    public ApkRegistrarAccessCheck()
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
        builder.title(Messages.ApkRegistrarAccessCheck_title)
            .description(Messages.ApkRegistrarAccessCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.PERFORMANCE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module()
            .checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        Module module = (Module)object;
        // Правило выполняется на модулях набора записей регистров — единственных модулях объекта-регистра
        if (progressMonitor.isCanceled() || module.getModuleType() != ModuleType.RECORDSET_MODULE)
        {
            return;
        }
        INode moduleNode = NodeModelUtils.findActualNodeFor(module);
        if (moduleNode == null)
        {
            return;
        }

        // Полный текст модуля: конкатенация листьев (код, комментарии, строковые литералы, скрытое).
        List<ILeafNode> leaves = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        for (ILeafNode leaf : moduleNode.getLeafNodes())
        {
            leaves.add(leaf);
            text.append(leaf.getText());
        }

        char[] chars = text.toString().toUpperCase(Locale.ROOT).toCharArray();
        maskExclusions(chars);

        int[] leafStarts = new int[leaves.size()];
        int offset = 0;
        for (int i = 0; i < leaves.size(); i++)
        {
            leafStarts[i] = offset;
            offset += leaves.get(i).getText().length();
        }

        // Базовая строка — реальная строка первого листа (перед модулем могут быть ведущие комментарии)
        int startLine = leaves.isEmpty() ? moduleNode.getTotalStartLine() : leaves.get(0).getTotalStartLine();

        for (int i = 0; i < SEARCH_PATTERNS.size() && !progressMonitor.isCanceled(); i++)
        {
            findOccurrences(new String(chars), SEARCH_PATTERNS.get(i), leaves, leafStarts, module, startLine,
                resultAceptor);
        }
    }

    private static String upper(String value)
    {
        return value.toUpperCase(Locale.ROOT);
    }

    private static List<String> buildExclusions()
    {
        List<String> result = new ArrayList<>();
        result.addAll(EXCLUDED_PREFIXES_RU.stream().map(prefix -> upper(prefix + '.' + WORD_RU)).toList());
        result.addAll(EXCLUDED_PREFIXES_EN.stream().map(prefix -> upper(prefix + '.' + WORD_EN)).toList());
        result.addAll(EXCLUDED_MEMBERS_RU.stream().map(member -> upper(WORD_RU + '.' + member)).toList());
        result.addAll(EXCLUDED_MEMBERS_EN.stream().map(member -> upper(WORD_EN + '.' + member)).toList());
        return result;
    }

    /**
     * Маскирует исключения символом-заполнителем той же длины, чтобы не сместить позиции вхождений.
     */
    private static void maskExclusions(char[] chars)
    {
        String text = new String(chars);
        for (String exclusion : EXCLUSIONS)
        {
            int index = text.indexOf(exclusion);
            while (index >= 0)
            {
                for (int i = index; i < index + exclusion.length(); i++)
                {
                    chars[i] = MASK_CHAR;
                }
                index = text.indexOf(exclusion, index + exclusion.length());
            }
        }
    }

    private void findOccurrences(String text, String pattern, List<ILeafNode> leaves, int[] leafStarts,
        Module module, int startLine, ResultAcceptor resultAceptor)
    {
        int position = text.indexOf(pattern);
        while (position >= 0)
        {
            int newLines = 0;
            for (int i = 0; i < position; i++)
            {
                if (text.charAt(i) == '\n')
                {
                    newLines++;
                }
            }
            int line = startLine + newLines;

            int leafIndex = leafIndexByPosition(leafStarts, position);
            ILeafNode leaf = leaves.get(leafIndex);
            DirectLocation location =
                new DirectLocation(leaf.getOffset() + position - leafStarts[leafIndex], pattern.length(), line,
                    module);
            Issue issue = new BslDirectLocationIssue(
                MessageFormat.format(Messages.ApkRegistrarAccessCheck_Registrar_access_breaks_self_sufficiency, line),
                location);
            resultAceptor.addIssue(issue);

            position = text.indexOf(pattern, position + pattern.length());
        }
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
