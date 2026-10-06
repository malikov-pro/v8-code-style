---
name: v8cs-port-ls
description: Конвейер переноса диагностики BSL Language Server в форк v8-code-style — выбор кандидата из гэп-листа, дедупликация, реализация BasicCheck, itests, сдача. Использовать при переносе диагностик LS, поиске свободных диагностик, учёте прогресса портов.
---

# Конвейер: перенос диагностики BSL LS → проверка форка

Смежный регламент: `v8cs-port-check` (общие правила, quick fix, верификация,
ветки). Статус портов и гэп-лист: `_notes/ls-port-gap.md`.
Перенесено: 2 АПК + 34 LS (состояние — в файле). Источник: 
`_ext_src/bsl-language-server` — ТОЛЬКО чтение; правки LS — апстрим/форк LS.
Батчи №2-№7: merge develop 006e2205 / 19c3237e / 0465eac3 / 9c992af6 /
5b0db929 / 27ca921b. Батч №7 (06.10, 10 проверек): missing-space,
magic-number, magic-date, if-condition-complexity, nested-statements,
cyclomatic-complexity, cognitive-complexity (упрощённая формула),
using-modal-windows, missing-temp-storage-deletion (упрощённая),
unused-parameters.
Деплой v20261005-2021+ в EDT 2026.1 + живой смоук партий 2-7 — пройден.

## 0. Выбор кандидата

- Из гэп-листа («Кандидаты на следующие порты») или новый разбор.
- Дедуп руками: `grep -ri <имя>` по `bundles/*/markdown/` + `docs/checks/`.
  Token-overlap матчёр врёт в обе стороны (см. гэп-лист).
- ❌ НЕ переносимы (проверено):
  - ловит компилятор 1С (procedure-returns-value — значение в процедуре);
  - unreachable-code — требует CFG (ControlFlowGraphIndex), нет в фреймворке;
  - **double-negatives — битый AST EDT-парсера**: «Не Сумма <> 0» →
    `Unary(НЕ, operand=NULL)`, «Не Не А» → внешний НЕ с operand=Binary.
    Проверено дампами AST в itest-рантайме 05.10;
  - typo/bad-words — словари; query-* — нужен query-канал (отдельная работа).

## 1. Спецификация из источника

- `docs/diagnostics/<Name>.md` (RU+EN), `<Name>Diagnostic.java`,
  `<Name>Diagnostic_ru.properties` (точные тексты сообщений).
- Инвариант = что ищется (не название!); severity/type; параметры; qfix в LS
  (QuickFixProvider) — оценить по критерию «механическая замена vs решение
  пользователя» (решение пользователя → фикса нет, описать в карточке).

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
  FixMethodOrder/MethodSorters в p2-junit ЗАПРЕЩЕНЫ (Access restriction).
- Отладка счётчиков: дамп `Marker::getMessage` в сообщение assertTrue.
- Ресурсы itest — синтаксически валидный BSL! Ошибка в ресурсе даёт пустые
  AST-фрагменты и фантомные маркеры (реальный кейс: «Пока … Делать» вместо
  «Цикл» = пустой WhileStatement из ниоткуда).
- Прогон: `bash scripts/test-check.sh <TestClass[,TestClass2]>` (всегда clean).

## 5. Сдача

- Полный гейт (цепочка реактора + bsl.itests целиком, `~15 мин`) → merge
  develop → push. Серия портов: коммит на каждый чек, гейт — периодически
  (договорённость 05.10: порты подряд, тесты сам, UI-проверка при ~50 наших
  портах; полный itests-гейт — перед merge партии в develop).
- Смоук в EDT: `scripts/smoke-edt.sh` (сборка+установка+запуск EDT) →
  MCP `clean_project` + `get_project_errors` (new checks живут только после
  clean build — revalidate недостаточно). Пример-модуль в
  АПК.ОшибкиДляПроверки — по желанию (для АПК-портов обязателен).
