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
 *     malikov-pro - port of the BSL Language Server CodeRecognizer/BSLFootprint
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Распознаватель «похоже на код 1С» для комментариев — упрощённый порт
 * CodeRecognizer/BSLFootprint из BSL Language Server: набор детекторов
 * с весами, итоговая вероятность объединяется как
 * {@code 1 - Π(1 - p_i)}, строка считается кодом при вероятности выше порога.
 *
 * @author malikov-pro
 */
public final class CommentCodeRecognizer
{

    private static final double CODE_EXACTLY = 0.95;
    private static final double CODE_MOST_LIKELY = 0.7;
    private static final double CODE_MAYBE = 0.3;

    private static final String[] CONTAINS_EXACT =
        {"КонецПроцедуры", "КонецФункции", "КонецЕсли;", "КонецЦикла;", "EndProcedure", "EndFunction", "EndIf;",
            "EndDo;", "Возврат;", ".НайтиСтроки(", "СтрНачинаетсяС(", "Return;", ".FindRows(", "StrStartsWith(",
            "СтрНайти(", ".Выбрать(", ".Выгрузить(", ".Выполнить(", "?(", ");", "StrFind(", ".Select(", ".Unload(",
            ".Execute(", "#Если", "#Иначе", "#КонецЕсли", "#Область", "КонецПопытки;", "#If", "#Else", "#ElsIf",
            "#EndIf", "#Region", "EndTry;"};

    private static final String[] KEYWORDS_EXACT = {"ИначеЕсли", "ElsIf"};

    private static final String[] KEYWORDS_MOST_LIKELY =
        {"ВЫБРАТЬ", "РАЗРЕШЕННЫЕ", "ПЕРВЫЕ", "ГДЕ", "СОЕДИНЕНИЕ", "НЕ", "ОБЪЕДИНИТЬ", "ВЫБОР", "КАК", "ТОГДА",
            "КОГДА", "ИНАЧЕ", "ПОМЕСТИТЬ", "ИЗ", "=", "+", "SELECT", "ТОР", "JOIN", "NOT", "AS", "THEN", "CASE",
            "ELSE", "FROM", "INTO"};

    private static final String[] KEYWORDS_MAYBE =
        {"И", "ИЛИ", "AND", "OR", "Если", "Тогда", "Процедура", "Функция", "Пока", "Для", "Каждого", "Цикл",
            "Возврат", "Новый", "*", "If", "Then", "Procedure", "Function", "Do", "For", "While", "Return", "New"};

    private static final Pattern HEAD_PATTERN = Pattern
        .compile("^[/\\s]*(?:Процедура|Функция|Procedure|Function)\\s+[а-яА-Яё\\w]+\\s*?\\(|^[/\\s]*(?:&На[а-яА-Яё]+|&At[\\w]+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);

    private final double threshold;

    /**
     * Instantiates a new recognizer.
     *
     * @param threshold порог вероятности «это код», не может быть {@code null}.
     */
    public CommentCodeRecognizer(double threshold)
    {
        this.threshold = threshold;
    }

    /**
     * Похожа ли строка на код 1С (вероятность выше порога).
     *
     * @param line строка, не может быть {@code null}.
     * @return {@code true}, если строка похожа на код.
     */
    public boolean meetsCondition(String line)
    {
        double probability = 0;
        probability = combine(probability, containsWords(line, CONTAINS_EXACT, CODE_EXACTLY));
        probability = combine(probability, keywordTokens(line, KEYWORDS_EXACT, CODE_EXACTLY));
        probability = combine(probability, camelCase(line, CODE_MOST_LIKELY));
        probability = combine(probability, keywordTokens(line, KEYWORDS_MOST_LIKELY, CODE_MOST_LIKELY));
        probability = combine(probability, endsWith(line, ';', CODE_MAYBE));
        probability = combine(probability, keywordTokens(line, KEYWORDS_MAYBE, CODE_MAYBE));
        probability = combine(probability, patternMatches(line, HEAD_PATTERN, CODE_EXACTLY));
        return probability - threshold > 0;
    }

    private static double combine(double probability, double detect)
    {
        return 1 - (1 - probability) * (1 - detect);
    }

    private static double detect(int matches, double probability)
    {
        if (matches == 0)
        {
            return 0;
        }
        return 1 - Math.pow(1 - probability, matches);
    }

    private static double containsWords(String line, String[] words, double probability)
    {
        String withoutWhitespace = line.replaceAll("\\s+", ""); //$NON-NLS-1$ //$NON-NLS-2$
        int matches = 0;
        for (String word : words)
        {
            int index = 0;
            while ((index = withoutWhitespace.indexOf(word, index)) >= 0)
            {
                matches++;
                index += word.length();
            }
        }
        return detect(matches, probability);
    }

    private static double keywordTokens(String line, String[] keywords, double probability)
    {
        List<String> tokens = new ArrayList<>(List.of(line.split("[ \t\n]")));
        int matches = 0;
        for (String token : tokens)
        {
            for (String keyword : keywords)
            {
                if (keyword.equals(token))
                {
                    matches++;
                    break;
                }
            }
        }
        return detect(matches, probability);
    }

    private static double camelCase(String line, double probability)
    {
        char previous = ' ';
        for (int i = 0; i < line.length(); i++)
        {
            char current = line.charAt(i);
            if (Character.getType(previous) == Character.LOWERCASE_LETTER
                && Character.getType(current) == Character.UPPERCASE_LETTER)
            {
                return detect(1, probability);
            }
            previous = current;
        }
        return 0;
    }

    private static double endsWith(String line, char end, double probability)
    {
        for (int i = line.length() - 1; i >= 0; i--)
        {
            char current = line.charAt(i);
            if (current == end)
            {
                return detect(1, probability);
            }
            if (!Character.isWhitespace(current))
            {
                return 0;
            }
        }
        return 0;
    }

    private static double patternMatches(String line, Pattern pattern, double probability)
    {
        Matcher matcher = pattern.matcher(line);
        int matches = 0;
        while (matcher.find())
        {
            matches++;
        }
        return detect(matches, probability);
    }
}
