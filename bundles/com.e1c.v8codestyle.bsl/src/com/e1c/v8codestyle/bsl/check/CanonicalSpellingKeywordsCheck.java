/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.e1c.v8codestyle.bsl.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Module;
import com.e1c.g5.v8.dt.check.BslDirectLocationIssue;
import com.e1c.g5.v8.dt.check.CheckComplexity;
import com.e1c.g5.v8.dt.check.DirectLocation;
import com.e1c.g5.v8.dt.check.ICheckParameters;
import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.g5.v8.dt.check.settings.IssueType;
import com.e1c.v8codestyle.check.CommonSenseCheckExtension;
import com.e1c.v8codestyle.internal.bsl.BslPlugin;

/**
 * Reports keywords whose spelling differs from the canonical one. The parser is
 * case-insensitive, so mixed-case keywords such as {@code ЕслИ} parse but are
 * hard to read. The allowed spellings are generated from the pinned LS
 * canonical keyword table; only words whose lowercase form is a known keyword
 * are checked, so identifiers and strings are untouched.
 */
public final class CanonicalSpellingKeywordsCheck
    extends BasicCheck
{
    public static final String CHECK_ID = "canonical-spelling-keywords"; //$NON-NLS-1$

    private static final Map<String, Set<String>> CANONICAL = canonical();

    @Override
    public String getCheckId()
    {
        return CHECK_ID;
    }

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {
        builder.title(Messages.CanonicalSpellingKeywordsCheck_title)
            .description(Messages.CanonicalSpellingKeywordsCheck_description)
            .complexity(CheckComplexity.NORMAL)
            .severity(IssueSeverity.TRIVIAL)
            .issueType(IssueType.CODE_STYLE)
            .extension(new CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID))
            .module().checkedObjectType(MODULE);
    }

    @Override
    protected void check(Object object, ResultAcceptor acceptor, ICheckParameters parameters, IProgressMonitor monitor)
    {
        Module module = (Module)object;
        INode node = NodeModelUtils.findActualNodeFor(module);
        if (node == null)
        {
            return;
        }
        for (ILeafNode leaf : node.getLeafNodes())
        {
            if (monitor.isCanceled())
            {
                return;
            }
            if (leaf.isHidden())
            {
                continue;
            }
            String text = leaf.getText();
            if (text == null || text.isEmpty())
            {
                continue;
            }
            int shift = text.startsWith("#") || text.startsWith("&") ? 1 : 0; //$NON-NLS-1$ //$NON-NLS-2$
            String word = text.substring(shift);
            Set<String> allowed = CANONICAL.get(word.toLowerCase(Locale.ROOT));
            if (allowed == null || allowed.contains(word))
            {
                continue;
            }
            acceptor.addIssue(new BslDirectLocationIssue(Messages.CanonicalSpellingKeywordsCheck_message,
                new DirectLocation(leaf.getOffset() + shift, word.length(), leaf.getStartLine(), module)));
        }
    }

    private static Map<String, Set<String>> canonical()
    {
        Map<String, Set<String>> map = new HashMap<>();
        entry(map, "addhandler", "AddHandler");
        entry(map, "and", "AND", "And");
        entry(map, "atclient", "AtClient");
        entry(map, "atclientatserver", "AtClientAtServer");
        entry(map, "atclientatservernocontext", "AtClientAtServerNoContext");
        entry(map, "atserver", "AtServer");
        entry(map, "atservernocontext", "AtServerNoContext");
        entry(map, "break", "Break");
        entry(map, "client", "Client");
        entry(map, "continue", "Continue");
        entry(map, "do", "Do");
        entry(map, "each", "Each", "each");
        entry(map, "else", "Else");
        entry(map, "elsif", "ElsIf");
        entry(map, "enddo", "EndDo");
        entry(map, "endfunction", "EndFunction");
        entry(map, "endif", "EndIf");
        entry(map, "endprocedure", "EndProcedure");
        entry(map, "endregion", "EndRegion");
        entry(map, "endtry", "EndTry");
        entry(map, "except", "Except");
        entry(map, "execute", "Execute");
        entry(map, "export", "Export");
        entry(map, "externalconnection", "ExternalConnection");
        entry(map, "false", "False");
        entry(map, "for", "For");
        entry(map, "function", "Function");
        entry(map, "goto", "Goto");
        entry(map, "if", "If");
        entry(map, "in", "In");
        entry(map, "mobileappclient", "MobileAppClient");
        entry(map, "mobileappserver", "MobileAppServer");
        entry(map, "mobileclient", "MobileClient");
        entry(map, "new", "New");
        entry(map, "not", "NOT", "Not");
        entry(map, "or", "OR", "Or");
        entry(map, "procedure", "Procedure");
        entry(map, "raise", "Raise");
        entry(map, "region", "Region");
        entry(map, "removehandler", "RemoveHandler");
        entry(map, "return", "Return");
        entry(map, "server", "Server");
        entry(map, "then", "Then");
        entry(map, "thickclientmanagedapplication", "ThickClientManagedApplication");
        entry(map, "thickclientordinaryapplication", "ThickClientOrdinaryApplication");
        entry(map, "thinclient", "ThinClient");
        entry(map, "to", "To");
        entry(map, "true", "True");
        entry(map, "try", "Try");
        entry(map, "undefined", "Undefined");
        entry(map, "val", "Val");
        entry(map, "var", "Var");
        entry(map, "webclient", "WebClient");
        entry(map, "while", "While");
        entry(map, "вебклиент", "ВебКлиент");
        entry(map, "внешнеесоединение", "ВнешнееСоединение");
        entry(map, "возврат", "Возврат");
        entry(map, "вызватьисключение", "ВызватьИсключение");
        entry(map, "выполнить", "Выполнить");
        entry(map, "для", "Для");
        entry(map, "добавитьобработчик", "ДобавитьОбработчик");
        entry(map, "если", "Если");
        entry(map, "знач", "Знач");
        entry(map, "и", "И");
        entry(map, "из", "Из");
        entry(map, "или", "ИЛИ", "Или");
        entry(map, "иначе", "Иначе");
        entry(map, "иначеесли", "ИначеЕсли");
        entry(map, "исключение", "Исключение");
        entry(map, "истина", "Истина");
        entry(map, "каждого", "Каждого", "каждого");
        entry(map, "клиент", "Клиент");
        entry(map, "конецесли", "КонецЕсли");
        entry(map, "конецобласти", "КонецОбласти");
        entry(map, "конецпопытки", "КонецПопытки");
        entry(map, "конецпроцедуры", "КонецПроцедуры");
        entry(map, "конецфункции", "КонецФункции");
        entry(map, "конеццикла", "КонецЦикла");
        entry(map, "ложь", "Ложь");
        entry(map, "мобильноеприложениеклиент", "МобильноеПриложениеКлиент");
        entry(map, "мобильноеприложениесервер", "МобильноеПриложениеСервер");
        entry(map, "мобильныйклиент", "МобильныйКлиент");
        entry(map, "наклиенте", "НаКлиенте");
        entry(map, "наклиентенасервере", "НаКлиентеНаСервере");
        entry(map, "наклиентенасерверебезконтекста", "НаКлиентеНаСервереБезКонтекста");
        entry(map, "насервере", "НаСервере");
        entry(map, "насерверебезконтекста", "НаСервереБезКонтекста");
        entry(map, "не", "НЕ", "Не");
        entry(map, "неопределено", "Неопределено");
        entry(map, "новый", "Новый");
        entry(map, "область", "Область");
        entry(map, "перейти", "Перейти");
        entry(map, "перем", "Перем");
        entry(map, "по", "По");
        entry(map, "пока", "Пока");
        entry(map, "попытка", "Попытка");
        entry(map, "прервать", "Прервать");
        entry(map, "продолжить", "Продолжить");
        entry(map, "процедура", "Процедура");
        entry(map, "сервер", "Сервер");
        entry(map, "тогда", "Тогда");
        entry(map, "толстыйклиентобычноеприложение", "ТолстыйКлиентОбычноеПриложение");
        entry(map, "толстыйклиентуправляемоеприложение", "ТолстыйКлиентУправляемоеПриложение");
        entry(map, "тонкийклиент", "ТонкийКлиент");
        entry(map, "удалитьобработчик", "УдалитьОбработчик");
        entry(map, "функция", "Функция");
        entry(map, "цикл", "Цикл");
        entry(map, "экспорт", "Экспорт");
        return map;
    }

    private static void entry(Map<String, Set<String>> map, String key, String... words)
    {
        map.computeIfAbsent(key, ignored -> new HashSet<>()).addAll(Set.of(words));
    }
}
