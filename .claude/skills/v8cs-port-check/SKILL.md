---
name: v8cs-port-check
description: Конвейер переноса проверки АПК в форк v8-code-style — от выбора правила в apk.db до релиза. Использовать при переносе проверок АПК/BSL, добавлении проверок по готовому алгоритму 1С, учёте прогресса конвейера.
---

# Конвейер: перенос проверки АПК → проверка форка

Статус конвейера — в `apk.db` (таблица `ported`); работа с реестром — скилл
`v8cs-apk-db`. Первая проверка идёт по этому регламенту полностью; после
прогона регламент дополняется находками (мета-правило AGENTS.md).

## 0. Взять правило

```sql
SELECT article, rule, name FROM backlog
 WHERE priority IN ('P1','P2') AND rule NOT IN (SELECT rule FROM ported)
 ORDER BY priority, article LIMIT 10;
```
Взять одно, проверить в живой базе (скилл `v8cs-apk-db`), что правило живо
и `Алгоритм` не пуст.

## 1. Разбор источника

- Тяну `Алгоритм`, `Описание(HTML)`, ТЧ `ОбслуживаемыеТипы`/`ОбнаруживаемыеОшибки`.
- Определяю **канал** (bsl/md/form/rights — по алгоритму, не по названию).
  Если алгоритм требует исполнения кода или данных платформы → **стоп**:
  пометить в `ported` с notes=`runtime — не переносится статически` и взять
  следующее правило.
- Выписываю из алгоритма **инвариант проверки**: что именно ищется
  (условия, исключения, пороговые значения) — это спецификация тестов.

## 2. Реализация (BasicCheck)

- Бандл по каналу: bsl → `com.e1c.v8codestyle.bsl`, метаданные → `.md`,
  формы → `.form`, права → `.right`, запросы → `.ql`.
- Класс: `extends BasicCheck`, `getCheckId()` → id вида
  **`apk-<NNNNN>-<краткий-slug>`** (например `apk-00260-no-yo-letter`) —
  префикс кода АПК даёт трассировку в базу/задачи апстрима.
- `configureCheck`: title/description — из `Описания` АПК (сократив);
  severity — из `Серьёзности`/`УровеньИсполнения` (Обязательно→MAJOR/CRITICAL
  по смыслу, по умолчанию MINOR/MAJOR); extension — `CommonSenseCheckExtension`
  (НЕ StandardCheckExtension: статья может конфликтовать с существующей
  привязкой v8-cs — свериться с cross-map.json).
- Сообщения проверки — по текстам `ОбнаруживаемыхОшибок` правила.
- Карточка: `bundles/<bundle>/markdown/ru/<id>.md` (+en), ссылка на статью
  v8std и код АПК.

## 3. Тест (обязательно)

- `tests/<bundle>.itests`: `resources/<id>.bsl` — минимальный код с нарушением
  И чистой вариант; тест наследует `AbstractSingleModuleTestBase`, проверяет
  количество и строки замечаний (паттерн существующих тестов).
- Локально: `bash compile.sh` (полный itests своего бандла) — зелёный.

## 3.5. Пример в расширение АПК.ОшибкиДляПроверки

Минимальный модуль-пример с нарушением (инвариант из шага 1) — через
`create_metadata` + `write_module_source` (см. раздел выше). В шапке —
ожидаемые позиции. АПК-прогон по расширению подтверждает, что родная
проверка АПК детектирует те же места, что и наш порт.

## 4. Фиксация конвейера

- `INSERT INTO ported(rule, check_id, article, ported_at, commit_sha, notes)`;
  Если перенос отменён (X по шагу 1) — тоже в `ported` с notes-причиной:
  правило больше не кандидат.

## 5. Сдача

- `bash compile.sh` (полный itests) → push master → smoke на стенде
  (скилл `v8cs-deploy`) → при накоплении пачки: тег `X.Y.(Z+1)` → Release+сайт
  (скилл `v8cs-release`).

## Параллелизация

После первого прогона правила раздаются пачками (по группам тем);
координация — таблица `ported` в apk.db (взял → внёс с notes=`в работе,
<дата>` ДО старта реализации). Правило в работе ≠ перенесено: поле notes
различает. Конфликты git между исполнителями маловероятны (разные файлы),
но сборку гоняет тот, кто мержит последним.

## Скрипты конвейера (не формировать руками)

- `python3 scripts/port-apk-check.py --apk АПК_NNNNN --slug <slug> --channel bsl --article NNN`
  — каркас: java-класс (TODO-тело), константы Messages (en/ru, в правильный
  check-подпакет), карточки ru/en, itest-класс + оба ресурса; печатает
  plugin.xml-фрагмент. `--print-sql` — готовый запрос Алгоритма.
- `bash scripts/test-check.sh <TestClassName>` — точечный прогон теста
  (полный список модулей реактора уже внутри; без полного suite).
- Обновление снапшота — `python3 build_db.py` (скилл `v8cs-apk-db`).

## Quick fix (если переносим с фиксом)

- Паттерн: `@QuickFix(checkId=..., supplierId=BslPlugin.PLUGIN_ID)` класс
  в `<pkg>/qfix/`, extends `SingleVariantXtextBslModuleFix`;
  `configureFix` (description/details, interactive) + `fixIssue(state, model)`
  → `TextEdit` (MultiTextEdit + ReplaceEdit/DeleteEdit), `null` если нечего
  чинить. Образец — `ConsecutiveEmptyLinesFix`.
- Messages фикса — в qfix-пакете (свой Messages/properties).
- Регистрация — EP `com.e1c.g5.v8.dt.check.fixes` (рядом с существующими fix).
- itest-инфры для qfix нет — smoke руками на стенде (деплой, «быстрое
  исправление» у замечания), честно отмечать в `ported.notes`.
- Для этой задачи: простой заменимо-механический фикс (как замена ё→е)
  делаем сразу; где исправление «только пользователем» — фикс не делаем,
  в карточке пишем рекомендацию.

## Примеры в расширении АПК.ОшибкиДляПроверки (MCP edt-apk)

Конвейер использует EDT-расширение `АПК.ОшибкиДляПроверки`
(`~/edt/apk/АПК.ОшибкиДляПроверки`, сервер `edt-apk`, EDT 2026.1): для каждой
переносимой проверки добавляется модуль-пример с нарушением.

- Создание: `create_metadata` (projectName=`АПК.ОшибкиДляПроверки`,
  fqn=`CommonModule.ош_<Slug>`) → текст: `write_module_source`
  (objectName=`CommonModule.ош_<Имя>`, moduleType=Module, mode=replace).
- **Имена объектов нормализуются: ё→е** («ош_ПримерЁ»→«ош_ПримерЕ») — в
  идентификаторах ё и так запрещена; нарушение кладём в ТЕКСТ (код/строки).
- В шапке примера — ожидаемое число и позиции вхождений (спецификация);
  `write_module_source` защищён от lost-update (`expectedSource`), нормализует
  символы (длинное тире и пр.), прогоняет синтакс-контроль.
- Точный текст тянется `read_module_source` по `modulePath`
  (`CommonModules/<Имя>/Module.bsl`).
- Проверка детекции: прогон АПК по расширению (вариант проверки в базе —
  справочник `ВариантыПроверки`/`Конфигурации`), ожидание = число/строки из
  шапки примера.

## Чего не делать

- Не переносить «вслепую» без чтения Алгоритма (название правила врёт).
- Не заменять id конвенции `apk-NNNNN-slug` на свободный текст.
- Не забыть itest: проверка без теста не считается перенесённой.
- Не трогать `StandardCheckExtension` без сверки с cross-map.json.

## Находки первого прогона (АПК_00260 → apk-00260-no-yo-letter)

- **Текст модуля**: `NodeModelUtils.findActualNodeFor(module)` → `getLeafNodes()`
  (листья включают комментарии и строковые литералы — текстовые проверки
  обходят именно листья); позиция вхождения → `BslDirectLocationIssue`
  с `DirectLocation(offset, 1, line, module)`. Образец —
  `ConsecutiveEmptyLinesCheck`. line 1-based: `leaf.getStartLine() + число
  '\n' до вхождения`.
- **`Marker` не имеет номера строки** — itest проверяет число замечаний
  (по вхождениям) и не сравнивает текст сообщения (локаль рантайма
  неопределённа).
- Конструктор проверки — **public** (ExecutableExtensionFactory в другом
  пакете; private → InstantiationException).
- При удалении «неиспользуемых» импортов не удалить нужный (EcoreUtil) —
  ошибка компиляции ловится `--skip-tests` прогоном.
- `mvn verify -pl <модули>`: реактор НЕ подтягивает таргет-модуль по -am —
  перечислять цепочку явно (targets/edt-2026.1, все bundles, docs В КОРНЕ
  репо — не bundles/, feature, repository, itests-фрагмент).
- Точечный тест: `-Dtest=ApkYoLetterCheckTest -DfailIfNoTests=false`.
- INSERT/UPDATE в apk.db — только с абсолютным путём (cwd бывает другим).

## Quick fix: грабли реализации (АПК_00260, итерация 2)

- **Два разных бандла qfix**: `com._1c.g5.v8.dt.bsl.check.qfix` (платформенный,
  НЕ в Import-Package форка — импорт оттуда = «cannot be resolved» каскадом)
  и `com.e1c.g5.v8.dt.bsl.check.qfix` (нужный: `SingleVariantXtextBslModuleFix`,
  `IXtextBslModuleFixModel`, `FixConfigurer`). Перепутать легко — javap-вывод
  был из _1c-jar.
- Позиция замечания в фиксе — `model.getIssue().getOffset()/getLength()`
  (xtext Issue); текст — через `model.getDocument().get(offset, length)`
  (`XtextResource.get` не существует).
- Точечный фикс = замена по `getIssue().getOffset()`; на позиции без ожидаемого
  символа (устаревший маркер) возвращать `null` — фикс просто не предложится.
- `Marker` не имеет номера строки → itest по числу замечаний, позицию
  проверять смоуком.
- `git checkout -- <bundle>/...` откатывает и СВОИ незафиксированные вставки
  (qfix/Messages) — вставки констант в Messages делать ПОСЛЕ экспериментов
  с checkout, либо перезапускать вставку.
- Точечные `-pl`-прогоны Tycho создают `.tycho-consumer-pom.xml` по модулям —
  в .gitignore, не коммитить.
