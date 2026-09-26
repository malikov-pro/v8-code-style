# AGENTS.md — форк v8-code-style (malikov-pro)

Форк [1C-Company/v8-code-style](https://github.com/1C-Company/v8-code-style) —
проверки «1С:Стандарты разработки» для 1C:EDT на фреймворке
`com.e1c.g5.v8.dt.check`. База для собственных проверок; родительский проект —
соседний `bslls-connector-for-edt` (канал диагностик BSL LS). Общие правила
каталога — в `../AGENTS.md`.

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
- Стенды: EDT 2026.1 → `~/edt/rt_test`; EDT 2026.2 → `~/edt/2026_test`
  (пользователь разрешил рестартовать EDT на стендах без спроса).

## Доставка (update site)

- http-update-site: **https://malikov-pro.github.io/v8-code-style/** (GitHub
  Pages, build_type=workflow). Workflow'и: `release.yml` (тег X.Y.Z → сборка
  обоих профилей → GitHub Release с двумя p2-zip) и `deploy-update-site.yml`
  (release/тег/dispatch → сборка → Pages).
- Для публикации тегом: в среде `github-pages` добавлено правило `0.*`
  (deployment tag policy) — без него director-деплой тега отклоняется.
- Апстримные `ci-build.yml`/`fork-pull.yml` на форке отключены (не адаптированы
  под наш пайплайн); `release.yml` апстрима ЗАМЕНЁН нашим — это дифф форка.
- Версия таргет-артефакта в bom/tests — `${project.version}` (не пинить:
  после релизного `set-version` резолв таргета ломается).

## Особенности репозитория

- Сборка перегенерирует `docs/checks/*` и карточки (индекс проверок) —
  изменения этих файлов после сборки нормальны, коммитить вместе с правками.
- Версия 0.8.0-SNAPSHOT; квалификатор `vyyyyMMdd-HHmm` из времени сборки.
- itests поднимают OSGi-рантайм с тестовым проектом (база — `CheckTestBase`
  из EDT); при сборке под 2026.1 следить, чтобы тестовая фича EDT резолвилась.

## Дубли с BSL LS (открытый вопрос)

Часть проверок v8-cs пересекается по смыслу с диагностиками BSL LS (канал
коннектора). Точка сведения нормативки — `../_ext_src/v8std` (статьи стандарта,
оттуда же генерируется каталог коннектора). Политика дедупликации не выбрана;
до того — не добавлять проверки, дублирующие существующие с обеих сторон,
без сверки по каталогам.
