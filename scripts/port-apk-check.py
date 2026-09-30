#!/usr/bin/env python3
"""Каркас переноса проверки АПК в форк v8-code-style.

Генерирует: java-класс проверки (с TODO-телом), константы Messages (en/ru),
фрагмент plugin.xml, карточки markdown (en/ru), itest-класс + ресурсы.

Из apk.db берёт наименование/группу/статью; алгоритм 1С НЕ хранится в apk.db —
агент тянет его точечным запросом (onec-apk-data execute_query) и вставляет
инвариант в TODO самостоятельно.

Использование:
  python3 scripts/port-apk-check.py --apk АПК_00261 --slug optional-params-after-required \
      --channel bsl --article 486 [--severity MINOR] [--itype CODE_STYLE]
  python3 scripts/port-apk-check.py --apk АПК_00261 --print-sql   # запрос алгоритма

После генерации заполнить TODO (тело check()), зарегистрировать фрагмент
plugin.xml (скрипт печатает его в stdout), прогнать:
  bash scripts/test-check.sh ApkYoLetterCheck   # пример точечного прогона
"""
import argparse, glob, json, sqlite3, sys, os

REG = os.environ.get('APK_DB', os.path.expanduser('~/edt-plugins/_notes/apk-registry'))
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

BUNDLE = {
    'bsl': 'com.e1c.v8codestyle.bsl',
    'md': 'com.e1c.v8codestyle.md',
    'form': 'com.e1c.v8codestyle.form',
    'right': 'com.e1c.v8codestyle.right',
    'ql': 'com.e1c.v8codestyle.ql',
}

COPYRIGHT = '''/*******************************************************************************
 * Copyright (C) 2026, malikov-pro and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     malikov-pro - port of the APK check %s
 *******************************************************************************/'''


def load_rule(apk):
    con = sqlite3.connect(REG + '/apk.db')
    row = con.execute('SELECT code, grp, name, article, used, manual FROM rules WHERE code=?', (apk,)).fetchone()
    if not row:
        sys.exit(f'правило {apk} не найдено в apk.db — обновите снапшот (build_db.py) или проверьте код')
    ported = con.execute('SELECT check_id, notes FROM ported WHERE rule=?', (apk,)).fetchone()
    if ported:
        print(f'ВНИМАНИЕ: правило уже в конвейере: {ported}', file=sys.stderr)
    return row, ported


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--apk', required=True, help='код правила, АПК_NNNNN')
    ap.add_argument('--slug', required=True, help='краткий slug проверки (dash-case, англ.)')
    ap.add_argument('--channel', required=True, choices=list(BUNDLE), help='канал: бандл-получатель')
    ap.add_argument('--article', type=int, required=True, help='номер статьи v8std (для карточки/трассировки)')
    ap.add_argument('--severity', default='MINOR', choices=['TRIVIAL', 'MINOR', 'MAJOR', 'CRITICAL'])
    ap.add_argument('--itype', default='CODE_STYLE',
                    choices=['CODE_STYLE', 'ERROR', 'SECURITY', 'SUSPICIOUS', 'PERFORMANCE', 'UI_STYLE', 'LOCALIZATION'])
    ap.add_argument('--print-sql', action='store_true', help='напечатать SQL запроса алгоритма и выйти')
    args = ap.parse_args()

    if args.print_sql:
        print(f'ВЫБРАТЬ Код, Наименование, Алгоритм, Описание ИЗ Справочник.Правила ГДЕ Код = "{args.apk}"')
        return

    row, ported = load_rule(args.apk)
    apk, grp, name, article, used, manual = row
    check_id = f'apk-{apk.split("_")[1].lower()}-{args.slug}'
    cls = ''.join(w.capitalize() for w in args.slug.split('-')) + 'Check'  # OptionalParamsAfterRequiredCheck
    bundle = BUNDLE[args.channel]
    base = f'{ROOT}/bundles/{bundle}/src/com/e1c/v8codestyle'
    pkg_sub = bundle.split('.')[-1]  # bsl | md | form | right | ql
    src_dir = f'{base}/{pkg_sub}/check'
    md_dir = f'{ROOT}/bundles/{bundle}/markdown'
    test_dir = f'{ROOT}/tests/{bundle}.itests/src/com/e1c/v8codestyle/{pkg_sub}/check/itests'
    res_dir = f'{ROOT}/tests/{bundle}.itests/resources'
    for d in (src_dir, md_dir + '/ru', test_dir, res_dir):
        os.makedirs(d, exist_ok=True)

    if os.path.exists(f'{src_dir}/{cls}.java'):
        sys.exit(f'{cls}.java уже существует — прервано')

    check_java = f'''{COPYRIGHT % apk}
package com.e1c.v8codestyle.{pkg_sub}.check;

import static com._1c.g5.v8.dt.bsl.model.BslPackage.Literals.MODULE;

import org.eclipse.core.runtime.IProgressMonitor;

import com.e1c.g5.v8.dt.check.components.BasicCheck;
import com.e1c.g5.v8.dt.check.settings.IssueSeverity;
import com.e1c.v8codestyle.{pkg_sub}.check.Messages;

/**
 * {name}.
 * <p>
 * Перенос проверки {apk} (статья {article} стандарта 1С).
 *
 * @author malikov-pro
 */
public class {cls}
    extends BasicCheck
{{

    /** Идентификатор проверки (префикс apk-NNNNN — код правила АПК). */
    public static final String CHECK_ID = "{check_id}";

    @Override
    public String getCheckId()
    {{
        return CHECK_ID;
    }}

    @Override
    protected void configureCheck(CheckConfigurer builder)
    {{
        builder.title(Messages.{cls}_title)
            .description(Messages.{cls}_description)
            .issueType(IssueType.{args.itype})
            .severity(IssueSeverity.{args.severity})
            .module()
            .checkedObjectType(MODULE);
    }}

    @Override
    protected void check(Object object, ResultAcceptor resultAceptor, ICheckParameters parameters,
        IProgressMonitor progressMonitor)
    {{
        // TODO(port): инвариант проверки из Алгоритма {apk} (точечный запрос — скилл v8cs-apk-db):
        //  1. получить модель (например, Module module = (Module)object;)
        //  2. обойти нужные узлы модели
        //  3. resultAceptor.addIssue(...) на каждое нарушение
    }}
}}
'''
    open(f'{src_dir}/{cls}.java', 'w', encoding='utf-8').write(check_java)

    # Messages: константы в существующие файлы (id-блок); Messages канального бандла
    # лежит в <pkg>/check/, фолбэк-глоб исключает comment/qfix/internal подпакеты
    msg_dir = None
    pref = f'{base}/{pkg_sub}/check/Messages.java'
    if os.path.exists(pref):
        msg_dir = os.path.dirname(pref)
    else:
        for cand in glob.glob(f'{base}/**/Messages.java', recursive=True):
            if not any(x in cand for x in ('/internal/', '/comment/', '/qfix/', '/strict/')):
                msg_dir = os.path.dirname(cand)
                break
    for fname, tkey, dkey in [('Messages.java', f'{cls}_title', f'{cls}_description'),
                              ('messages.properties', f'{cls}_title', f'{cls}_description'),
                              ('messages_ru.properties', f'{cls}_title', f'{cls}_description')]:
        if msg_dir is None:
            print(f'ВНИМАНИЕ: Messages.java бандла не найден — константы {cls} добавьте руками')
            break
        path = f'{msg_dir}/{fname}'
        src = open(path, encoding='utf-8').read()
        if f'{cls}_title' in src:
            print(f'ВНИМАНИЕ: {path} уже содержит {cls}_title — константы не добавлены')
            continue
        if fname.endswith('.java'):
            src = src.replace('\n    static\n', f'    public static String {tkey};\n'
                f'    public static String {dkey};\n\n    static\n')
        else:
            val = name if 'ru' in fname else f'{name[:70]}'
            src = src.rstrip('\n') + f'\n{tkey} = {val}\n{dkey} = TODO_fill_description\n'
        open(path, 'w', encoding='utf-8').write(src)

    # plugin.xml-фрагмент (печатаем, вставляет агент)
    print('=== plugin.xml (вставить рядом с проверками категории) ===')
    print(f'''      <check
            category="com.e1c.v8codestyle.{pkg_sub}"
            class="com.e1c.v8codestyle.internal.{pkg_sub}.ExecutableExtensionFactory:com.e1c.v8codestyle.{pkg_sub}.check.{cls}">
      </check>''')

    # карточки
    open(f'{md_dir}/{check_id}.md', 'w', encoding='utf-8').write(
        f"# {name}\n\nTODO: description (en). Ported from the APK check `{apk}`.\n\n## See\n\n"
        f"- [Standard {article}](https://its.1c.ru/db/v8std#content:{article}:hdoc)\n")
    open(f'{md_dir}/ru/{check_id}.md', 'w', encoding='utf-8').write(
        f"# {name}\n\nTODO: описание (ru). Перенос проверки АПК `{apk}`.\n\n## См. также\n\n"
        f"- [Стандарт {article}](https://its.1c.ru/db/v8std#content:{article}:hdoc)\n")

    # тесты
    test_java = f'''{COPYRIGHT % apk}
package com.e1c.v8codestyle.{pkg_sub}.check.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com._1c.g5.v8.dt.validation.marker.Marker;
import com.e1c.v8codestyle.{pkg_sub}.check.{cls};

/**
 * Tests for {{@link {cls}}} — port of the APK check {apk}.
 *
 * @author malikov-pro
 */
public class {cls}Test
    extends AbstractSingleModuleTestBase
{{

    private static final String RESOURCE_YO = FOLDER_RESOURCE + "{check_id}.bsl"; //$NON-NLS-1$
    private static final String RESOURCE_CLEAN = FOLDER_RESOURCE + "{check_id}-clean.bsl"; //$NON-NLS-1$

    /**
     * Instantiates a new test.
     */
    public {cls}Test()
    {{
        super({cls}.class);
    }}

    /**
     * Violations are reported.
     */
    @Test
    public void testViolationsReported() throws Exception
    {{
        updateModule(RESOURCE_YO);

        List<Marker> markers = getModuleMarkers();
        assertEquals(TODO_expected_count, markers.size());
    }}

    /**
     * Clean module produces no issues.
     */
    @Test
    public void testCleanModuleHasNoIssues() throws Exception
    {{
        updateModule(RESOURCE_CLEAN);

        List<Marker> markers = getModuleMarkers();
        assertTrue(markers.isEmpty());
    }}
}}
'''
    open(f'{test_dir}/{cls}Test.java', 'w', encoding='utf-8').write(test_java)
    open(f'{res_dir}/{check_id}.bsl', 'w', encoding='utf-8').write(
        '// TODO(port): минимальный модуль С НАРУШЕНИЕМ (инвариант из Алгоритма)\n')
    open(f'{res_dir}/{check_id}-clean.bsl', 'w', encoding='utf-8').write(
        '// TODO(port): тот же модуль БЕЗ нарушения\n')

    print(f'''=== сгенерировано ===
check : {src_dir}/{cls}.java  (TODO-тело!)
test  : {test_dir}/{cls}Test.java
res   : {res_dir}/{check_id}.bsl, {check_id}-clean.bsl (TODO)
cards : {md_dir}/{check_id}.md, md/ru/{check_id}.md
msgs  : константы добавлены в Messages.java + messages*.properties (bundle {bundle})
дальше: заполнить TODO (тело check, описания, ресурсы, expected count),
        вставить plugin.xml-фрагмент, точечный прогон:
        bash scripts/test-check.sh {cls}Test''')


if __name__ == '__main__':
    main()
