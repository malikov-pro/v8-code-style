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
 *     malikov-pro - checks for references in descriptions of deprecated methods
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;

/**
 * Common helpers of the checks for references «см. Модуль.ИмяМетода»
 * (англ. «see Module.Method») in descriptions of deprecated procedures
 * (functions) — issues #769/#768, section 5.7 of the standard 453.
 * <p>
 * Метод считается устаревшим, если он помечен комментарием
 * «Устарела.»/«Deprecated.» (первая строка описания), размещён в области
 * «УстаревшиеПроцедурыИФункции»/«Deprecated» либо имеет признак устаревания
 * в контексте модуля ({@link com._1c.g5.v8.dt.bsl.model.BslContextDefMethod#isDeprecated()}).
 * Отсылки на замену извлекаются из комментария-описания метода, размещённого
 * над объявлением (между комментарием и объявлением допускаются пустые
 * строки) — техника чтения комментария OverridableModuleUtil (АПК_00460).
 *
 * @author malikov-pro
 */
final class DeprecatedMethodRefUtil
{

    /**
     * Отсылка «см. Модуль.ИмяМетода», извлечённая из описания метода.
     */
    static final class SeeReference
    {

        final String moduleName;

        final String methodName;

        SeeReference(String moduleName, String methodName)
        {
            this.moduleName = moduleName;
            this.methodName = methodName;
        }

        String qualifiedName()
        {
            return moduleName + "." + methodName; //$NON-NLS-1$
        }
    }

    private static final String NAME_PART = "[\\p{L}_][\\p{L}\\p{Nd}_]*"; //$NON-NLS-1$

    /**
     * Отсылка «см. &lt;Модуль&gt;.&lt;ИмяМетода&gt;» (англ. «see …»),
     * в любом регистре; между «см.» и именем допускается одно слово
     * («см. также …», «см. в …», «see also …»). Имена — буквы Unicode,
     * цифры и «_», разделённые точкой.
     */
    private static final Pattern SEE_REFERENCE_PATTERN = Pattern.compile(
        "(?:см|see)\\.\\s*(?:[^\\s.]+\\s+)?(" + NAME_PART + ")\\.(" + NAME_PART + ")", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /**
     * Признак устаревания в строке описания: «Устарела.»/«Deprecated.»
     * в начале строки, в любом регистре.
     */
    private static final Pattern DEPRECATED_MARK_PATTERN = Pattern.compile(
        "(?:устарела|deprecated)\\b", //$NON-NLS-1$
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);

    private DeprecatedMethodRefUtil()
    {
        // Utility class, no instances
    }

    /**
     * Проверяет, что метод устарел: помечен комментарием
     * «Устарела.»/«Deprecated.», размещён в области
     * «УстаревшиеПроцедурыИФункции»/«Deprecated» либо имеет признак
     * устаревания в контексте модуля.
     *
     * @param module the module containing the method, may be {@code null}
     * @param method the method to test, may be {@code null}
     * @return true, if the method is deprecated
     */
    static boolean isDeprecated(Module module, Method method)
    {
        if (OverridableModuleUtil.isDeprecatedMethod(module, method))
        {
            return true;
        }
        return isDeprecatedComment(OverridableModuleUtil.getPrecedingComment(module, method));
    }

    /**
     * Проверяет, что в комментарии-описании есть признак устаревания:
     * строка, начинающаяся с «Устарела.»/«Deprecated.» (в любом регистре).
     *
     * @param comment the comment text, may be {@code null}
     * @return true, if the comment marks the method as deprecated
     */
    static boolean isDeprecatedComment(String comment)
    {
        if (comment == null)
        {
            return false;
        }
        for (String line : comment.split("\n")) //$NON-NLS-1$
        {
            String text = line.replace("/", " ").strip(); //$NON-NLS-1$ //$NON-NLS-2$
            if (text.isEmpty())
            {
                continue;
            }
            if (DEPRECATED_MARK_PATTERN.matcher(text).lookingAt())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Извлекает из комментария-описания все отсылки
     * «см. &lt;Модуль&gt;.&lt;ИмяМетода&gt;» (англ. «see …»).
     *
     * @param comment the comment text, may be {@code null}
     * @return the list of references, never {@code null}
     */
    static List<SeeReference> findSeeReferences(String comment)
    {
        if (comment == null || comment.isBlank())
        {
            return List.of();
        }
        List<SeeReference> references = new ArrayList<>();
        Matcher matcher = SEE_REFERENCE_PATTERN.matcher(comment);
        while (matcher.find())
        {
            references.add(new SeeReference(matcher.group(1), matcher.group(2)));
        }
        return references;
    }
}
