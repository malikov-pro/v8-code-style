[![Релиз](https://github.com/malikov-pro/v8-code-style/actions/workflows/release.yml/badge.svg)](https://github.com/malikov-pro/v8-code-style/actions/workflows/release.yml)
[![Update Site](https://github.com/malikov-pro/v8-code-style/actions/workflows/deploy-update-site.yml/badge.svg)](https://github.com/malikov-pro/v8-code-style/actions/workflows/deploy-update-site.yml)
[![Версия](https://img.shields.io/github/v/release/malikov-pro/v8-code-style)](https://github.com/malikov-pro/v8-code-style/releases)

# 1С:Стандарты разработки V8

Расширение для 1C:EDT, которое помогает разрабатывать конфигурации/приложения по стандартам 1С для платформы "1С:Предприятие 8".

## Основные возможности

- [Проверки кода и метаданных](docs/checks/readme.md) по [стандартам 1С](https://its.1c.ru/db/v8std)
   - [Проверки метаданных](docs/checks/md.md)
   - [Проверки Форм](docs/checks/form.md)
   - [Проверки прав ролей](docs/checks/right.md)
   - [Проверки модулей](docs/checks/bsl.md)
   - [Проверки языка запросов](docs/checks/ql.md)
- Дополнительные инструменты, улучшающие и ускоряющие работу по стандартам 1С
   - [Авто-сортировка метаданных](docs/tools/autosort.md)
   - [Создание общих модулей по типам](docs/tools/common-module-types.md)
   - [Панель "Bsl Документирующий комментарий"](docs/tools/bsl-doc-comment-view.md)
   - [Автоматическое создание структуры модуля](docs/tools/module-structure.md)



## Установка

> **Внимание!** Расширение включается в дистрибутив `1C:EDT`. Текущая ветка форка поддерживает только **EDT 2026.2.2 / Java 25 / Linux GTK x86_64**. Установка форка **обновляет комплектную копию на месте** (те же IU-имена). Совместимость старых опубликованных релизов смотрите в их release notes; текущая ветка не означает, что update site уже обновлён.

Сборка: `bash compile.sh`, из публичных источников, без приватного архива EDT. SHA-256 lock и требования CI — [docs/build-edt-2026.2.2.md](docs/build-edt-2026.2.2.md).

### Из update site форка (рекомендуется)

Репозиторий обновляется автоматически при выпуске релиза форка:

| Версия | 1C:EDT | Update site |
|--------|--------|-------------|
| Опубликованные версии — по release notes | Совместимость конкретного релиза | `https://malikov-pro.github.io/v8-code-style/` |

Установка: `Справка – Установить новое ПО` → `Add…` → вставить URL → отметить компонент `1C:Code style V8` → `Next >` → принять лицензию → `Finish` → перезапустить EDT.

Последующие обновления: `Справка – Проверить обновления` (сайт должен оставаться в списке «Доступных сайтов обновления»).

Проверка установленного: `Справка – О платформе – Установленное ПО` → `com.e1c.v8codestyle.feature` с квалификатором вида `0.8.0.v<дата-время>` (свежее комплектного).

### Из zip релиза

Архивы — в [релизах](https://github.com/malikov-pro/v8-code-style/releases).
Для новых релизов этой ветки предусмотрен один архив
`v8codestyle-edt2026.2.2-X.Y.Z.zip`. Старые архивы сохраняют прежние имена
и требования; новые бинарники с JavaSE-25 на Java 17 не запускаются.

Установка: `Справка – Установить новое ПО` → `Add…` → `Archive…` → выбрать скачанный zip (флажок «Обращаться во время инсталляции ко всем сайтам…» можно снять) → далее как выше.

### Требования к Java

| Профиль | 1C:EDT | JVM | Примечание |
|---------|--------|-----|------------|
| default (алиас edt-2026.2) | 2026.2.2 | **полная Java 25**: JDK с JavaFX и jshell (например, Axiom `axiomjdk-java25-pro-full`) | На неполной JVM (JRE или без JavaFX) ломаются карточки замечаний **всех** провайдеров — их строит AI-плагин EDT. Запуск: `1cedt -vm <путь-к-java25>/bin/java -data <воркспейс>` |

### Откат к комплектной версии

Деплой форка заменяет комплектную копию (те же IU-имена). Откат выполняется восстановлением комплектной версии из дистрибутива EDT (или переустановкой EDT); «Проверить обновления» комплектную версию не вернёт.

### Официальные поставки 1С (апстрим)

Официальные p2-репозитории апстрима [1C-Company/v8-code-style](https://github.com/1C-Company/v8-code-style) — см. таблицу ниже и [релизы апстрима](https://github.com/1C-Company/v8-code-style/releases). Форк собирается независимо; синхронизация с апстримом не выполняется.

| Версия | 1C:EDT | P2-репозиторий |
|--------|--------|----------------|
| 0.8.0  | 2026.2 | [p2-link](https://edt.1c.ru/downloads/releases/plugins/v8-code-style/edt-2026.2/0.8.0/repo/), [p2-zip](https://edt.1c.ru/downloads/releases/plugins/v8-code-style/edt-2026.2/0.8.0/repo.zip) |
| 0.7.0  | 2023.3 | [p2-link](https://edt.1c.ru/downloads/releases/plugins/v8-code-style/edt-2023.3/0.7.0/repo/), [p2-zip](https://edt.1c.ru/downloads/releases/plugins/v8-code-style/edt-2023.3/0.7.0/repo.zip) |
| 0.6.0  | 2023.2 | [p2-link](https://edt.1c.ru/downloads/releases/plugins/v8-code-style/edt-2023.2/0.6.0/repo/), [p2-zip](https://edt.1c.ru/downloads/releases/plugins/v8-code-style/edt-2023.2/0.6.0/repo.zip) |
| 0.5.0  | 2023.1 | [p2-link](https://edt.1c.ru/downloads/releases/plugins/v8-code-style/edt-2023.1/0.5.0/repo/), [p2-zip](https://edt.1c.ru/downloads/releases/plugins/v8-code-style/edt-2023.1/0.5.0/repo.zip) |
| 0.4.0  | 2022.2 | [p2-link](https://edt.1c.ru/downloads/releases/plugins/v8-code-style/edt-2022.2/0.4.0/repo/), [p2-zip](https://edt.1c.ru/downloads/releases/plugins/v8-code-style/edt-2022.2/0.4.0/repo.zip) |
| 0.3.0  | 2022.1 | [p2-link](https://edt.1c.ru/downloads/releases/plugins/v8-code-style/edt-2022.1/0.3.0/repo/), [p2-zip](https://edt.1c.ru/downloads/releases/plugins/v8-code-style/edt-2022.1/0.3.0/repo.zip) |
| 0.2.0  | 2021.3 | [p2-link](https://edt.1c.ru/downloads/releases/plugins/v8-code-style/edt-2021.3/0.2.0/repo/), [p2-zip](https://edt.1c.ru/downloads/releases/plugins/v8-code-style/edt-2021.3/0.2.0/repo.zip) |
| 0.1.0  | 2021.2 | [p2-link](https://edt.1c.ru/downloads/releases/plugins/v8-code-style/edt-2021.2/0.1.0/repo/), [p2-zip](https://edt.1c.ru/downloads/releases/plugins/v8-code-style/edt-2021.2/0.1.0/repo.zip) |


## Участие в проекте

Добро пожаловать! Правила — [CONTRIBUTING.md](CONTRIBUTING.md) (унаследованы от апстрима).
- [Добавить свою проверку](docs/contributing/readme.md) — туториал и [соглашение](docs/contributing/Check_Convention.md)
- [Помочь с документацией](docs/contributing/documentation.md)
- Сообщить нам о [ложном срабатывании проверки](https://github.com/malikov-pro/v8-code-style/issues/new?template=check_false.md&title=Ложное+срабатывание+проверки%3A+%3Cкод+проверки%3E) или о [не нахождении существующей ошибки](https://github.com/malikov-pro/v8-code-style/issues/new?template=check_not_found.md&title=Проверка%3A+%3Cкод+проверки%3E+не+находит+ошибку).

> Вопросы апстриму (официальной версии расширения) — в трекер [1C-Company/v8-code-style](https://github.com/1C-Company/v8-code-style/issues). Синхронизация форка с апстримом не выполняется.


## Лицензия

[Лицензирование расширений размещенных в данном проекте осуществляется на условиях свободной (открытой) лицензии Eclipse Public License - v 2.0](docs/contributing/licensing.md) (полный текст лицензии - https://www.eclipse.org/legal/epl-2.0/)
