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
- Локально: точечный прогон своего теста —
  `bash scripts/test-check.sh <TestClassName>` (цепочка реактора внутри,
  минуты, не весь suite).

## 3.5. Пример в расширение АПК.ОшибкиДляПроверки

Минимальный модуль-пример с нарушением (инвариант из шага 1) — через
`create_metadata` + `write_module_source` (см. раздел выше). В шапке —
ожидаемые позиции. Пример лежит в EDT-воркспейсе АПК: наш перенос видит его
в этой же EDT и вешает маркеры (проверка детекции — шаг 5/smoke).

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
- Верификация — по радиусу поражения (полный прогон НЕ на каждый чек):
  | Сценарий | Прогон | Время |
  |---|---|---|
  | итерация над чеком | `bash scripts/test-check.sh <TestClass>` | ~5–7 мин |
  | перед merge | та же цепочка, itests-модуль своего бандла целиком
    (bsl включает `CheckDescriptionTest` — синк карточек/доков) | ~10–15 мин |
  | полный `bash compile.sh` (все itests-модули) | перед релизом/тегом,
    правки общих мест (bom/targets/manifests), раз в пачку | ~35 мин |
- Merge → smoke на стенде (скилл `v8cs-deploy`) → релиз по команде
  пользователя (скилл `v8cs-release`: merge `develop`→`master` + тег).

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
- `Marker` не имеет номера строки → itest по числу замечаний, позицию
  проверять смоуком.
- `git checkout -- <bundle>/...` откатывает и СВОИ незафиксированные вставки
  (qfix/Messages) — вставки констант в Messages делать ПОСЛЕ экспериментов
  с checkout, либо перезапускать вставку.
- Точечные `-pl`-прогоны Tycho создают `.tycho-consumer-pom.xml` по модулям —
  в .gitignore, не коммитить.

## itests: JVM тестов — только через useJDK=BREE (находка 30.09.2026)

- Сборка идёт на JDK 25 (Tycho 5 требует 21+), но тестовая JVM должна быть
  **JDK 17** (BREE 2026.1): `tests/pom.xml` → tycho-surefire
  `<useJDK>BREE</useJDK>` + `~/.m2/toolchains.xml` (jdk 17 → axiom-jdk-full-17,
  jdk 25 → tools/jdk-25). SYSTEM-JVM (JDK 25) роняет itests: spifly 1.3.7
  при витье классов (ServiceLoader-детекция на старте бандлов) читает файлы
  JDK 25 (major 69) ASM'ом 9.6 — `Unsupported class file major version 69` →
  `ClassFormatError` → LINKING-фаза падает → старт проектного контекста
  не завершается → `waitForProjectStart`/`waitForDD` висят сотни секунд.
- Симптомы отличить: NPE `getProject()==null` / «Cannot get bundle project»
  каскадом с первых тестов — тоже из этой семьи (воркспейс не стартовал).
- При синке с апстримом tests/pom.xml: useJDK сохранить (апстрим на 2026.2
  с JDK 25 — там SYSTEM работает, у нас 2026.1 — нет).

## Продуктовый smoke: после установки новой версии — clean build проекта

- Новые проверки в установленном плагине **не появляются** от
  `revalidate_objects` — маркеры возникают только после **полной перестройки
  проекта** (MCP `clean_project`, в UI «Проект → Очистить…» / пересоздание
  сборки). Симптом: другие проверки плагина стреляют, новые — нет; журнал
  чистый, регистрации на месте.
- Проверка детекции через MCP (сервер в запущенной EDT): `clean_project` →
  `get_project_errors` (опц. `checkId='apk-…'`) → сверить число/строки с
  шапкой модуля-примера. `revalidate_objects` для этой цели недостаточен.
- Смоук-петля порта (задокументировано, скрипт по необходимости):
  build → deploy-edt.sh (EDT закрыта) → пользователь открывает EDT →
  MCP `clean_project` + `get_project_errors` → сравнение с ожиданиями
  из шапки примера.
