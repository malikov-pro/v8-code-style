---
name: v8cs-port-check
description: Конвейер переноса проверки АПК в форк v8-code-style — от выбора правила в apk.db до релиза. Использовать при переносе проверок АПК/BSL, добавлении проверок по готовому алгоритму 1С, учёте прогресса конвейера.
---

# Конвейер: перенос проверки АПК → проверка форка

Смежный регламент: `v8cs-port-ls` — перенос диагностик BSL Language Server
(гэп-лист `_notes/ls-port-gap.md`). Статус конвейера — в `apk.db` (таблица
`ported`); работа с реестром — скилл `v8cs-apk-db`. Первая проверка идёт по
этому регламенту полностью; после прогона регламент дополняется находками
(мета-правило AGENTS.md).

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

- BSL: `AbstractSingleModuleTestBase` +нарушающий/чистый ресурс. QL:
  `AbstractQueryTestBase` +`.ql`; при owner-specific инварианте нужен также
  настоящий bridge/BM integration тест. MD/form/right: fixture или BM-тест
  по паттерну канала, не BSL-scaffold ради формального наличия.
- С первого теста bad→fix→bad, создание/удаление **и замена existing values**.
  Проверять конкретные объекты, offsets/lines, отсутствие лишних markers.
  Parent flags/command source, языковые map-entry.value и связанные типы
  не меняют свойства якоря автоматически — incremental regression обязательна.
- Form markers: `getNestedMarkers` +CheckUid filter для дочерних объектов,
  не только root markers. Не совмещать root traversal с containment-якорем.
- Локально: точечный прогон своего теста —
  `bash scripts/test-check.sh <TestClassName[,NeighborTest]> --module <канал>`;
  `--profile edt-2026.2` для второй платформы. Clean reactor внутри.
  Незнакомое API сначала javap/export/пример; research на один вопрос.

## 3.5. Изолированный пример в EDT

Пример по каналу (BSL-модуль, metadata object, форма, набор СКД) создавать
в разрешённом изолированном проекте стенда через MCP. Шапка/отчёт содержит
ожидаемые checks/count/objects/ranges. Формы/metadata/СКД не заменять
синтетическим модулем. В базу АПК пример не внедрять — она только источник.

## 4. Фиксация конвейера

- `INSERT INTO ported(rule, check_id, article, ported_at, commit_sha, notes)`;
  Если перенос отменён (X по шагу 1) — тоже в `ported` с notes-причиной:
  правило больше не кандидат.

## 5. Сдача

- Ветки (как у всего каталога): фича/пачка живёт в `feature/apk-ports`
  (или `feature/<тема>`) **от `develop`**; merge `--no-ff` в `develop`
  после зелёного прогона. `master` — только под релиз: merge из `develop`
  + тег `X.Y.Z` **по явной команде пользователя** (release.yml соберёт
  Release+сайт).
- Рабочие мини-пачки —2–3 связанных checks одного канала внутри основной
  пачки до10. Собственный тест/учёт каждого чека; один итоговый гейт канала
  на стабильном снимке, не весь suite после каждого правила.
- Верификация — по радиусу поражения (v8cs-build):
  | Шаг | Проверка |
  |---|---|
  | Итерация | Целевой класс и соседние tests канала |
  | Перед merge мини-пачки | Весь затронутый itests-модуль **обеих платформ** |
  | Только markdown/messages-тексты | CheckDescriptionTest канала +generated index |
  | Основной batch/общая инфраструктура или dependencies/релиз | `compile.sh` всех modules **обеих платформ** |
- Snapshot freeze/один Maven; active script/source/tests не менять.
  Сохранять commit+diff/hash, exit code/профиль/время/XML counts, p2 профиля
  до следующего clean. Новые ключи/EP/parameters не считать doc-only.
- После green source-gates own live smoke на **этом артефакте** до merge:
  initialclean/new check→bad/fix безclean→finalclean/log. Smoke defect →
  красная regression/fix/соответствующий rerun, не «сборка уже зелёная».
- Затем audit/merge develop/push/учёт завершения. Release master/tag —
  только по явной команде. Feature-ready !=merged !=installed !=released.
- Перед merge пачки — **гейт полноты** по каждому feature-коммиту:
  `python3 scripts/audit-check-integration.py --source-commit <sha> ...`.
  Проверяет фактические классы и регистрацию, не только ancestry (порты
  могут быть cherry-pick). Прецедент 10.10: up-725/up-722/up-710/bad-words
  были готовы в ветках, но отсутствовали в develop несмотря на зелёный
  гейт и tracking «ГОТОВО». Generated docs не заменяют этот контроль.

## Учёт и независимая работа

До реализации claim в `ported`: notes=`в работе, <дата>`, без завершённого
SHA. Только gates/live/audit/merge переводят в ГОТОВО с sourceSHA/mergeSHA/
ограничениями. Дедуп/runtime/отложено — не считать прямым переносом.
Во время background build можно готовить live fixture/читать кандидата,
но не менять snapshot. Субагентов не запускать без явного разрешения
пользователя/применимых инструкций; если разрешены — независимый prescreen
полезнее параллельных правок общих Messages/plugin.xml/Maven.
Worktree только по явной просьбе пользователя.

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
- Existing fix-itests использовать, если канал поддерживает; иначе own
  live qfix smoke и честное ограничение в `ported.notes`. Detection-pass
  не доказывает применимость/безопасность быстрого исправления.
- Для этой задачи: простой заменимо-механический фикс (как замена ё→е)
  делаем сразу; где исправление «только пользователем» — фикс не делаем,
  в карточке пишем рекомендацию.

## Примеры в расширении АПК.ОшибкиДляПроверки (MCP edt-apk)

Активный workspace `~/edt/apk`, MCP `edt-apk`. Actual project names читать
через `list_projects`, не подставлять старые имена. Известные изолированные
примеры находятся в `v8cs.Smoke1014`, `v8cs.OptionParams15`; для расширения
использовался `apk.ПроверкаОшибок`. Выбирать native/extension owner по
инварианту; не превращать исходную рабочую конфигурацию в набор нарушений.

- Создание: `create_metadata` (projectName=`<разрешённый-smoke-проект>`,
  fqn=`CommonModule.ош_<Slug>`) → текст: `write_module_source`
  (objectName=`CommonModule.ош_<Имя>`, moduleType=Module, mode=replace).
- Имена MCP по умолчанию нормализуются ё→е. Если инвариант именно о
  metadata name/synonym/tooltip, явно задать нужное normalization поведение
  и проверять persisted значение; не подменять metadata-тест текстом BSL.
- В шапке примера — ожидаемое число и позиции вхождений (спецификация);
  `write_module_source` защищён от lost-update (`expectedSource`), нормализует
  символы (длинное тире и пр.), прогоняет синтакс-контроль.
- Точный текст тянется `read_module_source` по `modulePath`
  (`CommonModules/<Имя>/Module.bsl`).
- Проверка детекции: в этой же EDT — наш перенос (`apk-…`) вешает маркеры на
  модуль примера (видно в «Ошибки конфигурации» и через MCP
  `get_project_errors`); сверить число/строки с шапкой примера.
  **База АПК для прогонов НЕ используется** — только источник правил
  (запросы). Схема самотеста через ВариантыПроверки/Демонстрационную
  конфигурацию — не наша.

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
- **`Marker` не имеет простого getLine()**: BSL-позиция через
  `marker.getExtraInfo().get(StandardExtraInfo.TEXT_LINE)`, directQL через
  test QueryMarker ranges. Учитывать locale, проверять объект/позицию,
  не только count. Qfix live не заменяется зелёным detection-тестом.
- Конструктор проверки — **public** (ExecutableExtensionFactory в другом
  пакете; private → InstantiationException).
- При удалении «неиспользуемых» импортов не удалить нужный (EcoreUtil) —
  ошибка компиляции ловится `--skip-tests` прогоном.
- `mvn verify -pl <модули>`: реактор НЕ подтягивает таргет-модуль по -am —
  перечислять цепочку явно (targets/default, все bundles, docs В КОРНЕ
  репо — не bundles/, feature, repository, itests-фрагмент).
- **`-pl` без `clean` после пересборки бандлов = каскад падений itests**:
  в `tests/*/target/work/data` остаётся воркспейс прежних квалификаторов —
  с первых тестов «Cannot get bundle project with name CommonModule» /
  NPE `getProject()==null` (0.1 сек на тест) и ханг в
  `TestingProjectLifecycleSupport.waitForProjectStart`. Лечение: `clean`
  (вытирает тестовый воркспейс), `test-check.sh` делает это всегда.
- Точечный тест: `-Dtest=ApkYoLetterCheckTest -DfailIfNoTests=false`.
- INSERT/UPDATE в apk.db — только с абсолютным путём (cwd бывает другим).

## Quick fix: грабли реализации (АПК_00260, итерация 2)

- **Два разных бандла qfix**: `com._1c.g5.v8.dt.bsl.check.qfix` (платформенный,
  НЕ в Import-Package форка — импорт оттуда = «cannot be resolved» каскадом)
  и `com.e1c.g5.v8.dt.bsl.check.qfix` (нужный: `SingleVariantXtextBslModuleFix`,
  `IXtextBslModuleFixModel`, `FixConfigurer`). Перепутать легко — javap-вывод
  был из _1c-jar.
- **`interactive(false)` НЕ поддерживается фреймворком**: FixConfigurer бросает
  `IllegalArgumentException: Non-interactive quick fix mode is not supported
  yet` — но не в UI, а при РЕГИСТРАЦИИ фикса (FixRepository.init на старте
  бандла). Пока фикс не зарегистрирован в EP, сломанный configureFix не
  вызывается и выглядит рабочим. Регистрация + interactive(false) = падение
  LINKING-фазы проектного контекста = ВСЕ itests бандла висят/падают.
  Только `interactive(true)` (как в апстримных фикса).
- Позиция замечания в фиксе — `model.getIssue().getOffset()/getLength()`
  (xtext Issue); текст — через `model.getDocument().get(offset, length)`
  (`XtextResource.get` не существует).
- Точечный фикс = замена по `getIssue().getOffset()`; на позиции без ожидаемого
  символа (устаревший маркер) возвращать `null` — фикс просто не предложится.
- BSL-позицию утверждать через StandardExtraInfo.TEXT_LINE/доступный range,
  затем own live сверка; не оставлять только count.
- Не применять `git checkout --`/reset для очистки итерации: можно потерять
  пользовательские и собственные незавершённые вставки. Исправлять точечным
  patch, сохранять snapshots; временный debug dump удалить явно до commit.
- Точечные `-pl`-прогоны Tycho создают `.tycho-consumer-pom.xml` по модулям —
  в .gitignore, не коммитить.

## itests: JVM тестов — Java25 SYSTEM (с 28.10.2026)

- Текущая платформа только EDT2026.2.2, BREE/EE/compiler25.
  `tests/pom.xml` → `<useJDK>SYSTEM</useJDK>`: полный JDK25 сборки, ASM9.9.1
  в locked platform. Локальные поддельные java17 toolchains не нужны.
- История 2026.1: spifly1.3.7 + ASM9.6 не понимали major69; поэтому тогда
  требовалась отдельная JVM17/BREE. Не переносить это ограничение на2026.2.2.
- Симптомы отличить: NPE `getProject()==null` / «Cannot get bundle project»
  каскадом с первых тестов — тоже из этой семьи (воркспейс не стартовал).
- Новый target/ASM/JVM требует полного гейта, targeted не доказывает все каналы.

## Продуктовый smoke: после установки новой версии — clean build проекта

- Новые проверки в установленном плагине **не появляются** от
  `revalidate_objects` — маркеры возникают только после **полной перестройки
  проекта** (MCP `clean_project`, в UI «Проект → Очистить…» / пересоздание
  сборки). Симптом: другие проверки плагина стреляют, новые — нет; журнал
  чистый, регистрации на месте.
- Проверка детекции через MCP (сервер в запущенной EDT): `clean_project` →
  `get_project_errors` (опц. `checkId='apk-…'`) → сверить число/строки с
  шапкой модуля-примера. `revalidate_objects` для этой цели недостаточен.
- После initialclean изменения existing value **без clean**:
  нарушение→fix→исчез, чистое→нарушение→появился; parent/dependencies если
  применимо. Затем fullclean+журнал (00136: stale LocalStringMapEntry.value).
- Pass только на правильном qualifier, объектах/позициях без дублей.
  OURS-real чинить, known PLATFORM записать отдельно. Startup Unknown check
  id после update не игнорировать: сохранить первый лог, restart/clean и
  проверить latest session; только отсутствие повторения даёт pass.
