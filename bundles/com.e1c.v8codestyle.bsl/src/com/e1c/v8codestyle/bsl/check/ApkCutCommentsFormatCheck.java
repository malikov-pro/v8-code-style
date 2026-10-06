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
 *     malikov-pro - port of the APK check АПК_01219
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.ModuleType;
import com._1c.g5.v8.dt.metadata.mdclass.CommonModule;
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
 * Проверка: оформление вырезаемых служебных комментариев — «// Локализация»,
 * «// Конец Локализация», «// _Демо начало примера», «// _Демо конец примера».
 * Ими обрамляют фрагменты, вырезаемые при сборке международной поставки
 * (статья 769 стандарта 1С).
 * <p>
 * Перенос проверки АПК_01219. Как и в алгоритме АПК — текстовый поиск
 * по листьям-комментариям модуля:
 * <ul>
 * <li>комментарий, «похожий» на служебный (без учёта регистра и пробелов),
 * но записанный не канонически — замечание о неверном формате;</li>
 * <li>в модуле без постфикса «Локализация» канонические комментарии
 * «Локализация»/«Конец Локализация» запрещены (национальную специфику
 * выносить в такие модули нельзя);</li>
 * <li>в общем модуле с постфиксом «Локализация» должна быть связка
 * «// Локализация» + «// Конец Локализация»;</li>
 * <li>несбалансированные открывающие/закрывающие комментарии — замечание.</li>
 * </ul>
 * Упрощения относительно алгоритма АПК: проверка расположения процедуры
 * (функции) с комментариями в области «ПрограммныйИнтерфейс» (ошибка 1449)
 * не переносится; комментарий с неверным форматом не учитывается в балансах
 * и в проверке связки; баланс пар «Локализация» проверяется только в модулях
 * с постфиксом (в остальных модулях каждый такой комментарий уже помечен
 * как запрещённый).
 *
 * @author malikov-pro
 */
public class ApkCutCommentsFormatCheck
    extends BasicCheck
{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "apk-01219-cut-comments-format"; //$NON-NLS-1$

    private static final String POSTFIX_LOCALIZATION = "Локализация"; //$NON-NLS-1$

    /**
     * Вид вырезаемого служебного комментария: каноническая запись и
     * «компактная» форма (нижний регистр, без пробелов) для нечёткого
     * распознавания попытки написать служебный комментарий.
     */
    private enum ServiceComment
    {
        /** «// Локализация». */
        OPEN_LOCALIZATION("Локализация", false), //$NON-NLS-1$
        /** «// Конец Локализация». */
        CLOSE_LOCALIZATION("Конец Локализация", false), //$NON-NLS-1$
        /** «// _Демо начало примера». */
        OPEN_DEMO("_Демо начало примера", true), //$NON-NLS-1$
        /** «// _Демо конец примера». */
        CLOSE_DEMO("_Демо конец примера", true); //$NON-NLS-1$

        private final String canonicalForm;
        private final String compactForm;
        private final boolean demoFamily;

        ServiceComment(String canonicalForm, boolean demoFamily)
        {
            this.canonicalForm = canonicalForm;
            this.compactForm = compact(canonicalForm);
            this.demoFamily = demoFamily;
        }

        /**
         * Recognizes an attempted cut service comment by its text ignoring
         * case and whitespace, or returns {@code null} if the text is not
         * similar to any of the canonical forms.
         */
        static ServiceComment match(String text)
        {
            String compact = compact(text);
            for (ServiceComment service : values())
            {
                if (service.compactForm.equals(compact))
                {
                    return service;
                }
            }
            return null;
        }

        boolean matchesCanonically(String text)
        {
            return canonicalForm.equals(text);
        }
    }

    /**
     * Instantiates a new check.
     */
    public ApkCutCommentsFormatCheck()
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
        builder.title(Messages.ApkCutCommentsFormatCheck_title)
            .description(Messages.ApkCutCommentsFormatCheck_description)
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
        boolean localizationModule = isLocalizationModule(module);

        List<DirectLocation> locOpeners = new ArrayList<>();
        List<DirectLocation> locClosers = new ArrayList<>();
        List<DirectLocation> demoOpeners = new ArrayList<>();
        List<DirectLocation> demoClosers = new ArrayList<>();

        for (ILeafNode leaf : node.getLeafNodes())
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
            String body = text.substring(2).strip();
            ServiceComment service = ServiceComment.match(body);
            if (service == null)
            {
                continue;
            }
            DirectLocation location =
                new DirectLocation(leaf.getOffset(), leaf.getLength(), leaf.getStartLine(), module);

            if (!service.matchesCanonically(body))
            {
                // похоже на служебный, но неверный формат/регистр/разделители
                Issue issue =
                    new BslDirectLocationIssue(Messages.ApkCutCommentsFormatCheck_Wrong_comment_format, location);
                resultAceptor.addIssue(issue);
                continue;
            }
            if (!service.demoFamily && !localizationModule)
            {
                // канонический «Локализация»-комментарий в модуле без постфикса запрещён
                Issue issue =
                    new BslDirectLocationIssue(Messages.ApkCutCommentsFormatCheck_Comment_not_allowed, location);
                resultAceptor.addIssue(issue);
                continue;
            }
            switch (service)
            {
            case OPEN_LOCALIZATION:
                locOpeners.add(location);
                break;
            case CLOSE_LOCALIZATION:
                locClosers.add(location);
                break;
            case OPEN_DEMO:
                demoOpeners.add(location);
                break;
            case CLOSE_DEMO:
                demoClosers.add(location);
                break;
            default:
                break;
            }
        }

        if (localizationModule && (locOpeners.isEmpty() || locClosers.isEmpty()))
        {
            resultAceptor.addIssue(Messages.ApkCutCommentsFormatCheck_Localization_module_missing_comment, module);
        }
        if (localizationModule)
        {
            reportUnbalanced(locOpeners, locClosers, resultAceptor);
        }
        reportUnbalanced(demoOpeners, demoClosers, resultAceptor);
    }

    private static boolean isLocalizationModule(Module module)
    {
        if (module.getModuleType() != ModuleType.COMMON_MODULE)
        {
            return false;
        }
        if (!(module.getOwner() instanceof CommonModule commonModule))
        {
            return false;
        }
        String name = commonModule.getName();
        return name != null && name.endsWith(POSTFIX_LOCALIZATION);
    }

    private static void reportUnbalanced(List<DirectLocation> openers, List<DirectLocation> closers,
        ResultAcceptor resultAceptor)
    {
        int paired = Math.min(openers.size(), closers.size());
        if (openers.size() > closers.size())
        {
            for (int i = paired; i < openers.size(); i++)
            {
                Issue issue = new BslDirectLocationIssue(Messages.ApkCutCommentsFormatCheck_Missing_pair,
                    openers.get(i));
                resultAceptor.addIssue(issue);
            }
        }
        else if (closers.size() > openers.size())
        {
            for (int i = paired; i < closers.size(); i++)
            {
                Issue issue = new BslDirectLocationIssue(Messages.ApkCutCommentsFormatCheck_Missing_pair,
                    closers.get(i));
                resultAceptor.addIssue(issue);
            }
        }
    }

    private static String compact(String text)
    {
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++)
        {
            char ch = text.charAt(i);
            if (!Character.isWhitespace(ch))
            {
                builder.append(Character.toLowerCase(ch));
            }
        }
        return builder.toString();
    }
}
