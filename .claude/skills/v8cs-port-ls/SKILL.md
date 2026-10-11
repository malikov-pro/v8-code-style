---
name: v8cs-port-ls
description: Конвейер переноса диагностики BSL Language Server в форк v8-code-style — выбор кандидата из гэп-листа, дедупликация, реализация BasicCheck, itests, сдача. Использовать при переносе диагностик LS, поиске свободных диагностик, учёте прогресса портов.
---

# Конвейер: перенос диагностики BSL LS → проверка форка

Смежный регламент: `v8cs-port-check`, сборки — `v8cs-build`, приёмка —
`v8cs-deploy`. Правила приоритетов/стабильного снимка — AGENTS.md.
Источники и реестры относительно корня проекта:
- `../_ext_src/bsl-language-server` — ТОЛЬКО чтение, фиксировать commit;
- `../_notes/ls-registry.md` / `ls-registry.json` — поштучный актуальный учёт;
- `ls-registry-decisions.tsv` / `build-ls-registry.py` — решения и проверка
  полноты каталога/регистраций (оба в `../_notes`);
- `../_notes/ls-next-plan-2026-10-12.md` — принятый план после аудита;
- `../_notes/ls-port-gap.md` — история, не очередь для слепого выбора.

Не хранить текущие количества в каждом скилле: читать реестр и сверять
actual `plugin.xml`. Прямой порт !=полная эквивалентность. Native overlap
без доказательства — кандидат на пробу, не покрытая диагностика.

## 0. Выбор кандидата

- Из принятого плана/поштучного реестра: сначала corrective mini-batch
  IfConditionComplexity (+1 граница) и LogicalOrInJoin (same-field exemption),
  затем reference/block regressions и новые BSL/QL-инкременты. Не начинать
  реализацию нового чека, если пользователь просит только аудит/план.
- Дедуп руками: `grep -ri <имя>` по `bundles/*/markdown/` + `docs/checks/`.
  Token-overlap матчёр врёт в обе стороны (см. гэп-лист).
- Выбирать действие по статусу: перенесено — corpus/regression при новых
  основаниях; частично — устранить конкретное расхождение; штатный аналог —
  не дублировать; кандидат — proof API/native и реализация; блокер — отдельно
  инфраструктура/данные, не «никогда не переносится».
- Полный UnreachableCode/AllFunctionPathMustHaveReturn требует CFG; lite
  unreachable уже существует. CognitiveComplexity — parse listener, **CFG
  не обязательная предпосылка**. Typo требует spell-service/словарей.
- DoubleNegatives уже в develop: валидный NOT(NE)/NOT(NOT) доступен на обеих
  платформах; NOT NOT без скобок имеет syntax errors. BadWords — configurable
  regex, не словарный блокер. Query-канал открыт через штатные hubs.
- QueryToMissingMetadata ordinary MDO и MultilineStringInQuery уже имеют
  live native-диагностику. Broad QueryNestedFieldsByDot не равен typed
  `ql-composite-reference-dereference` — сначала политика overlap/CAST scope.

## 1. Спецификация из источника

- `docs/diagnostics/<Name>.md` (RU+EN), `<Name>Diagnostic.java`,
  `<Name>Diagnostic_ru.properties` (точные тексты сообщений).
- Инвариант = что ищется (не название!); severity/type; параметры; qfix в LS
  (QuickFixProvider) — оценить по критерию «механическая замена vs решение
  пользователя» (решение пользователя → фикса нет, описать в карточке).
- Сверять код, параметры, scope/module-body и default текущего pinned LS,
  не только markdown. Все отличия записывать в реестр/карточку; корпус LS
  и counterpart проверять контрпримерами, не token-overlap процентом.
- Новое API: existing example +javap/MANIFEST; research-тест отвечает на
  один вопрос и не заменяет продуктовые regressions.

## 2. Реализация

- id = ключ LS в kebab-case (= имя md-дока, например `empty-code-block`).
- Класс `<Name>Check extends BasicCheck` в check-пакете канала
  (bsl → `com.e1c.v8codestyle.bsl`). Референсы-шаблоны: 
  `FunctionShouldHaveReturnCheck` (EClass-объект), `EmptyCodeBlockCheck`
  (module-скан + параметр), `LineLengthCheck` (DirectLocation на строку).
- Маппинг severity: BLOCKER→CRITICAL, CRITICAL→CRITICAL/MAJOR, MAJOR→MAJOR,
  MINOR→MINOR, INFO→TRIVIAL. Маппинг type: ERROR→IssueType.ERROR,
  CODE_SMELL→WARNING/CODE_STYLE, SUSPICIOUS→ERROR/WARNING.
- Extension — `CommonSenseCheckExtension(getCheckId(), BslPlugin.PLUGIN_ID)`.
- plugin.xml: `<check category="com.e1c.v8codestyle.bsl" class=
  "com.e1c.v8codestyle.internal.bsl.ExecutableExtensionFactory:<FQCN>">`.
- Карточки ОБЕ: `markdown/<id>.md` (EN, корень) **и** `markdown/ru/<id>.md` —
  CheckDescriptionTest требует наличие для языков "" и "ru".

## 3. API-грабли EDT-модели (все проверены 05.10)

- **EmptyStatement (и, вероятно, другие «пустые» EObject) НЕ доставляется
  через checkedObjectType** — не попадает в BM-модель. Только якорь METHOD +
  `EcoreUtil2.getAllContentsOfType(method, EmptyStatement.class)`.
- **Правило грамматики EmptyStatement не потребляет текст** («;» — снаружи,
  соседний лист): узел EObject нулевой длины, якорить DirectLocation на
  лист «;» (образец — наш EmptyStatementCheck); Marker от addIssue(msg, obj)
  на таком EObject не работает.
- «Выкл. по умолчанию» = IBasicCheckExtension с `definition.setEnabled(false)`
  (наш OptInCheckExtension в com.e1c.v8codestyle.check); НЕ регистрировать в
  CommonCheckRegistry, чтобы не включался групповым переключателем.
- `IDocument.getDefaultLineDelimiter()` НЕ существует в таргете 2026.1 —
  `TextUtilities.getDefaultLineDelimiter(document)` (qfix-переносы строк).
- Логика «переприсваивание до первого чтения» (rewrite-method-parameter):
  в usedParams добавлять ИМЕНА, ПРОЧИТАННЫЕ В RIGHT (все StaticFeatureAccess
  правой части), а не имя левой части — иначе clean-тест ловит ложный флаг.
- **Узел строкового литерала включает скрытый whitespace** (пробел после «=»):
  literalContent — `.trim()` ДО срезания кавычек, иначе кавычки остаются и
  регексы/URL-фильтр молча не работают (поймано тестами хардкодов, 05.10).
- **Лист комментария включает завершающий \n** (и возможный \r): проверки
  good/annotation-паттернами через matches() — сначала `text.strip()`
  (поймано дампом маркеров в space-at-start-comment, 06.10).
- CodeRecognizer/BSLFootprint LS перенесены как CommentCodeRecognizer
  (check-пакет bsl-бандла): детекторы с весами (contains/keywords/camelCase/
  endsWith/голова процедуры), вероятность `1-Π(1-p)`, threshold 0.9.
- **EcoreUtil2.getAllContentsOfType НЕ включает корень**: если ищете узлы
  типа T, начиная с узла, который сам T — считайте его отдельно
  (if-condition-complexity: предикат-AND не считался, 06.10).
- Форматирование-чеки (missing-space): xtext-листы — оператор отдельный
  лист, ws-листы между; «пробел есть» = соседний лист blank или сосед
  начинается/заканчивается переводом строки.
- `OperatorStyleCreator.getType().getName()` — английское имя типа
  (Structure/Map/FTPConnection); тип не разрешился — фолбэк: имя из текста
  «Новый <Тип>» регекспой. mcore.TypeItem extends DuallyNamedElement (есть
  getNameRu). ECJ-загадка: `mcoreType instanceof DuallyNamedElement` в
  bsl-бандле НЕ компилируется («cannot be a subtype of the Pattern type»),
  хотя иерархия верна — вызывать getName() прямо на Type.
- Тернарник = Invocation, `methodAccess instanceof StaticFeatureAccess` с
  name «?» (грамматика: `name = Question`); EClass Ternary НЕ существует.
  Ветви/условие — `Invocation.getParams()`; булев литерал = BooleanLiteral.isIsTrue().
- `Method.eAll()` недоступен в компиляции Tycho-бандла — использовать
  `org.eclipse.xtext.EcoreUtil2` (НЕ `org.eclipse.xtext.util.EcoreUtil2`).
- Функции/процедуры — разные EClass: `Function`, `Procedure` (нет
  `Method.getKind()`). `checkedObjectType(METHOD)` ловит оба (фреймворк
  матчит супертипы — доказано ModuleStructureMethodInRegionCheck).
- Имя метода/объекта: `McorePackage.Literals.NAMED_ELEMENT__NAME`
  (не METHOD__NAME — его не существует).
- `CheckComplexity` = {NORMAL, COMPLEX} — TRIVIAL нет.
- `IssueType` = {ERROR, WARNING, SECURITY, PERFORMANCE, PORTABILITY,
  LIBRARY_DEVELOPMENT_AND_USAGE, CODE_STYLE, UI_STYLE, SPELLING,
  CRITICAL_DATA_INTEGRITY} — SUSPICIOUS нет.
- `ICheckParameters.getInt(String)` — single-arg, throws WrongParameterException
  (есть и getString для строковых параметров).
- IfStatement: `getIfPart()`/`getElsIfParts()` → Conditional (predicate +
  statements), `getElseStatements()`. **Пустой список else означает и «нет
  Иначе», и «пустое Иначе»** — наличие «Иначе» проверять по Keyword-листьям
  узла (текст «Иначе»/«Else», grammar element instanceof Keyword).
- Циклы: WhileStatement extends LoopStatement; ForToStatement/ForEachStatement
  extends ForStatement; ForStatement extends LoopStatement. Тело —
  LoopStatement.getStatements().
- ReturnStatement.getExpression() != null → возврат со значением.
- Текст узла: `NodeModelUtils.findActualNodeFor(obj).getText()`.

## 4. Тест

- Один объединённый тест-метод: clean-ресурс → 0, violating → N.
- Включать обратные переходы и изменения existing value: clean→violation→
  fix, precise objects/ranges, отсутствие дублей. Если поведение зависит от
  родителей/типов/обращений — regression изменения зависимости без fullclean.
  Незнакомый AST проверять на syntax-clean ресурсе отдельно от recovery.
  FixMethodOrder/MethodSorters в p2-junit ЗАПРЕЩЕНЫ (Access restriction).
- Отладка счётчиков: дамп `Marker::getMessage` в сообщение assertTrue.
- Ресурсы itest — синтаксически валидный BSL! Ошибка в ресурсе даёт пустые
  AST-фрагменты и фантомные маркеры (реальный кейс: «Пока … Делать» вместо
  «Цикл» = пустой WhileStatement из ниоткуда).
- Прогон: `bash scripts/test-check.sh <TestClass[,TestClass2]>` (всегда clean).
- QL: `--module ql`, второй профиль: `--profile edt-2026.2`. Targeted не
  означает весь suite. Активный snapshot не редактировать; один Maven.

## 5. Сдача

- Рабочая мини-пачка2–3 связанных одного канала; каждому свои tests/commit/
  строка реестра. First vertical slice неизвестного канала до live, потом
  группировать. Итерация targeted, перед integration весь затронутый
  itests-модуль обеих платформ. Fullcompile обеих — раз в основную пачку,
  при общих dependencies и перед release (v8cs-build).
- Только markdown/messages-text правки: CheckDescriptionTest +generated
  index, не новый fullsuite. EP/ключи/parameters/MANIFEST — runtime изменения.
- Сохранить p2 профиля перед следующим clean; выполнить own live smoke:
  initial clean для нового registration, потом bad→fix→bad **без clean**,
  затем финальный clean/log. Без live не merge: не ждать произвольных50 портов.
- Audit integration по исходным commits; обновить ls-registry decisions
  только по фактически merged реализации с ограничениями. Build green не
  доказывает полноту/точность переноса. Master/tag не менять без команды.
