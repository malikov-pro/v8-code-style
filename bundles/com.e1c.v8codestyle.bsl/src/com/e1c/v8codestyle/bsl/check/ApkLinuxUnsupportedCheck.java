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
 *     malikov-pro - port of the APK check 01364 (Linux unsupported methods)
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
import com.e1c.v8codestyle.check.OptInCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Проверка: в коде не используются методы и объекты, не поддерживаемые ОС Linux.
 * <p>
 * Перенос проверки АПК_01364 (статья 723 стандарта 1С). Реализация текстовая —
 * по алгоритму АПК: по полному тексту модуля (включая комментарии и строковые
 * литералы) без учёта регистра ищутся выражения:
 * <ul>
 * <li>объектные методы: «.УстановитьНевидимость(» / «.SetHidden(»,
 * «.УстановитьНевидимостьАсинх(» / «.SetHiddenAsync(» (объект Файл),
 * «.ПолучитьОтображениеЗаголовкаОС(» / «.GetOSCaptionRepresentation(»,
 * «.УстановитьОтображениеЗаголовкаОС(» / «.SetOSCaptionRepresentation(»
 * (объект КлиентскоеПриложение);</li>
 * <li>методы глобального контекста: «ЗагрузитьВнешнююКомпоненту(» /
 * «LoadAddIn(», «НачатьПодключениеВнешнейКомпоненты(» /
 * «BeginAttachingAddIn(», «ПодключитьВнешнююКомпоненту(» / «AttachAddIn(»,
 * «ПодключитьВнешнююКомпонентуАсинх(» / «AttachAddInAsync(»;</li>
 * <li>COM-вариант перечисления: «ТипВнешнейКомпоненты.COM» /
 * «AddInType.COM»;</li>
 * <li>конструкторы: «Новый Почта» / «New Mail», «Новый ИзвлечениеТекста» /
 * «New TextExtraction», «Новый COMОбъект» / «New COMObject».</li>
 * </ul>
 * Замечание — на каждое вхождение.
 * <p>
 * Отклонение от алгоритма АПК: предусловие «правило выполняется только в ОС
 * Linux» (ЭтоLinux()) конфигурационное и статически не воспроизводится,
 * поэтому проверка сделана аудитом — по умолчанию выключена.
 *
 * @author malikov-pro
 */
public class ApkLinuxUnsupportedCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-01364-linux-unsupported"; //$NON-NLS-1$

    /** Объектные методы (объекты Файл и КлиентскоеПриложение), рус/англ. */
    private static final List<String> OBJECT_METHOD_NAMES = List.of(
        "УстановитьНевидимость", "SetHidden", //$NON-NLS-1$ //$NON-NLS-2$
        "УстановитьНевидимостьАсинх", "SetHiddenAsync", //$NON-NLS-1$ //$NON-NLS-2$
        "ПолучитьОтображениеЗаголовкаОС", "GetOSCaptionRepresentation", //$NON-NLS-1$ //$NON-NLS-2$
        "УстановитьОтображениеЗаголовкаОС", "SetOSCaptionRepresentation"); //$NON-NLS-1$ //$NON-NLS-2$

    /** Методы подключения внешних компонент (глобальный контекст), рус/англ. */
    private static final List<String> GLOBAL_METHOD_NAMES = List.of(
        "ЗагрузитьВнешнююКомпоненту", "LoadAddIn", //$NON-NLS-1$ //$NON-NLS-2$
        "НачатьПодключениеВнешнейКомпоненты", "BeginAttachingAddIn", //$NON-NLS-1$ //$NON-NLS-2$
        "ПодключитьВнешнююКомпоненту", "AttachAddIn", //$NON-NLS-1$ //$NON-NLS-2$
        "ПодключитьВнешнююКомпонентуАсинх", "AttachAddInAsync"); //$NON-NLS-1$ //$NON-NLS-2$

    /** Перечисление типа внешней компоненты (COM-вариант), рус/англ. */
    private static final List<String> ADD_IN_TYPE_NAMES = List.of("ТипВнешнейКомпоненты", "AddInType"); //$NON-NLS-1$ //$NON-NLS-2$

    /** Объекты, не поддерживаемые ОС Linux, рус/англ. */
    private static final List<String> OBJECT_NAMES = List.of(
        "Почта", "Mail", //$NON-NLS-1$ //$NON-NLS-2$
        "ИзвлечениеТекста", "TextExtraction", //$NON-NLS-1$ //$NON-NLS-2$
        "COMОбъект", "COMObject"); //$NON-NLS-1$ //$NON-NLS-2$

    private static final String NEW_KEYWORD_RU = "Новый"; //$NON-NLS-1$
    private static final String NEW_KEYWORD_EN = "New"; //$NON-NLS-1$

    /**
     * Набор искомых выражений — в точности по алгоритму АПК (СформироватьМассивРусскихИАнглийскихЗначений):
     * «\.Имя\(», «\bИмя\(», «\bИмя\.COM\b» и «\bНовый\s+Имя\b» для рус и англ имён.
     */
    private static final List<Pattern> SEARCH_PATTERNS = buildSearchPatterns();

    /**
     * Instantiates a new check.
     */
    public ApkLinuxUnsupportedCheck()
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
        builder.title(Messages.ApkLinuxUnsupportedCheck_title)
            .description(Messages.ApkLinuxUnsupportedCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.PORTABILITY)
            .extension(new OptInCheckExtension())
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
                    MessageFormat.format(Messages.ApkLinuxUnsupportedCheck_Unsupported_construct, matcher.group(),
                        line),
                    location);
                resultAceptor.addIssue(issue);
            }
        }
    }

    private static List<Pattern> buildSearchPatterns()
    {
        List<Pattern> patterns = new ArrayList<>();
        for (String name : OBJECT_METHOD_NAMES)
        {
            patterns.add(compile("\\." + name + "\\(")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        for (String name : GLOBAL_METHOD_NAMES)
        {
            patterns.add(compile("\\b" + name + "\\(")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        for (String name : ADD_IN_TYPE_NAMES)
        {
            patterns.add(compile("\\b" + name + "\\.COM\\b")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        for (String newKeyword : List.of(NEW_KEYWORD_RU, NEW_KEYWORD_EN))
        {
            for (String objectName : OBJECT_NAMES)
            {
                patterns.add(compile("\\b" + newKeyword + "\\s+" + objectName + "\\b")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        }
        return List.copyOf(patterns);
    }

    private static Pattern compile(String regex)
    {
        // UNICODE_CHARACTER_CLASS: \b учитывает кириллицу; поиск без учёта регистра — как в АПК
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);
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
