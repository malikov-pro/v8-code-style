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

- CFG-волна (EMF vs pinned): EDT вставляет EmptyStatement-терминаторы —
  фильтровать в блоках; `PreprocessorConditionVertex` LS не ConditionalVertex —
  в классификаторе LS падает в default и флагает — наш preprocessor-Conditional
  всегда flag; препроцессор уровня метода живёт в IfPreprocessorDeclareStatement
  внутри `Block.getDeclareStatements()` (builder: declares+statements);
  goto/метки на уровне ТЕЛА модуля грамматикой EDT не принимаются (гэпов в теле
  не существует); after-Break/Continue LS не флагает вовсе — вариант B это
  осознанное расширение (сироты с тегом причины сохраняются при финализации).
  Эталон снимать дампом pinned-движка (Scoped*Test в pinned-ls-build), asserts
  корпусов — истина, комментарии — нет.
- UnavailableMemberCall scoped residual: версии появления брать из pinned
  builtin-platform-types (ErrorCategory — 28 записей, exact-audit против JSON),
  не из имен. Guard: resolved platform Property + URI плагина platform_v* +
  sysEnum-владелец EnumErrorCategory; неразрешённые/Structure-поля skip;
  compatibilityMode null = latest (skip). Native undefined globals/ctors
  не дублировать; произвольные члены без источника версий не переносить.
- Live после свежего старта EDT: внешние записи Configuration.mdo/Module.bsl
  подхватываются с задержкой (догрузка воркспейса + refresh). Перед чтением
  маркеров подтверждать фактическую BM-модель (get_configuration_properties);
  ранние reads в период загрузки — stale, сохранять как историю и перечитывать
  отдельным `-confirmed` ярлыком, а не форсировать revalidate/clean между
  переходами. В спокойной сессии подхват занимал секунды.
- DeprecatedAttributes8312 pinned — лексический safety-net, не типизированный
  deprecated Property: root Chart/ChartPlotArea (RU/EN), alias другого имени skip,
  Chart=Structure может сработать. При переносе не «улучшать» контракт молча.
  Сверять валидные chain/index/receiver-call и AccessCall отдельно от AccessProperty.
  C-style /* */ невалидны в EDT: прямой LS-run может дать recovery-маркеры;
  сначала syntax assertions, затем сравнение диапазонов. Проверенный valid corpus
  совпал по9строкам, dot+property ranges включают whitespace после точки.
  Native current-runtime удалённые enum-ветки не дублировать; это не доказательство
  покрытия на старом runtime/других execution contexts. Совместимость >=8.3.12
  и BM/file-only пересчёт проверены без новых collector; исходник потребителя не менять.
- Платформенные версии: `DT-INF/PROJECT.PMF Runtime-Version` выбирает модель
  platform_v8.x, `Configuration.compatibilityMode` — отдельная настройка. Меняя
  только compatibilityMode, нельзя доказать смену каталога типов/методов EDT.
  Fixture runtime должен входить в testing platform versions (на исследованном
  2026.1:8.3.19/8.3.23/8.5.1). Для enumcompat8.1 сериализовать **8.1.0**:
  недопустимое8.1 превращалось в defaultlatest. Сначала assert фактической BM-модели.
- Deprecated platform properties: actual `DynamicFeatureAccessComputer.resolveObject`
  в validation context даёт Property с VersionedItem.deprecatedSince/replacedBy.
  Данные могут отсутствовать в старой platformmodel: PrintParametersName в8.3.19
  безdeprecated, в8.3.23/27 deprecated8.2.9. URI платформы отличает такой Property
  от одноимённого динамического поля Structure. Union проверять на syntax-clean
  модуле: modulebody передmethods оставлял recoveryAST без вызова нужного контекста.
  Native уже диагностирует obsoleteglobalmethods и NSSconstructedtype; не дублировать.
  BM+filecompat-only пересчёт проверен штатный, лишний collector не нужен.
- Тестовые проекты должны содержать .settings: без каталога applyChanges падал
  ProfileStorageException при записи Default.cset; это дефект fixture, не детектора.
- LocalMCP алиас не доказывает стенд: после перенастройки edt_rt_test оказался
  WindowsРТ, localapk доступен отдельным127.0.0.1 endpoint. Проверять version+
  list_projects paths перед записью; не менять remote userprojects. Токен изprefs
  читать только в память, не включать в evidence/команды/коммиты.
- События элементов форм: `BslEventsService.getAllModuleEvents` даёт только
  root-form назначения. В `getEventHandlers` есть элементы/extInfo/command, но
  ParamSet события элемента НЕ содержит первый `Элемент` (к min/max добавить1),
  а synthetic Action имеет1 Parameter при min/max0: base command требует ровно1.
  Считать по фактическому binding, не имени события; root-derived handlers могут
  быть вложены в форму, но не имеют FormItem ancestor — им offset1 не добавлять.
  Версия совместимости меняет контракт (StartChoice8.3.21 max2 event args,
  live8.3.27 max3). Собственные formal defaults не уменьшают declared count.
- Инкрементальность формы проверять раздельно: item name, extInfo name, command
  name, замена action, состав items и Form.form. Общий BM-cycle скрывал stale
  command за пересчётом соседнего item. Доказанные collectors — CommandHandler.name
  и FORM-scoped membership; при добавлении второго item без collector маркер
  первого исчезал, а check context не вызывался. Не лечить touch/clean.
  Повторные назначения одного Event нельзя схлопывать Map<Event,name>.
  Требование всем известным назначениям строже pinned LS first-by-name: это
  прежняя политика partial check, не полная LS-эквивалентность.
- Startup Unknown check id после upgrade воспроизведён на установленном
  check6.0.101: ProjectCheckMetadataManager.addUpdatedContributorToDelta при смене
  contributor version считает updated, но пропускает added из-за else. Same-version
  addition/new-contributor контроли проходят. Research disconnected managers и
  immutable FAIL сохранены в `_notes/evidence/startup-check-registry-20261019/`.
  Это PLATFORM, не разрешение patch/reflection/unregister в продукт; следующий
  restart объясняет симптом, но не исправляет алгоритм. Полный UI upgrade/2026.2
  не проверены. Не обобщать этот диагноз на любой сбой запуска.
- QL ВТ: resolved DbViewSelectDef покрывает обычные ВТ/FilterCriterion, но
  бухгалтерская Субконто — DbViewTableDef под EXT_DIMENSIONS_VIEW. В JOIN она
  входит в LS-контракт, в empty-params — исключена. Не подменять видом по имени.
  Пустой QuerySchemaTableParameter имеет QuerySchemaExpression с EmptyExpression,
  а не обязательно null; проверять содержимое и DCS-expression список.
- Provider-only ВТ: InformationRegister.dbViewDefs приходит после изменения
  периодичности и файловой загрузки. QL delegate generic BM-collector приводил
  к повторяемым startup BLOCKED (причина не установлена); этот путь удалён.
  Рабочий путь — lifecycle BM async listener по образцу SortService, затем
  ICheckScheduler с хабом И обоими делегатами. СКД-кандидаты читаются заново,
  binding к регистру искать также через parentTable: SliceLast уже может быть
  Unresolved, а физический родитель ещё разрешён. BSL fallback перепланирует
  модули текущего проекта; это не адресный reference index, benchmark не выполнен.
  Исследовательский TEST-listener/scheduler удалить перед строгим повтором.
- Lifecycle-регистрация после RESOURCE_LOADING не подключает listener к уже
  открытым проектам: live1931 имел registrations={} при зелёном full1069.
  После регистрации сервиса подключать уже активные IDtProject отдельным
  идемпотентным start sweep, без кеширования неготового проекта как false;
  дальнейшие загрузки — lifecycle init, unload/stop снимают listener.
  Проверять fresh service ПОСЛЕ открытия fixture, start дважды, provider-only
  цикл без query touch. Это отдельный сценарий от импорта нового проекта.
- Однократного startup sweep также может не хватить: до готовности workspace
  getDtProjects бывает пуст, а RESOURCE_LOADING сервиса уже пропущен. Подписаться
  на IWorkspaceOrchestrator stateChanged ДО sweep; при active повторять attach,
  при closing снимать listener. Тестировать empty-initial-list → готовность →
  provider cycle и повтор готовности. INFO attach нужен для проверки live;
  mock ready callback не заменяет реальное открытие workspace.
- Disabled dcs-ql-hub сохраняет старые delegated markers: в regression ID и
  createdAt совпали до/после disable и provider change. Это не новый запуск.
  Отдельно проверять отсутствие НОВЫХ маркеров из чистого состояния без clean;
  не выдавать guard enabled за гарантию удаления всех прежних маркеров EDT.
- QL параметры: прямой delegate-test с новыми ICheckParameters не доказывает
  автоматический пересчёт потребителя. TOP skipSelectTopOne true→false→true
  в BSL пересчитывается штатно; СКД на EDT2026.1 требовала отдельного планирования.
  Проверенный публичный ICheckScheduler требует BOTH shortUID: dcs-ql-hub и
  delegate, плюс реальные DCS container IDs. Только hub UID отфильтровал делегат.
  Product listener реагирует на settingsChanged TOP, не меняет query/settings,
  уважает disabled hub/check; тестовый listener после research удалить и повторить
   исходную строгую регрессию. Новые DI/lifecycle/пакеты требуют полного гейта текущей платформы.
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
   новую metadata→BSL зависимость закрывать полным гейтом текущей платформы, не только targeted.
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
