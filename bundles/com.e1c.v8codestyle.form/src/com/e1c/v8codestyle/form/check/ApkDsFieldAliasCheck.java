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
 *     malikov-pro - port of the APK check АПК_01214
 *******************************************************************************/
package com.e1c.v8codestyle.form.check;

import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.ABSTRACT_FORM_ATTRIBUTE;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.DYNAMIC_LIST_EXT_INFO;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.FORM;
import static com._1c.g5.v8.dt.form.model.FormPackage.Literals.FORM_ATTRIBUTE__EXT_INFO;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.core.event.BmSubEvent;
import com._1c.g5.v8.dt.form.model.DynamicListExtInfo;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormAttribute;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.ICheckDefinition;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.components.IBasicCheckExtension;
import com.e1c.g5.v8.dt.check.context.CheckContextCollectingSession;
import com.e1c.g5.v8.dt.check.context.OnModelFeatureChangeContextCollector;
import com.e1c.g5.v8.dt.check.context.OnModelObjectAssociationContextCollector;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.form.CorePlugin;

/**
 * Проверка: псевдоним поля запроса динамического списка формы совпадает
 * с именем класса объектов метаданных.
 * <p>
 * Перенос проверки АПК_01214 (статья 758 стандарта 1С). Псевдонимы источников
 * и полей запросов должны быть осмысленными; не следует использовать имена
 * классов объектов метаданных («Справочник», «Документ», Catalog, Document
 * и т.п.). Источник — текст запроса (queryText) реквизита формы с типом
 * «ДинамическийСписок». Как и в алгоритме АПК — текстовый поиск: для каждого
 * вхождения ключевого слова «КАК»/«AS» (в каноническом регистре; для
 * канонического написания ключевых слов есть отдельная проверка) берётся
 * первое слово после ключевого слова, и если оно совпадает (без учёта
 * регистра, «ё» нормализована к «е») с именем класса объектов метаданных —
 * фиксируется замечание. Каждое вхождение помечается отдельным замечанием.
 * <p>
 * Отличия от алгоритма АПК: комментарии «//» в тексте запроса исключаются
 * из поиска (как комментарии исключены в текстовом анализе АПК_01216),
 * разделители заменяются с сохранением позиций.
 *
 * @author malikov-pro
 */
public class ApkDsFieldAliasCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-01214-ds-field-alias"; //$NON-NLS-1$

    private static final String KEYWORD_AS_RU = " КАК "; //$NON-NLS-1$

    private static final String KEYWORD_AS_EN = " AS "; //$NON-NLS-1$

    private static final char SPACE = ' ';

    private static final String LINE_COMMENT = "//"; //$NON-NLS-1$

    // Имена классов объектов метаданных на языке запросов (рус/англ);
    // сравнение без учёта регистра, «ё» нормализована к «е».
    // Список синхронизирован с apk-01216-query-field-alias (bsl-канал)
    private static final Set<String> METADATA_CLASS_NAMES = Set.of(
        "СПРАВОЧНИК", "ДОКУМЕНТ", "ЖУРНАЛДОКУМЕНТОВ", "ПЕРЕЧИСЛЕНИЕ", "ОТЧЕТ", "ОБРАБОТКА", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        "ПЛАНОБМЕНА", "ПЛАНСЧЕТОВ", "ПЛАНВИДОВХАРАКТЕРИСТИК", "ПЛАНВИДОВРАСЧЕТА", "КОНСТАНТА", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        "ПОСЛЕДОВАТЕЛЬНОСТЬ", "РЕГИСТРСВЕДЕНИЙ", "РЕГИСТРНАКОПЛЕНИЯ", "РЕГИСТРБУХГАЛТЕРИИ", "РЕГИСТРРАСЧЕТА", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        "БИЗНЕСПРОЦЕСС", "ЗАДАЧА", //$NON-NLS-1$ //$NON-NLS-2$
        "CATALOG", "DOCUMENT", "DOCUMENTJOURNAL", "ENUMERATION", "REPORT", "DATAPROCESSOR", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        "EXCHANGEPLAN", "CHARTOFACCOUNTS", "CHARTOFCHARACTERISTICTYPES", "CHARTOFCALCULATIONTYPES", "CONSTANT", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        "SEQUENCE", "INFORMATIONREGISTER", "ACCUMULATIONREGISTER", "ACCOUNTINGREGISTER", "CALCULATIONREGISTER", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        "BUSINESSPROCESS", "TASK"); //$NON-NLS-1$

    /**
     * Instantiates a new check.
     */
    public ApkDsFieldAliasCheck()
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
        builder.title(Messages.ApkDsFieldAliasCheck_title)
            .description(Messages.ApkDsFieldAliasCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.MINOR)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), CorePlugin.PLUGIN_ID))
            .extension(new DynamicListQueryChangeExtension());

        builder.topObject(FORM)
            .containment(ABSTRACT_FORM_ATTRIBUTE)
            .features(FORM_ATTRIBUTE__EXT_INFO);
    }

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {
        if (!(object instanceof FormAttribute attribute)
            || !(attribute.getExtInfo() instanceof DynamicListExtInfo extInfo))
        {
            return;
        }

        String queryText = extInfo.getQueryText();
        if (queryText == null || queryText.isBlank() || progressMonitor.isCanceled())
        {
            return;
        }

        String normalizedText = normalize(queryText);
        searchKeyword(normalizedText, queryText, KEYWORD_AS_RU, attribute, resultAceptor, progressMonitor);
        searchKeyword(normalizedText, queryText, KEYWORD_AS_EN, attribute, resultAceptor, progressMonitor);
    }

    /**
     * Нормализованная копия текста той же длины (как в АПК): комментарии «//»
     * и разделители «, ( ) { } "» заменены пробелами; табы и переводы строк
     * заменены пробелами — позиции не смещаются, нумерация строк сохраняется.
     */
    private static String normalize(String queryText)
    {
        char[] characters = queryText.toCharArray();
        int lineStart = 0;
        for (int i = 0; i <= characters.length; i++)
        {
            if (i == characters.length || characters[i] == '\n')
            {
                blankLineComment(characters, lineStart, i);
                lineStart = i + 1;
            }
        }
        for (int i = 0; i < characters.length; i++)
        {
            switch (characters[i])
            {
                case '\t':
                case '\n':
                case '\r':
                case ',':
                case '(':
                case ')':
                case '{':
                case '}':
                case '"':
                    characters[i] = SPACE;
                    break;
                default:
                    break;
            }
        }
        return new String(characters);
    }

    private static void blankLineComment(char[] characters, int lineStart, int lineEnd)
    {
        int commentIndex = indexOf(characters, LINE_COMMENT, lineStart, lineEnd);
        if (commentIndex >= 0)
        {
            for (int i = commentIndex; i < lineEnd; i++)
            {
                characters[i] = SPACE;
            }
        }
    }

    private static int indexOf(char[] characters, String searched, int from, int to)
    {
        int limit = Math.min(to, characters.length) - searched.length();
        for (int i = Math.max(from, 0); i <= limit; i++)
        {
            boolean found = true;
            for (int j = 0; found && j < searched.length(); j++)
            {
                found = characters[i + j] == searched.charAt(j);
            }
            if (found)
            {
                return i;
            }
        }
        return -1;
    }

    private void searchKeyword(String normalizedText, String queryText, String keyword, FormAttribute attribute,
        ResultAcceptor resultAceptor, IProgressMonitor progressMonitor)
    {
        int keywordIndex = normalizedText.indexOf(keyword);
        while (keywordIndex >= 0)
        {
            if (progressMonitor.isCanceled())
            {
                return;
            }
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
                    int line = 1 + newLinesBefore(queryText, aliasStart);
                    String message = MessageFormat.format(
                        Messages.ApkDsFieldAliasCheck_Alias_matches_metadata_class_name, alias, attribute.getName(),
                        line);
                    resultAceptor.addIssue(message, FORM_ATTRIBUTE__EXT_INFO);
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

    /**
     * Пересчитывает проверку реквизита формы при создании/изменении
     * расширения реквизита «ДинамическийСписок» (в том числе текста запроса).
     */
    private static final class DynamicListQueryChangeExtension
        implements IBasicCheckExtension
    {
        @Override
        public void configureContextCollector(ICheckDefinition definition)
        {
            OnModelObjectAssociationContextCollector associationCollector =
                (IBmObject bmObject, BmSubEvent bmEvent, CheckContextCollectingSession contextSession) -> {
                    addAttributeCheck(bmObject, contextSession);
                };
            OnModelFeatureChangeContextCollector changeCollector =
                (IBmObject bmObject, EStructuralFeature feature, BmSubEvent bmEvent,
                    CheckContextCollectingSession contextSession) -> {
                    addAttributeCheck(bmObject, contextSession);
                };
            definition.addGenericModelAssociationContextCollector(associationCollector, DYNAMIC_LIST_EXT_INFO, FORM);
            definition.addGenericModelFeatureChangeContextCollector(changeCollector, DYNAMIC_LIST_EXT_INFO, FORM);
        }

        private void addAttributeCheck(IBmObject bmObject, CheckContextCollectingSession contextSession)
        {
            if (bmObject instanceof DynamicListExtInfo extInfo)
            {
                EObject container = extInfo.eContainer();
                if (container instanceof FormAttribute attribute && attribute.eContainer() instanceof Form)
                {
                    contextSession.addModelCheck(attribute);
                }
            }
        }
    }
}
