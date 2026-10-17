---
name: v8cs-bsl-binding
description: Типовые и межмодульные BSL-проверки EDT: binding, экспортные сигнатуры, адресный пересчёт зависимостей и исследование штатного покрытия.
---

# Binding и инкрементальность BSL

Использовать для checks, зависящих от сигнатуры, типа receiver или другого
модуля. Это дополнение к `v8cs-port-ls`, а не разрешение менять контракт LS.

## Сначала доказательства

Native-инвариант устанавливается маркером EDT, не отказом MCP в записи.
`modify_metadata` может запретить nonexport/nonserver/missing handler ещё до
изменения модели. Для пробы создать валидное назначение, затем изменить своего
поставщика или собственный metadata-файл; подтвердить фактическую модель и
маркеры после завершения валидации. Guard инструмента записывать отдельно.
Pinned-код приоритетнее формулировки: Web handler message «функция» не означает
проверку Function, а subscription rule не проверяет параметры. Native-покрытие
пустоты метода не сертифицирует всю LS ReferenceIndex-семантику пустого тела.

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

- QL параметры: прямой delegate-test с новыми ICheckParameters не доказывает
  автоматический пересчёт потребителя. TOP skipSelectTopOne true→false→true
  в BSL пересчитывается штатно; СКД на EDT2026.1 требовала отдельного планирования.
  Проверенный публичный ICheckScheduler требует BOTH shortUID: dcs-ql-hub и
  delegate, плюс реальные DCS container IDs. Только hub UID отфильтровал делегат.
  Product listener реагирует на settingsChanged TOP, не меняет query/settings,
  уважает disabled hub/check; тестовый listener после research удалить и повторить
  исходную строгую регрессию. Новые DI/lifecycle/пакеты требуют полного2026.1.
- QL UNION: enum UNION имеет и первый SELECT без объединения; использовать
  flagUnionType && !flagUnionTypeAll, а keyword брать из owned semantic leaf.
  QL keyword-range и BSL строка с продолжением `|` различаются: для `|UNION`
  native bridge вернул length6, не keyword length5. Не «чинить» это угадыванием.
- HTTP/Web metadata checks: изменение BSL не обязано менять HTTPService.module
  или WebService.module. Для неэкспортного HTTP-метода проверено событие
  `ModuleContextDefIndex.contextDef`, не только methods: URI из BslContextDef
  → текущая BM-транзакция → Module.owner → metadata-check только своего сервиса.
  Не обобщать на все события Module без proof: такой research имел startup
  BLOCKED, причинность не установлена. Удалять исследовательские подписки.
- Маркеры на HTTP Method.handler / Web Operation.procedureName вложены в
  metadata-владельца. Owner-only getMarkers пропускал их: читать getNestedMarkers
  с фильтром CheckUid, проверять feature ID и отдельный markerObjectId каждого
  назначения. Ошибка наблюдения теста не является дефектом детектора.
- SingleProjectReadOnlyCheckTestBase оставляет проект, но очищает ThreadLocal
  между классами. Несколько классов одной fixture должны брать существующий
  IDtProject через dtProjectManager, а не повторять import (resource already exists).
  Это проверять совместным/канальным прогоном, не только каждым классом отдельно.
- ScheduledJob metadata check: CommonModule.contextDef может существовать, но
  allMethods() пуст. Это не доказательство нулевой арности. Проверенный путь —
  module.getModule(), при proxy текущая BM-транзакция.getExternalObjectByUri,
  затем реальный AST.allMethods()/formalParams. Требуется явный bsl.model import;
  новую metadata→BSL зависимость закрывать полным 2026.1, не только targeted.
- Дубли заданий: BM methodName-событие содержит старое значение в публичном
  BmChangeEvent.getOldValues(), можно пересчитать старую/новую группы. Файловая
  загрузка в проверенной EDT присылает только schedule/suppressObject без старого
  обработчика. Проверенный fallback — пересчёт набора ScheduledJob проекта по
  derived schedule, без кеша назначений. Это не адресный group lookup и может
  стоить O(jobs²) вместе с детектором; явно фиксировать ограничение performance.
  Проверять неизменного владельца маркера при правке ДРУГОГО задания и файловый
  сценарий отдельно; self-rebind тест пропускал этот дефект.
- Metadata handler строка `CommonModule.Module.Method` — квалифицированная
  ссылка, не произвольное BSL-имя. Для module flags разрешать canonical FQN
  через текущую BM-транзакцию, пропускать malformed/missing/ambiguous объект;
  RU type-prefix канонизировать отдельно, не искать метод по тексту модуля.
  `IBmObject.bmGetTransaction()` доступен в metadata check без нового DI-сервиса.
- В subscription server-flag regression загрузка CommonModule.mdo не прислала
  `COMMON_MODULE__SERVER`, но обновила `COMMON_MODULE__CONTEXT_DEF`. Generic
  collector этих двух событий планирует model-check только ссылающихся подписок.
  BM-флаг и file-only provider change тестировать раздельно, stamp/bytes
  потребителя не менять. Обход подписок линейный: это адресное планирование,
  не индексный O(1) lookup и не доказанный benchmark большой конфигурации.
- Назначенные события формы: `EventHandler.event` — объект платформенного Event,
  не строка; в fixture безопасно сериализовать OnOpen, чтобы ссылка разрешилась
  штатным parser. `BslEventsService.getAllModuleEvents` возвращает назначения
  только корня формы (не все элементы/команды). Диапазон брать из ParamSet,
  OnOpen в проверенной платформе 0..1, BeforeClose 0..4: не фиксировать точную
  арность по числу привычных имён параметров. Defaults самого обработчика не
  уменьшают его declared count. Для общих обработчиков нескольких событий
  проверять каждый контракт отдельно; scope относительно LS описывать явно.
- Изменение `EventHandler.name/event` и файловый rebind — разные события.
  В regression загрузка Form.form не вызвала handler.name, но обновила
  `MdClassPackage.ABSTRACT_FORM__FORM_EVENTS`. Проверенный collector этого события
  планирует только BSL-модуль своей формы. Событие/имя меняются без touch модуля;
  внешняя файловая regression нужна отдельно от BM-транзакции.
- Таблицы формы: TypesComputer даёт общий FormTable. Для scope DynamicList
  связывать обращение через DerivedProperty.source с конкретным Table, затем
  проверять актуальный источник данных и valueType реквизита, не имя элемента.
  IDataSourceInfoAssociationService требует package import и отдельного
  `bind(...).toService()` в BSL ExternalDependenciesModule (пример form-бандла).
- После изменения существующего dataPath.segments коллекция dataPath.objects
  может ещё описывать старую привязку; isRelatedDynamicList доверяет ей. Проверенный
  путь — findPropertyInfo(form, path, 0) по текущим сегментам → фактический
  FormAttribute → extInfo и valueType. Это не универсальное правило для nested
  путей: сертифицировать нужный scope, не разбирать имена вручную.
- Form-only регрессии разделять: замена пути, изменение сегмента того же пути,
  вид таблицы и вложенный TypeDescription.types. Collector с top-filter FORM
  нужен на конкретных Table/FormAttribute/DataPath/TypeDescription; одной
  подписки на замену dataPath/valueType недостаточно для вложенной мутации.
  Readiness/live гейты обязательны и для новой wiring-зависимости; targeted
  Guice MissingImplementation — BLOCKED, не FAIL детекции.
- BM-мутация и синхронизация Form.form с диска — разные сценарии. CurrentRowAccess
  прошёл полные 7 модулей, но live-файловая смена пути оставила маркеры:
  EDT обновила модель без события segments/dataPath, затем прислала изменение
  `AbstractDataPath.objects`. Нужна отдельная файловая regression и адресный
  пересчёт по этому событию. Association-подписка сама по себе дефект не исправила.
  Если live расходится с зелёными itests, остановить merge, добавить red, повторить
  gate и live новым артефактом. Clean/ручной revalidate не заменяют пересчёт.
- Менеджер метаданных: в настоящем check context TypesComputer возвращает
  TypeItem с containment к фактическому MdObject (пример CatalogManager.Products,
  URI bm://Project/Catalog.Products#/producedTypes/managerType/type). Проверять
  resolved owner + `McoreUtil.getTypeCategory`, не извлекать имя объекта из текста.
  Класс MdManagerType виден через javap, но без объявленного package import
  компилятор таргета отвергает доступ: javap не доказывает classpath/API-контракт.
  Не подавлять restriction вместо проверки MANIFEST/публичной альтернативы.
- CheckUnique/CodeSeries не гарантируют штатную перевалидацию BSL-потребителя.
  В UnsafeFindByCode подтверждён generic BM feature collector на трёх владельцах:
  persisted `IResourceDescription.getImportedNames()` содержит managerType;
  по этому индексу планируются использующие тип модули того же проекта.
  Initial-unsafe→sourceupdate и metadata-only cycle проверять отдельно;
  alias-scope определять pinned LS-кодом, а не расширять только потому, что EDT
  умеет вывести тип переменной. Текущий индексный обход линейный, не benchmark.
- Платформенное script-имя не всегда имя Type: для `ErrorCategory` подтверждён
  владелец `EnumErrorCategory` / `ПеречислениеКатегорияОшибки`, sysEnum=true.
  Связанный member — Property внутри ContextDef этого Type. Не выбирать имя
  владельца из документации вместо исследования реального check context.
- У parenthesized DynamicFeatureAccess `getNode` может вернуть только внутренний
  доступ, без скобок. Для LS last-token guard исследовать `findActualNodeFor`:
  в BadExceptionCategory он сохраняет закрывающую скобку и исключение срабатывает.
  Это не универсальная замена getNode (см. QL-ловушки в AGENTS).
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
