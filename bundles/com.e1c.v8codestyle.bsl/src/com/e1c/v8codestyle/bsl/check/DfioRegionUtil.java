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
 *     malikov-pro - port of the upstream issues #632, #633, #634 (std 644)
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Общие утилиты семьи проверок области «ДляВызоваИзДругихПодсистем»
 * (англ. «InterfaceImplementation» — статья 644 стандарта 1С, п. 2.2):
 * канонические имена области, «ПрограммныйИнтерфейс» и разбор
 * комментария-отметки подсистемы-потребителя вида
 * {@code // Подсистема1.Подсистема2} (допускается список через запятую
 * и одна завершающая точка).
 *
 * @author malikov-pro
 */
final class DfioRegionUtil
{
    /** Русское имя области для вызова из других подсистем. */
    static final String REGION_NAME_RU = "ДляВызоваИзДругихПодсистем"; //$NON-NLS-1$

    /** Английское имя области (статья 644 стандарта 1С, п. 2.2). */
    static final String REGION_NAME_EN = "InterfaceImplementation"; //$NON-NLS-1$

    private static final String PUBLIC_NAME_EN = "Public"; //$NON-NLS-1$

    private static final String PUBLIC_NAME_RU = "ПрограммныйИнтерфейс"; //$NON-NLS-1$

    /**
     * Идентификатор подсистемы: начинается с буквы или подчёркивания,
     * далее буквы, цифры и подчёркивания (кириллица и латиница).
     */
    private static final String IDENTIFIER = "[A-Za-zА-Яа-яЁё_][A-Za-zА-Яа-яЁё0-9_]*"; //$NON-NLS-1$

    /**
     * Строка-отметка потребителя: одна или несколько групп
     * «Имя[.Имя]*», разделённых запятыми; допускается одна завершающая
     * точка. Строки с пробелами внутри пути (описания, закрывающие
     * маркеры «// Конец …» и «// End …») отметками не считаются.
     */
    private static final Pattern CONSUMER_MARKING_LINE = Pattern
        .compile("\\s*" + IDENTIFIER + "(\\." + IDENTIFIER + ")*(\\s*,\\s*" + IDENTIFIER + "(\\." + IDENTIFIER + ")*)*" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            + "\\s*\\.?\\s*"); //$NON-NLS-1$

    private DfioRegionUtil()
    {
        // Утилитный класс, не инстанцируется.
    }

    /**
     * Имя области совпадает (без учёта регистра) с каноническим русским
     * или английским именем области «ДляВызоваИзДругихПодсистем».
     *
     * @param name the region name, may be {@code null}
     * @return {@code true} if the region is the region for calls from other subsystems
     */
    static boolean isDfioRegionName(String name)
    {
        return REGION_NAME_RU.equalsIgnoreCase(name) || REGION_NAME_EN.equalsIgnoreCase(name);
    }

    /**
     * Имя области совпадает (без учёта регистра) с каноническим русским
     * или английским именем области «ПрограммныйИнтерфейс».
     *
     * @param name the region name, may be {@code null}
     * @return {@code true} if the region is the public (API) region
     */
    static boolean isPublicRegionName(String name)
    {
        return PUBLIC_NAME_EN.equalsIgnoreCase(name) || PUBLIC_NAME_RU.equalsIgnoreCase(name);
    }

    /**
     * Является ли текст комментария отметкой подсистемы-потребителя:
     * вся строка после «//» состоит из одного или нескольких путей
     * подсистем (групп идентификаторов через точку), разделённых
     * запятыми, с опциональной завершающей точкой.
     *
     * @param commentText the full comment leaf text starting with "//", may be {@code null}
     * @return {@code true} if the comment is a consumer marking comment
     */
    static boolean isConsumerMarkingLine(String commentText)
    {
        return CONSUMER_MARKING_LINE.matcher(commentContent(commentText)).matches();
    }

    /**
     * Разбирает комментарий-отметку на пути подсистем-потребителей:
     * каждый путь — список сегментов имени (от верхнеуровневой
     * подсистемы к вложенной). Не-отметка даёт пустой список.
     *
     * @param commentText the full comment leaf text starting with "//", may be {@code null}
     * @return the list of consumer subsystem paths, never {@code null}
     */
    static List<List<String>> parseConsumerPaths(String commentText)
    {
        List<List<String>> result = new ArrayList<>();
        if (!isConsumerMarkingLine(commentText))
        {
            return result;
        }
        for (String group : commentContent(commentText).split(",")) //$NON-NLS-1$
        {
            String trimmed = group.strip();
            if (trimmed.endsWith(".")) //$NON-NLS-1$
            {
                trimmed = trimmed.substring(0, trimmed.length() - 1).strip();
            }
            List<String> segments = new ArrayList<>();
            for (String segment : trimmed.split("\\.")) //$NON-NLS-1$
            {
                if (!segment.isEmpty())
                {
                    segments.add(segment);
                }
            }
            if (!segments.isEmpty())
            {
                result.add(segments);
            }
        }
        return result;
    }

    private static String commentContent(String commentText)
    {
        if (commentText == null)
        {
            return ""; //$NON-NLS-1$
        }
        String text = commentText.strip();
        if (text.startsWith("//")) //$NON-NLS-1$
        {
            return text.substring(2);
        }
        return text;
    }
}
