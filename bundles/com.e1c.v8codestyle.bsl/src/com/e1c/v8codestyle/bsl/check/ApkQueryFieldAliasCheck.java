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
 *     malikov-pro - port of the APK check АПК_01216
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.util.EcoreUtil;
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
 * Проверка: псевдоним в тексте запроса совпадает с именем класса объектов
 * метаданных.
 * <p>
 * Перенос проверки АПК_01216 (статья 758 стандарта 1С). Псевдонимы источников
 * и полей запросов должны быть осмысленными; не следует использовать имена
 * классов объектов метаданных («Справочник», «Документ», Catalog, Document
 * и т.п.). Как и в алгоритме АПК — текстовый поиск по модулю: для каждого
 * вхождения ключевого слова «КАК»/«AS» (в каноническом регистре; для
 * канонического написания ключевых слов есть отдельная проверка) берётся
 * первое слово после ключевого слова, и если оно совпадает (без учёта
 * регистра) с именем класса объектов метаданных — фиксируется замечание.
 * Комментарии не проверяются; табы, переводы строк и разделители учитываются
 * как в исходном алгоритме. Каждое вхождение помечается отдельным замечанием.
 *
 * @author malikov-pro
 */
public class ApkQueryFieldAliasCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-01216-query-field-alias"; //$NON-NLS-1$

    private static final String KEYWORD_AS_RU = " КАК "; //$NON-NLS-1$

    private static final String KEYWORD_AS_EN = " AS "; //$NON-NLS-1$

    private static final char SPACE = ' ';

    // Имена классов объектов метаданных на языке запросов (рус/англ);
    // сравнение без учёта регистра, «ё» нормализована к «е»
    private static final Set<String> METADATA_CLASS_NAMES = Set.of(
        "СПРАВОЧНИК", "ДОКУМЕНТ", "ЖУРНАЛДОКУМЕНТОВ", "ПЕРЕЧИСЛЕНИЕ", "ОТЧЕТ", "ОБРАБОТКА", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        "ПЛАНОБМЕНА", "ПЛАНСЧЕТОВ", "ПЛАНВИДОВХАРАКТЕРИСТИК", "ПЛАНВИДОВРАСЧЕТА", "КОНСТАНТА", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        "ПОСЛЕДОВАТЕЛЬНОСТЬ", "РЕГИСТРСВЕДЕНИЙ", "РЕГИСТРНАКОПЛЕНИЯ", "РЕГИСТРБУХГАЛТЕРИИ", "РЕГИСТРРАСЧЕТА", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        "БИЗНЕСПРОЦЕСС", "ЗАДАЧА", //$NON-NLS-1$ //$NON-NLS-2$
        "CATALOG", "DOCUMENT", "DOCUMENTJOURNAL", "ENUMERATION", "REPORT", "DATAPROCESSOR", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        "EXCHANGEPLAN", "CHARTOFACCOUNTS", "CHARTOFCHARACTERISTICTYPES", "CHARTOFCALCULATIONTYPES", "CONSTANT", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        "SEQUENCE", "INFORMATIONREGISTER", "ACCUMULATIONREGISTER", "ACCOUNTINGREGISTER", "CALCULATIONREGISTER", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        "BUSINESSPROCESS", "TASK"); //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * Instantiates a new check.
     */
    public ApkQueryFieldAliasCheck()
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
        builder.title(Messages.ApkQueryFieldAliasCheck_title)
            .description(Messages.ApkQueryFieldAliasCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
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
        INode node = NodeModelUtils.findActualNodeFor(module);
        if (node == null)
        {
            return;
        }
        String moduleText = node.getText();
        String normalizedText = normalize(moduleText, node);
        String moduleName = EcoreUtil.getURI(module).lastSegment();

        searchKeyword(normalizedText, moduleText, KEYWORD_AS_RU, moduleName, module, resultAceptor);
        searchKeyword(normalizedText, moduleText, KEYWORD_AS_EN, moduleName, module, resultAceptor);
    }

    /**
     * Нормализованная копия текста той же длины (как в АПК): скрытые листья
     * (пробелы, комментарии) заменены пробелами; табы, переводы строк и
     * разделители «; ( ) { } ,» заменены пробелами — позиции не смещаются.
     */
    private static String normalize(String moduleText, INode node)
    {
        char[] characters = moduleText.toCharArray();
        for (ILeafNode leafNode : node.getLeafNodes())
        {
            if (leafNode.isHidden())
            {
                int offset = leafNode.getOffset();
                int end = Math.min(offset + leafNode.getLength(), characters.length);
                Arrays.fill(characters, offset, end, SPACE);
            }
        }
        for (int i = 0; i < characters.length; i++)
        {
            switch (characters[i])
            {
                case '\t':
                case '\n':
                case '\r':
                case ';':
                case '(':
                case ')':
                case '{':
                case '}':
                case ',':
                    characters[i] = SPACE;
                    break;
                default:
                    break;
            }
        }
        return new String(characters);
    }

    private void searchKeyword(String normalizedText, String moduleText, String keyword, String moduleName,
        Module module, ResultAcceptor resultAceptor)
    {
        int keywordIndex = normalizedText.indexOf(keyword);
        while (keywordIndex >= 0)
        {
            int aliasStart = keywordIndex + keyword.length();
            int aliasEnd = aliasStart;
            while (aliasEnd < normalizedText.length() && isAliasCharacter(normalizedText.charAt(aliasEnd)))
            {
                aliasEnd++;
            }
            if (aliasEnd > aliasStart)
            {
                String alias = normalizedText.substring(aliasStart, aliasEnd);
                String aliasUpper = alias.toUpperCase(Locale.ROOT).replace('Ё', 'Е');
                if (METADATA_CLASS_NAMES.contains(aliasUpper))
                {
                    int line = 1 + newLinesBefore(moduleText, aliasStart);
                    String message = MessageFormat.format(
                        Messages.ApkQueryFieldAliasCheck_Alias_matches_metadata_class_name, alias, moduleName,
                        line);
                    DirectLocation location = new DirectLocation(aliasStart, aliasEnd - aliasStart, line, module);
                    resultAceptor.addIssue(new BslDirectLocationIssue(message, location));
                }
            }
            keywordIndex = normalizedText.indexOf(keyword, keywordIndex + 1);
        }
    }

    private static boolean isAliasCharacter(char character)
    {
        return Character.isLetterOrDigit(character) || character == '_';
    }

    private static int newLinesBefore(String text, int position)
    {
        int count = 0;
        for (int i = 0; i < position && i < text.length(); i++)
        {
            if (text.charAt(i) == '\n')
            {
                count++;
            }
        }
        return count;
    }
}
