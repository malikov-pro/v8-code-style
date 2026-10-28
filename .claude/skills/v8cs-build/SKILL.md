---
name: v8cs-build
description: Сборка форка v8-code-style через compile.sh — EDT 2026.2.2 / Java 25, публичный p2 с metadata lock, тулчейн, itests и ошибки резолва.
---

# Сборка v8-code-style (форк)

## Канонический вход

> **Актуальное решение 26–27.10.2026:** поддерживается только EDT 2026.2.2
> (Java 25). Дефолт compile.sh и test-check.sh = 2026.2.2; старый target удалён.
> BREE/EE/compiler = 25, Surefire SYSTEM. Toolchain выбирается общим
> scripts/build-environment.sh; публичные p2-источники проверяются
> scripts/verify-public-platform.py по targets/default/public-platform-lock.json.
> Приватный архив/EDT_P2_ARCHIVE_URL не нужны. Инструкции —
> docs/build-edt-2026.2.2.md. Нет сети/metadata изменились → ошибка до Maven.

`bash compile.sh` в корне репозитория (mvn-команду «из головы» не собирать):

| Команда | Что делает |
|---|---|
| `bash compile.sh` | **EDT 2026.2.2**, Java25, targets/default |
| `bash compile.sh --profile edt-2026.2` | алиас того же режима |
| `bash compile.sh --skip-tests` | быстрая итерация (резолв+компиляция+p2, ~5 мин) |
| `bash compile.sh --full` | в точности CI апстрима (`-PSDK,find-bugs`, дольше) |
| `bash scripts/test-check.sh TestA,TestB --module bsl` | целевые тесты и цепочка реактора EDT2026.2.2 |
| `bash scripts/test-check.sh '*' --module ql` | весь QL-itests |

`--module`: bsl (default), ql, md, form, right. Старый профиль отвергается;
скрипт сам выбирает target, не конструировать
реактор через source/sed. Для bsl.ui/autosort и многобандлового общего
изменения использовать канонический `compile.sh`, пока нет отдельного входа.

## Прогоны по радиусу изменения

| Шаг | Проверка |
|---|---|
| Новое API | Проверить сигнатуры/экспорт пакетов и existing пример; research на один вопрос при необходимости |
| Итерация | Целевой TestClass +затронутые соседние тесты; `--skip-tests` лишь компиляция, не зелёная диагностика |
| Приёмка мини-пачки2–3 одного канала | Весь itests-модуль этого канала на 2026.2.2, стабильный снимок |
| Только тексты карточек/messages | Целевой `CheckDescriptionTest` канала +сверка generated индекса; runtime-гейт по тому же source не повторять |
| Основная пачка/общие зависимости | `compile.sh`, EDT2026.2.2 |
| Релиз | `compile.sh`; `--full`/SDK — когда нужен SDK/CI-кандидат |

Изменения EP, ключей локализации, defaults/параметров или MANIFEST не
считать текстовыми. Findings из live smoke могут потребовать новых
регрессий и повторного гейта. Длительности ориентировочные, не критерий успеха.
Изменение только AGENTS/CLAUDE/skills не требует Maven: проверять Markdown,
ссылки/команды и согласованность инструкций. Не путать эти файлы с runtime
markdown-карточками, для которых нужен CheckDescriptionTest.

Частота уточнена пользователем 17.10: полный гейт — на основную волну,
общую инфраструктуру/зависимости и релиз, не на каждый локальный fix/коммит.
Полный suite включает гейт канала: если он нужен сейчас, не запускать перед
ним тот же канальный suite отдельно. После локального runtime-fix — targeted,
затронутый канал и новый saved-p2 live; все семь модулей повторять при новом
инфраструктурном риске. Проверенный снимок не пересобирать ради merge/push.

Финальный полный гейт и предыдущий live могут иметь разные квалификаторы.
Сопоставлять хеши всех runtime/fixture/build-входов и явно сохранять это
сравнение. Совпадение входов не доказывает побайтовое равенство p2: писать
отдельно «последний сохранённый» и «фактически установленный/live-проверенный».
Не утверждать live-приёмку нового бинарника, если он не устанавливался.

## Стабильный снимок и отчёт

1. Один Maven в checkout; source/test/fixture/build scripts не редактировать
   до окончания. Background — ждать уведомления, не sleep/poll.
2. Перед стартом записать commit и diff/хеши, включая untracked исходники.
   Не выдавать прогон прежних файлов за проверку текущего дерева.
3. После результата проверять exit code +BUILD SUCCESS и XML каждого
   реально выполненного модуля. Отдельно totals tests/failures/errors/skipped,
   команда/профиль/длительность. Существующий targeted-script всего этого
   автоматически не сохраняет — собрать явно в отчёте.
4. Сохранить p2 и его хеши в уникальный durable-каталог свидетельств
   (например, `../_notes/evidence/`; `/tmp/opencode` — только временные данные)
   до следующего clean. Записать qualifier/путь; это artifact для smoke,
   не последний случайно оставшийся `target/repository`.
5. Generated docs проверить и коммитить с правкой; правки только текстов
   проверять целевым тестом карточек. Crash/hs_err хранить отдельно от git.

### Ловушки свидетельств (27–28.10)

- `git ls-files -co --exclude-standard` включает tracked paths, удалённые
  до старта. Для обоих snapshot одинаково фильтровать `is_file()`, удалённые
  пути учитывать отдельно через git diff. Иначе отсутствующий и до, и после
  файл становится ложным added и sourceUnchanged=false. Прецедент — удаление
  targets/edt-2026.1. Ошибка wrapper не означает автоматически ни провал, ни
  успех: заново сравнить полный набор фактических файлов/хешей, сохранить
  отдельный correction-proof, исходный results.json не переписывать.
- Если полный прогон остановился на одном research expected, проверить причину
  до его изменения. После подтверждённой test-only правки можно принять ранее
  зелёные модули + весь исправленный канал + не выполненные модули, если
  runtime/build/fixtures/platform hashes совпадают. Явно перечислить источники
  XML; это **составной гейт**, прежний full exit остаётся красным. BREE/JVM/
  dependencies/target изменились — нужен новый единый полный гейт.
- `-Dtest='*'` может включить абстрактный базовый класс как дополнительный
  skipped test. Сверять suite/test names, а не только totals: +1/+1 skip не
  доказывает потерю покрытия и не разрешает скрыть настоящий новый skip.
- Canonical полный гейт и targeted строят разные наборы reactor-модулей.
  Saved p2/qualifier брать из принятого прогона до следующего clean; одинаковые
  входы разных сборок не означают побайтово одинаковые JAR.

### Публичная платформа и CI

- Штатный target получает EDT из ruby2026.2, дополнения из Orbit4.38,
  Eclipse2025-12 и Eclipse license. Lock фиксирует content/artifacts всех sites,
  target — точные версии roots. Preflight не пропускать из-за тёплого кэша;
  отсутствующий индекс не считать pass. При изменении upstream lock обновлять
  только после исследования, нового cold/full гейта и saved-p2 live.
- SCR/SWT SVG/EventAdmin должны быть не только target roots, но и явными
  extraRequirements tests/pom.xml. Без них возможны validate-classpath failure
  или старт UI без IEventBroker. Не удалять обязательные capabilities из JAR.
- Metadata preflight проверяет состояние на момент запроса. Он не устраняет
  гонку с mutable upstream; бинарные checksum проверяет Tycho при загрузке.
  YAML parse не равен actionlint/GitHub execution, локальный cold не равен
  GitHub-hosted pass. Remote запуск только по отдельному разрешению.

### Исторический приватный snapshot (не fallback штатной сборки)

- P2 metadata не всегда содержит SHA-256 каждого артефакта (в том числе
  feature JAR и распакованных в исходной установке bundles). Поэтому одного
  artifacts.jar checksum недостаточно: проверять metadata hashes **и полный
  канонический inventory** из platform-lock.json. Не пропускать бесконтрольные
  файлы и не генерировать новый lock автоматически при несовпадении.
- Tycho 5.0.2 умеет `${env_var:EDT_P2_REPOSITORY}` в target; подтверждено
  javap DefaultTargetDefinitionVariableResolver и настоящей сборкой.
  Подготовка платформы должна предшествовать **любому** Maven, включая
  tycho-versions:set-version: резолв target возможен до lifecycle сборки.
- Импорт из локального repo, импорт tar и загрузка private HTTPS — разные
  доказательства. Unit-тест tar не доказывает доступность remote URL/secret.
  YAML parse не равен actionlint/GitHub execution; локальный гейт не доказывает
  холодный runner. Эти ограничения явно оставлять в отчёте перед релизом.
- Оригинальная публикация snapshot — исторические внешние скрипты ниже.
  prepare-platform.py сохранён для отдельной проверки/import прежнего snapshot;
  compile.sh его больше не вызывает. Не перепубликовывать/не менять старый lock.

## Тулчейн (без sudo)

- Полный JDK **25** — `~/tools/jdk-25.0.4.1+1`; Maven **3.9.9** — `~/tools/apache-maven-3.9.9`.
  Скрипт находит их сам; переопределение — `--java-home`/`--maven-home`.
- p2-кэш — `~/.m2/repository/p2` (первый прогон нового профиля холодный —
  десятки минут, дальше минуты).

## Что важно знать

- **Только EDT2026.2.2**: бандлы/компилятор/EE JavaSE-25; не возвращать
  совместимость с Java17. Платформа содержит ASM9.9.1.
- **itests долгие**: полный прогон ~35–40 мин. Итерации — целевые тесты;
  полный прогон не нужен перед каждым коммитом отдельной проверки,
  но обязателен для общей инфраструктуры, основной пачки и релиза.
- **Исторически edt.1c.ru рвал отдачу natives.library**; публичная сборка
  зависит от доступности источников. Сетевой обрыв отличать от несовпадения
  SHA-256; последнее не лечить ретраями или автоматическим изменением lock.
- Сборка **перегенерирует `docs/checks/*`** (индекс/карточки проверок) —
  эти изменения после сборки нормальны, коммитить вместе с правками.

## Чего не делать

- Не менять locked platform молча и не публиковать EDT-бинарники.
- Не пинить версию таргет-артефакта в bom/tests — только `${project.version}`
  (иначе релизный `set-version` ломает резолв таргета).
- Не запускать второй mvn по тому же дереву, пока идёт сборка (reactor/clean).
- Не редактировать выполняющийся shell-script: Bash может дочитать его
  после Maven и завершиться с syntax error даже после BUILD SUCCESS.
- SWT/AT-SPI crash в `gtk_widget_is_sensitive` не assertion failure.
  Не совмещать pyatspi UI automation с itests; для известных стендов
  используется `NO_AT_BRIDGE=1`. Не менять глобальные настройки пользователя.

## Типовые ошибки резолва (включая исторические старых платформ)

| Симптом | Причина/решение |
|---|---|
| `org.eclipse.swt.svg could not be found` | Запрашивается bom'ом (extraRequirements → свойство `extra.requirement.swt.svg`); в ruby/2026.1 его нет — дефолт `org.eclipse.swt` |
| `org.osgi.service.http.whiteboard not found` | Расщеплённые IU появились после 2023-12; из таргета edt-2026.1 убраны (пакеты отдаёт osgi.services) |
| `com._1c.g5.v8.dt.bm.common ... requires JavaSE-25` | В ruby/2026.1 смешаны поколения; таргет edt-2026.1 + EE 17 выбирает совместимые версии |
| ECF `httpclientjava` требует `org.eclipse.ecf 3.12.0` exact | Сайт rt/ecf/3.15.7 рассинхронизирован; ECF берётся из releases/2023-12 |
| `Invalid Collect Request: null` с classifier `lib/*.jar` | Maven 3.10/Resolver 2 несовместим с вложенными JAR Tycho 5.0.2 (eclipse-tycho/tycho#6399); использовать фиксированный Maven 3.9.9 как в compile.sh, не системный mvn из ubuntu-latest |
| Surefire: `org.eclipse.ui.ide.application ... could not be found`, хотя IU есть в локальном p2 | IU должен попасть в resolved target: добавить явным `<unit>`; репозиторий сам по себе не равен target. Для 2026.2.2 убрано смешение Eclipse/Orbit/ECF с локальной платформой. Минимальный md CheckDescriptionTest 27.10: 1/0/0/0, BUILD SUCCESS |
| После перехода на локальный target нет `jakarta.activation-api/1.2.2` | В установленной 2026.2.2 — 2.1.4. Сверять каждый explicit IU/version с content.jar, не копировать старые пины из Orbit вслепую |
| ExtensionMethodVisibleMode не видит исходные методы, NotifyDescription даёт лишний маркер в расширении | Одной `dt.feature` вместо продуктового IU недостаточно: в test runtime не попала `com._1c.g5.v8.dt.extension`. Без `dt.bsl.extension` используется DefaultModuleExtensionService с emptyMap. Добавить extension feature в target И tests extraRequirements. Красный повтор 5/3/0/0 → зелёный 7/0/0/0 на 2026.2.2 (27.10), без изменения checks/assertions |
| QL research AnyRef exact list содержит лишний `ChartOfCharacteristicTypesRef.QueryDimensions` | На 2026.2.2 TypeSet разворачивает и ПВХ общей QlFullDemo fixture. Проверить реальный metadata-состав: QueryDimensions добавлен вместе с QueryAccounts в 3f1014b1. Обновить точный список, сохранить CatalogRef-only negative assert; targeted research + product tests 13/0/0/0 (27.10). Это не основание менять продуктовый детектор или ослаблять assert до containsAll |

Локальный p2 публиковать только в пустой каталог (прежний переименовать для
отката), без `-append`; затем запускать `~/edt/patch-p2-ranges.py`.
Mockito 5.20.0 / Objenesis 3.4.0 / Byte Buddy + agent 1.18.1 взяты из
Maven-кэша (их JAR уже OSGi); JDT — bundle `org.eclipse.jdt.core`, ECF
httpclientjava — bundle `org.eclipse.ecf.provider.filetransfer.httpclientjava`
(соответствующих feature IU в установленном продукте нет).
