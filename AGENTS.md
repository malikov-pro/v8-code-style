# AGENTS.md — форк v8-code-style (malikov-pro)

Форк [1C-Company/v8-code-style](https://github.com/1C-Company/v8-code-style) —
проверки «1С:Стандарты разработки» для 1C:EDT на фреймворке
`com.e1c.g5.v8.dt.check`. База для собственных проверок; родительский проект —
соседний `bslls-connector-for-edt` (канал диагностик BSL LS). Общие правила
каталога — в `../AGENTS.md`.

## Процесс (изменено 05.10)

- **Ветки**: `develop` — интеграция; фичи/пачки — `feature/*` от `develop`;
  merge `--no-ff` в `develop` после зелёного прогона. `master` — только под
  релиз: merge из `develop` + тег `X.Y.Z` **по явной команде пользователя**.
- **Верификация по радиусу поражения** (полный прогон НЕ на каждый чек):
  итерация — `scripts/test-check.sh <TestClass>` (~5–7 мин); перед merge —
  цепочка реактора + itests-модуль своего бандла целиком (~10–15 мин);
  полный `compile.sh` — перед релизом, правки общих мест, раз в пачку
  (~35 мин). Регламент — скилл `v8cs-port-check`.
- **10.10: зелёный гейт не доказывает полноту пачки.** Четыре готовые
  feature-проверки №12 не попали в интеграцию, но гейт прошёл без них.
  Перед merge сверять исходные feature-коммиты с фактическими классами
  и регистрациями: `python3 scripts/audit-check-integration.py
  --source-commit <sha> ...`. Скрипт разбирает plugin.xml, ловит склейки,
  дубли и отсутствующие исходники; учитывает cherry-pick (одной проверки
  git ancestry недостаточно). Tracking «готово» отличать от «влито».

## Платформы и сборка

- **Основная цель — EDT 2026.1** (рабочая платформа); **EDT 2026.2** — пре-релиз.
  Один исходник на обе платформы: бандлы собираются как **JavaSE-17**
  (JVM 17 → JVM 25 совместим снизу). BREE обратно на 25 не поднимать.
- Вход — `bash compile.sh`:
  - без флагов — EDT 2026.1 (таргет `targets/edt-2026.1`, ruby/2026.1);
  - `--profile edt-2026.2` — пре-релиз (таргет `targets/default`, ruby/2026.2);
  - `--skip-tests` — быстрые итерации; полный прогон itests ~35 мин;
  - `--full` — в точности CI апстрима (`-PSDK,find-bugs`).
- Тулчейн без sudo: JDK 25 — `~/tools/jdk-25.0.4.1+1` (только тулчейн сборки),
  Maven 3.9.9 — `~/tools/apache-maven-3.9.9`.
- Апстрим собран под JavaSE-25/EDT 2026.2; отличие форка от master:
  `bom/pom.xml` (компилятор 17, EE 17, свойство `edt.target.artifact`,
  профиль `edt-2026.2`), все `MANIFEST.MF` (BREE 17), `targets/` (модуль
  `edt-2026.1`). Это осознанный дифф — при синке с апстримом эти места
  переносить руками, не откатывать.

## Деплой и проверка в EDT

- `bash scripts/deploy-edt.sh --edt <инсталляция> --workspace <ws>`
  (p2 director, EDT при деплое закрыта; скрипт сверяет все бандлы
  `com.e1c.v8codestyle*` с `bundles.info` и чистит устаревшие jar пула).
- v8-cs **поставляется в комплекте EDT**: деплой тем же IU-именем обновляет
  комплектную копию (наш квалификатор `v<дата>` всегда свежее). Откат —
  только переустановка/восстановление комплектной версии.
- **EDT 2026.2 требует полную Java 25 (JDK + JavaFX + jshell)**: подходит
  `/usr/lib/jvm/axiomjdk-java25-pro-full-amd64`. На неполных JVM ломаются
  карточки замечаний ВСЕХ провайдеров (AI-плагин `com.e1c.edt.ai.ui` строит
  их через Guice и падает: без FX — `NoClassDefFoundError javafx/scene/Node`,
  на JRE без jshell — `jdk/jshell/spi/ExecutionControlProvider`; исключение
  из contributor'а рвёт построение карточки — try/catch в bsl.ui 24.0.0 нет).
  Запуск: `1cedt -vm /usr/lib/jvm/axiomjdk-java25-pro-full-amd64/bin/java`.
  На EDT 2026.1 — axiom-jdk-full-17 из комплекта.
- Не удалять jar из `~/.p2/pool` при работающей EDT (CNFE в рантайме).
- **Стенды (изменено пользователем 30.09)**: `rt_test` и `2026_test` удалены.
  Активный воркспейс — **АПК** (`~/edt/apk`, EDT 2026.1.3.25, MCP `edt-apk`):
  там же расширение `АПК.ОшибкиДляПроверки` (примеры конвейера). Деплой форка
  в эту инсталляцию — только по явной просьбе (это рабочая среда АПК).
  EDT 2026.2 installation существует, воркспейса нет; smoke проверок —
  на временно создаваемом воркспейсе или договорённость с пользователем.

## Доставка (update site) — работает

- Сайт: **https://malikov-pro.github.io/v8-code-style/** (Pages, build_type=
  workflow) — отдаёт p2 и index.html с квалификатором. URL ставить в
  «Справка → Установить новое ПО»; обновляет комплектный v8-cs на месте.
- Release **0.8.0**: zip обоих профилей (`v8codestyle-edt2026.1/2026.2-0.8.0.zip`).
- Workflow'и: `release.yml` (тег X.Y.Z → сборка обоих профилей **с ретраями
  ×3** — edt.1c.ru рвёт отдачу natives.library раннерам GitHub → GitHub
  Release) и `deploy-update-site.yml` (release/тег/dispatch → сборка → Pages).
- Теговые деплои: в среде `github-pages` добавлены правила `0.*` (и для
  branch, и для Tags через UI) — деплой из тега работает. Через API теговые
  политики недоступны, branch-правило теги не покрывало.
- Апстримные `ci-build.yml`/`fork-pull.yml` на форке ОТКЛЮЧЕНЫ (не адаптированы
  под наш пайплайн); апстримный `release.yml` ЗАМЕНЁН нашим — дифф форка.
- Версия таргет-артефакта в bom/tests — `${project.version}` (не пинить:
  после релизного `set-version` резолв таргета ломается).
- Прочие грабли CI: «зомби»-прогоны (in_progress навечно) — cancel + rerun;
  `pgrep -f` в inline-командах с путями стенда — самострел, пути собирать
  конкатенацией.
- **10.10: Maven в release/Pages фиксирован на 3.9.9**, как локально.
  Maven 3.10 из ubuntu-latest с Resolver 2 отвергает classifier `lib/*.jar`,
  инжектируемый Tycho 5.0.2: `Invalid Collect Request: null`
  (eclipse-tycho/tycho#6399). A/B на теге 0.9.0: 3.10 — failure,
  3.9.9 — BUILD SUCCESS обоих профилей; кэш невиновен.
  Исправленный workflow можно вызвать из master с `-f tag=X.Y.Z`
  (`release.yml`, затем `deploy-update-site.yml`): checkout существующего
  тега, без пересоздания. Rerun старого запуска исправление не подхватывает.

## Особенности репозитория

- Сборка перегенерирует `docs/checks/*` и карточки (индекс проверок) —
  изменения этих файлов после сборки нормальны, коммитить вместе с правками.
- Версия 0.8.0-SNAPSHOT; квалификатор `vyyyyMMdd-HHmm` из времени сборки.
- itests поднимают OSGi-рантайм с тестовым проектом (база — `CheckTestBase`
  из EDT); при сборке под 2026.1 следить, чтобы тестовая фича EDT резолвилась.

## Управление проверками из кода (API настроек, EDT 2026.2)

Фреймворк `com.e1c.g5.v8.dt.check.settings` даёт полный цикл **программного**
управления проверками на уровне проекта (декомпиляция 6.0.102):

| Операция | API |
|---|---|
| Чтение настройки | `getSettings(CheckUid, IProject)` → `ICheckSettings`: `isEnabled()`, `setEnabled(bool)`, `setSeverity`, параметры |
| Запись | `applyChanges(Collection<ICheckSettings>, IProject)` — сохраняет и перезапускает валидацию |
| id ↔ UID | `getCheckUidForCheckId("id", project)` / `getUidForShortUid` / `toUid` (UID = plugin id + check id) |
| События | `addChangeListener(ICheckSettingsChangeListener)` / `removeChangeListener` |
| Профили валидации | `getAvailableSettingsProfiles`, `getActiveSettingsProfile`, `setActiveSettingsProfile`, `import/export/duplicate/rename/deleteSettingsProfile` — настройки живут профилями, applyChanges пишет в активный |
| Массовые правки | `setMassiveCheckProcessDisabled(true, project)` → пачка правок → `false` |
| Динамические проверки | `registerChecks/unregisterChecks(ICheckProvider)` (+ `CheckInfo`) — кодом, без extension point |

**Дедупликация** («включили точную — погасить грубую»), принципы:

1. По событию `addChangeListener`, НЕ «при старте»: при старте проекты не
   загружены; ранний вызов — «не знаю» без кэширования, дефолт = прежнее
   поведение (образец — `LsProjectGate` коннектора, issue #26).
2. Однонаправленно: гасим грубую при включении точной; обратно никогда
   не включаем (пользователь мог выключить сознательно).
3. Опционально и прозрачно: выключатель в настройках, дефолт — ничего не
   трогаем; каждое автогашение — INFO в журнал.
4. Анти-пинг-понг: игнорировать изменения, порождённые самим дедупликатором.
5. Таблица пересечений: статья v8std → проверка v8-cs ↔ код LS ↔ (будущее)
   проверка АПК. Каталоги обеих сторон уже есть.

## Реализация дедупликации (09.10)**: `CheckSettingsDeduplicator` +
`CheckSubstitutionRegistry` в `com.e1c.v8codestyle.check`. Слушатель
`ICheckSettingsChangeListener` стартует из `CorePlugin` в фазе
`ServiceInitialization` (после готовности сервисов). Триггер — событие, где
изменился UID **точной** проверки пары; состояния обеих проверок
перечитываются из репозитория (событию не доверяем). Гейт — преференс
проекта `dedupSubstitutedChecks` (`CheckUtils.PREF_KEY_DEDUP_SUBSTITUTED_CHECKS`,
дефолт off; страница настроек НЕ сделана — включать программно, UI в бэклог).
Пары: apk-00126-md-no-yo-letter → mdo-ru-name-unallowed-letter;
create-query-in-cycle → query-in-loop. Событие только по грубой проверке
(включили сознательно) ничего не делает. Itest:
`CheckSettingsDeduplicatorTest` (md.itests) — прямой вызов `onChange`
против реального репозитория (события фреймворка асинхронны — file-watcher
профиля, ждать их в тесте флакочно).

**Авто-включение bsl-ql-hub (09.10, решение пользователя «включаем»)**:
`QlHubAutoEnabler` (com.e1c.v8codestyle.check). Хаб — EDT-мост «литералы
запросов модулей → QL-модель → ql-делегаты», зарегистрирован **выключенным**
(`.disable()`); enabler включает его для проектов с нетронутыми настройками.
Триггеры: sweep через 30 с после старта, ретрай 15 с на открытие проекта,
события настроек. Однократность — маркер-преференс
(`qlHubAutoEnabled.<project>` в InstanceScope): дефолт хаба «выключен», явное
отключение пользователя value-равно дефолту (customization удаляется) и без
маркера возвращалось бы. `isDefault()==false` до включения — не трогаем.
Каждое включение — INFO. **Itest-рантаймы**: property
`v8codestyle.qlHubAutoEnableDisabled` (tests/pom.xml vmargs) глушит фоновые
триггеры — асинхронное включение рестартует валидацию и гонит тесты, читающие
маркеры после waitForDD (флейк DbObjectUnlimitedStringCheckTest); тест зовёт
`ensureEnabled` напрямую. Грабли: `getSettings` для неготового профиля
возвращает ДЕФОЛТЫ (не бросает); `POST_OPEN` не существует — только
`POST_CHANGE`+флаг OPEN; Guice требует `@Inject`.

## Роль проекта: сборщик проверок

Форк — единый дом проверок: сюда переносятся проверки BSL LS (при этом
коннектор станет не нужен) и проверки АПК; сюда же собираются зависшие
задачи апстрима (1C-Company/v8-code-style issues) как бэклог. Карта
пересечений с нормативкой — `_ext_src/v8std`.

**Прогресс портов (06.10, партия №7)**: **36 своих проверок** — 2 АПК
(apk-00260, apk-01194, с qfix) + 34 BSL LS. Батчи №2-№7 (merge develop
`006e2205`/`19c3237e`/`0465eac3`/`9c992af6`/`5b0db929`/`27ca921b`), гейт
bsl.itests **368/0**. С 05.10 работа пачками по 10 правил (решение
пользователя); ограничение «50 портов до деплоя» снято — деплой после пачки.
**Замещение (политика 05.10)**: deprecated-find/current-date перенесены,
Найти/ТекущаяДата убраны из дефолта use-non-recommended-method; пересмотр
дедуп-вердиктов — `_notes/ls-port-gap.md`.
**Деплой + смоук (06.10)**: v8-cs 0.8.0.v20261005-2021+ в EDT 2026.1
(~/edt/apk); примеры ош_ПримерХардкод/ош_ПримерСтруктура/ош_ПримерКомментарии/
ош_ПримерКачество в apk.ПроверкаОшибки — живая детекция ВСЕХ партий 2-7,
qfix «yes», журнал чист. CodeRecognizer/BSLFootprint LS перенесены как
CommentCodeRecognizer. Гэп-лист — `_notes/ls-port-gap.md`; регламенты —
скиллы `v8cs-port-check` (АПК) и `v8cs-port-ls` (BSL LS).

**Схема работы с АПК**: база АПК (`onec-apk-data`) — ТОЛЬКО источник правил
(запросы: реестр, алгоритмы, описания). Прогоны/проверка детекции — в EDT
на расширении `АПК.ОшибкиДляПроверки` (примеры нарушений), детектит наш
переносимый чек. Конечная цель — проверки живут в этом форке (модуль для EDT).

## Sonar-конвейер (направление, 27.09)

Цель: на хосте с EDT — сборка из git-проекта по коммиту с отправкой списка
замечаний в Sonar (связки git↔EDT↔sonar и ключи — в json-конфиге).

Опорные блоки (все уже есть):
- MCP `edt-rt-test-malikov` — агент над живой EDT: `list_projects`,
  `import_configuration_from_xml`, `get_project_errors` (ошибки валидации!),
  git-ветки, `get_check_description` (описание проверки → маппинг на v8std).
- `1cedtcli` (в инсталляциях) — headless: import → validate → результат.
- Логика stebi (`_jenkins/PUBID_1117485-Scripts`) — конвертация
  `edt-result.out` → Sonar generic-issue JSON + фильтры (поддержка, файл
  настроек правил). Переносится в плагин, чтобы не зависеть от oscript.
- Отправка — sonar-scanner с generic-issue отчётом.

Схема: `git pull/checkout <commit>` → проект в воркспейсе (1cedtcli/MCP) →
валидация → выгрузка замечаний в Sonar-JSON (силами плагина) → sonar-scanner.
