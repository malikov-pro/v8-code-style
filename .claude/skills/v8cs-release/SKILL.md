---
name: v8cs-release
description: Релиз и доставка форка v8-code-style — EDT2026.2.2, тег X.Y.Z, публичный p2 и metadata lock, GitHub Release/Pages, защита среды github-pages.
---

# Релиз и доставка

## Что куда доставляется

- **Сайт**: https://malikov-pro.github.io/v8-code-style/ — p2 + index.html
  с квалификатором. В EDT: «Справка – Установить новое ПО» → URL; обновляет
  комплектную копию на месте.
- **GitHub Release**: один `v8codestyle-edt2026.2.2-X.Y.Z.zip`.
- **Платформа:** публичный target с точными roots и public-platform-lock.json.
  До Maven — verify-public-platform.py, checksum артефактов — при загрузке Tycho.
  Приватный archive/secret не нужны. Docs: docs/build-edt-2026.2.2.md.
  Бинарники EDT публично не публиковать; cold GitHub pass доказывать отдельно.

## Процедура релиза

Релиз/изменение master/тега/публикация — только по явной команде пользователя.
Зелёная мини-пачка develop не является разрешением выпустить версию.

1. Стабильный release-кандидат: версия `X.Y.Z-SNAPSHOT`, **полный compile.sh
   EDT2026.2.2**, XML результаты всех modules и own live smoke сохранены.
   После зелёного develop merge в master под согласованный релиз; сверить
   соответствие исходников кандидату. CI без тестов не заменяет локальный гейт.
2. Пуш тега `X.Y.Z` в master → `release.yml`: сборка **EDT2026.2.2**
   canonical compile.sh --skip-tests → `gh release create` с одним zip.
3. Публикация релиза дёргает `deploy-update-site.yml` (дублирован триггером
   по тегу — release-событие от GITHUB_TOKEN не триггерит другие workflow)
   → Pages обновляется сборкой из тега.
4. Проверка: `gh release view X.Y.Z` (один zip), сайт отдаёт квалификатор
   теговой сборки, `curl .../artifacts.xml.xz` = 200.

## Грабли (всё уже происходило)

| Симптом | Причина / решение |
|---|---|
| `Tag "X.Y.Z" is not allowed to deploy to github-pages` | Защита среды: теговые правила добавляются **только в UI** (Settings → Environments → github-pages → Deployment branches and tags → Tags → `0.*`). В API есть только branch-политики — они теги не покрывают |
| «workflow file issue», прогон падает за 0–3 с | Файл не принят парсером GitHub: pyYAML не валидатор. Прецедент — `ref:` на уровне шага вместо `with:`. Сверять структуру с рабочим файлом |
| Резолв таргет-артефакта `…:target:X.Y.Z-SNAPSHOT` не найден после «Версия из тега» | Пинить нечего: в bom/tests должно быть `${project.version}` (set-version снимает SNAPSHOT у всех модулей) |
| CI `--full`/SDK: source-feature version matching старому релизу не найден | Generated `com.e1c.v8codestyle.feature.source` не является reactor-проектом, tycho-versions не обновляет ссылку SDK. В sdk.feature includes source-feature держать version=0.0.0 (текущая доступная source-фича); release/Pages без SDK это не ловят |
| `Premature EOF` на `natives.library.*` с edt.1c.ru | Сетевой обрыв отличать от checksum mismatch. Источники публичные; private snapshot не fallback. Изменение metadata/SHA-256 не маскировать ретраями или автоматическим обновлением lock |
| `Invalid Collect Request: null` на первом eclipse-plugin, вложенные `lib/*.jar` в списке | Maven 3.10/Resolver 2 отвергает classifier с `/`, инжектируемый Tycho 5.0.2 (eclipse-tycho/tycho#6399). Доказано A/B на теге 0.9.0: Maven 3.10 — failure, 3.9.9 — success. CI фиксирует Maven 3.9.9; чистка кэша и ретраи это не исправляют |
| Исправлен workflow, но rerun тега по-прежнему падает | Rerun использует старый workflow. После отдельного разрешения на CI-правку master: `gh workflow run release.yml --ref master -f tag=X.Y.Z`, затем `gh workflow run deploy-update-site.yml --ref master -f tag=X.Y.Z`. Оба checkout'ят существующий тег, не HEAD master; тег не пересоздавать |
| Прогон «in_progress» часами, счётчик замер | «Зомби»-раннер: `gh run cancel` → `gh run rerun` (rerun может проигнорировать свежую защиту среды — лучше новый dispatch/тег) |

## Инфраструктура Pages

- Pages включён (`build_type=workflow`), environment `github-pages`:
  правила `0.*` и для branch, и для **Tags** (UI; в API теговых политик нет —
  branch-политика теги не покрывала).
- Сайт можно перепубликовать без релиза: `gh workflow run
  deploy-update-site.yml` (dispatch собирает master HEAD), по отдельному
  разрешению. Существующий опубликованный тег не пересоздавать: для
  восстановления доставки использовать поддержанный workflow input tag.
- `ci-build.yml`/`fork-pull.yml` по API GitHub 28.10 активны; перед push
  проверять triggers, не полагаться на старые записи об отключении.
  Апстримный `release.yml` заменён нашим — это дифф форка (см. AGENTS.md).
