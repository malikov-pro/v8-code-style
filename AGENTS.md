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

## Доставка (update site) — работает

- Сайт: **https://malikov-pro.github.io/v8-code-style/** (Pages, build_type=
  workflow) — отдаёт p2 и index.html с квалификатором. URL ставить в
  «Справка → Установить новое ПО»; обновляет комплектный v8-cs на месте.
- Release **0.8.0**: zip обоих профилей (`v8codestyle-edt2026.1/2026.2-0.8.0.zip`).
- Workflow'и: `release.yml` (тег X.Y.Z → сборка обоих профилей **с ретраями
  ×3** — edt.1c.ru рвёт отдачу natives.library раннерам GitHub → GitHub
  Release) и `deploy-update-site.yml` (release/тег/dispatch → сборка → Pages).
- **ХВОСТ**: теговые деплои в среду `github-pages` отклоняются protection
  rules. Через API добавляется только branch-политика (`0.*`, добавлена);
  теговые правила — только в UI: Settings → Environments → github-pages →
  Deployment branches and tags → Add deployment tag or branch rule → Tags →
  `0.*`. После этого сайт обновляется из релиза автоматически (сейчас сайт
  публикуется dispatch'ем с master — не блокирует).
- Апстримные `ci-build.yml`/`fork-pull.yml` на форке ОТКЛЮЧЕНЫ (не адаптированы
  под наш пайплайн); апстримный `release.yml` ЗАМЕНЁН нашим — дифф форка.
- Версия таргет-артефакта в bom/tests — `${project.version}` (не пинить:
  после релизного `set-version` резолв таргета ломается).
- Прочие грабли CI: «зомби»-прогоны (in_progress навечно) — cancel + rerun;
  `pgrep -f` в inline-командах с путями стенда — самострел, пути собирать
  конкатенацией.

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
