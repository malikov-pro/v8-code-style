---
name: v8cs-release
description: Релиз и доставка форка v8-code-style — тег X.Y.Z, release.yml (оба профиля, GitHub Release), deploy-update-site.yml (GitHub Pages), защита среды github-pages, зомби-прогоны. Использовать при выпуске версии, проблемах Actions или обновлении сайта.
---

# Релиз и доставка

## Что куда доставляется

- **Сайт**: https://malikov-pro.github.io/v8-code-style/ — p2 + index.html
  с квалификатором. В EDT: «Справка – Установить новое ПО» → URL; обновляет
  комплектную копию на месте.
- **GitHub Release**: zip обоих профилей (`v8codestyle-edt2026.1/-edt2026.2-X.Y.Z.zip`).

## Процедура релиза

1. Убедиться: версия в pom — `X.Y.Z-SNAPSHOT`, master зелёный (полный itests
   прогон локально, ~35 мин — CI тесты не гоняет).
2. Пуш тега `X.Y.Z` в master → `release.yml`: сборка **обоих профилей**
   (без тестов, ретраи ×3) → `gh release create` с двумя zip.
3. Публикация релиза дёргает `deploy-update-site.yml` (дублирован триггером
   по тегу — release-событие от GITHUB_TOKEN не триггерит другие workflow)
   → Pages обновляется сборкой из тега.
4. Проверка: `gh release view X.Y.Z` (оба zip), сайт отдаёт квалификатор
   теговой сборки, `curl .../artifacts.xml.xz` = 200.

## Грабли (всё уже происходило)

| Симптом | Причина / решение |
|---|---|
| `Tag "X.Y.Z" is not allowed to deploy to github-pages` | Защита среды: теговые правила добавляются **только в UI** (Settings → Environments → github-pages → Deployment branches and tags → Tags → `0.*`). В API есть только branch-политики — они теги не покрывают |
| «workflow file issue», прогон падает за 0–3 с | Файл не принят парсером GitHub: pyYAML не валидатор. Прецедент — `ref:` на уровне шага вместо `with:`. Сверять структуру с рабочим файлом |
| Резолв таргет-артефакта `…:target:X.Y.Z-SNAPSHOT` не найден после «Версия из тега» | Пинить нечего: в bom/tests должно быть `${project.version}` (set-version снимает SNAPSHOT у всех модулей) |
| `Premature EOF` на `natives.library.*` с edt.1c.ru | Флак сети 1С→раннеры; в release.yml ретраи ×3 на каждый профиль. Локально — просто перезапустить |
| Прогон «in_progress» часами, счётчик замер | «Зомби»-раннер: `gh run cancel` → `gh run rerun` (rerun может проигнорировать свежую защиту среды — лучше новый dispatch/тег) |

## Инфраструктура Pages

- Pages включён (`build_type=workflow`), environment `github-pages`:
  правила `0.*` и для branch, и для **Tags** (UI; в API теговых политик нет —
  branch-политика теги не покрывала).
- Сайт можно перепубликовать без релиза: `gh workflow run
  deploy-update-site.yml` (dispatch собирает master HEAD).
- Апстримные `ci-build.yml`/`fork-pull.yml` отключены, апстримный
  `release.yml` заменён нашим — это дифф форка (см. AGENTS.md).
