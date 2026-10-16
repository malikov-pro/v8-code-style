---
name: v8cs-bsl-binding
description: Типовые и межмодульные BSL-проверки EDT: binding, экспортные сигнатуры, адресный пересчёт зависимостей и исследование штатного покрытия.
---

# Binding и инкрементальность BSL

Использовать для checks, зависящих от сигнатуры, типа receiver или другого
модуля. Это дополнение к `v8cs-port-ls`, а не разрешение менять контракт LS.

## Сначала доказательства

1. Прочитать pinned LS-код, параметры, defaults и корпус. Идентичный текст
   обращения не доказывает тип/владельца. Сначала native probe на своём объекте,
   затем положительные и отрицательные случаи в настоящем check context.
2. Найти рабочий пример в репозитории; проверить javap и экспорт пакетов именно
   целевого профиля. EClass индексов лежат в `BslIndexPackage`, не `BslPackage`.
3. Research отвечает на один вопрос. Если результат не изменился, собирать
   фактические binding/URI/signature, а не подряд менять точки запуска чека.

## Разрешение пользовательского метода

- Локальные `StaticFeatureAccess.getFeatureEntries()` содержат связанные объекты.
- Для динамического вызова использовать actual `Environmental.environments()`
  и проверенный `DynamicFeatureAccessComputer.resolveObject(...)`.
- Экспортный результат может быть `BslContextDefMockMethod`, а не BSL `Method`.
  Он реализует `SourceObjectLinkProvider`, `getSourceUri()` указывает на исходник.
  Такой mock может иметь `eResource()==null`: resolve относительно него оставляет
  proxy. В рабочем check context сигнатуру читать по URI через текущую
  BM-транзакцию: `modelManager.getModel(call).getEngine().getCurrentTransaction()`
  → `getExternalObjectByUri(uri)`. Пример: `MissedRequiredParameterCheck`.
- Не заменять неразрешённую ссылку поиском по имени. Null/proxy/несколько разных
  методов — недоказанная сигнатура, а не основание выбрать первый.
- Не считать обычный BM-task равнозначным контексту валидации для TypesComputer.

## Пересчёт: обязательная отдельная регрессия

- Поставщик required→optional→required; потребитель не редактируется. Проверять
  конкретные lines/ranges/messages, stamp и содержимое потребителя.
- Отдельно менять позиции обязательности при прежнем minParams: индекс общего
  числа аргументов не описывает обязательность каждой позиции.
- Если маркер завис: сначала проверить актуальные данные поставщика. Диагностический
  ручной перезапуск чека допустим только ПОСЛЕ сохранения автоматического результата;
  тест всё равно падает по исходному результату. Off/on, clean, touch потребителя
  не превращать в приёмку инкрементальности. Удалять research-ветку до итогового гейта.

## Публичный BM-collector для экспортной сигнатуры

`ExportMethodProvider` заменяет `ModuleContextDefIndex.methods` целиком.
Подписка только на `ContextDefMethodInfo` не поймала required→optional.
Рабочий путь — `addGenericModelFeatureChangeContextCollector` для
`BslIndexPackage.Literals.MODULE_CONTEXT_DEF_INDEX`, фильтр
`MODULE_CONTEXT_DEF_INDEX__METHODS`, затем `session.addLanguageCheck(path)`
для доказанных индексом потребителей. Пример в `MissedRequiredParameterCheck`.

- `setLanguageResourceChangeContextCollector` в исследованном EDT2026.1/check6.0.101
  столкнулся с MODULE language collector: ClassCastException при init проектов.
  Этот путь отклонён; внутренние cast/reflection обходы не добавлять.
- События не гарантируют мутацию каждого вложенного элемента. Проверять фактический
  event/top-object до проектирования подписок; лишние подписки/println удалять.
- Текущая реализация линейно просматривает описания и ссылки того же проекта,
  но планирует только найденных потребителей, дедуплицируя paths. Это не O(1)
  и не доказательство межпроектного scope или производительности большой базы.
- Не переносить collector всем чекам автоматически: существующая типовая проверка
  может уже пересчитываться штатно. Новый механизм — только по красной регрессии.

## Границы доказательства

- Native coverage классифицировать по подслучаю. Strict annotation, compatibility,
  unknown/union/metadata receiver и недостающие platform signatures — разные случаи.
- FAIL детекции отличать от BLOCKED стартом тестового проекта/компиляцией/средой.
- SQLite: fixture и snapshot hashes, точный testcase XML, неизменяемая история
  FAIL→PASS. Общий зелёный gate не сертифицирует каждый LS-подслучай.
- Live MCP показывает count/line/message, но не точный offset/length и не GUI-карточку.
  Точные диапазоны доказывать itests отдельно. Сохранять аномалии старта и проверять
  повторяемость; отсутствие собственного пакета в стеке не доказывает чужую причину.

## Референсы

- `bundles/com.e1c.v8codestyle.bsl/src/com/e1c/v8codestyle/bsl/check/MissedRequiredParameterCheck.java`
- `tests/com.e1c.v8codestyle.bsl.itests/src/com/e1c/v8codestyle/bsl/check/itests/MissedRequiredParameterCrossModuleTest.java`
- `../_notes/WORK-2026-10-15-v8cs-required-empty.md`
- `../_notes/WORK-2026-10-15-v8cs-native-type-coverage.md`
