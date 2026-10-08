# Проверки модулей 1С


Общее количество проверок: 159

| Код проверки | Наименование |
|--------------|--------------|
| [apk-00074-session-params-init](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-00074-session-params-init.html) | Инициализацию параметров сеанса следует выполнять в модуле сеанса |
| [apk-00150-registrar-access](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-00150-registrar-access.html) | Самодостаточность регистров: обращение к реквизиту «Регистратор» |
| [apk-00184-scheduled-jobs-manager](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-00184-scheduled-jobs-manager.html) | Обращение в программном коде к менеджеру регламентных заданий |
| [apk-00205-query-empty-result](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-00205-query-empty-result.html) | Пустота результата запроса проверяется выборкой вместо метода Пустой |
| [apk-00260-no-yo-letter](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-00260-no-yo-letter.html) | Буква «ё» в текстах модулей |
| [apk-00305-type-by-metadata-name](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-00305-type-by-metadata-name.html) | Тип значения переменной следует определять сравнением с типом |
| [apk-00306-metadata-via-object](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-00306-metadata-via-object.html) | Метаданные объекта получаются через свойство глобального контекста Метаданные |
| [apk-00334-postings-write-order](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-00334-postings-write-order.html) | Явная запись движений документа в обработчике проведения |
| [apk-00350-reuse-module-return](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-00350-reuse-module-return.html) | Бессмысленные методы в общем модуле с повторным использованием |
| [apk-00460-non-deprecated-functions](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-00460-non-deprecated-functions.html) | Неустаревшая функция в переопределяемом общем модуле |
| [apk-00460-non-export-methods](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-00460-non-export-methods.html) | Неэкспортный метод в переопределяемом общем модуле |
| [apk-00460-top-region](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-00460-top-region.html) | Внешняя область, кроме «ПрограммныйИнтерфейс», в переопределяемом общем модуле |
| [apk-00714-no-you-pronoun](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-00714-no-you-pronoun.html) | В сообщениях не употребляются местоимения «Вы», «Вас» и пр. |
| [apk-00715-no-exclamation-mark](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-00715-no-exclamation-mark.html) | Сообщения не должны содержать восклицательных знаков |
| [apk-01149-file-global-methods](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-01149-file-global-methods.html) | Использование методов глобального контекста для работы с файлами |
| [apk-01171-string-concat-in-loop](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-01171-string-concat-in-loop.html) | Массовая конкатенация строк в цикле |
| [apk-01179-obsolete-object-module](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-01179-obsolete-object-module.html) | Код в модуле устаревшего объекта метаданных |
| [apk-01190-update-version-format](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-01190-update-version-format.html) | Неверный формат версии в обработчике обновления ИБ |
| [apk-01192-summa-in-query](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-01192-summa-in-query.html) | Функция СУММА() с числовым операндом в запросах в текстах модулей |
| [apk-01194-no-french-quotes](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-01194-no-french-quotes.html) | Французские кавычки «ёлочки» в интерфейсных текстах |
| [apk-01205-main-language-code](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-01205-main-language-code.html) | Код основного языка получается через Метаданные.ОсновнойЯзык |
| [apk-01216-query-field-alias](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-01216-query-field-alias.html) | Псевдоним в тексте запроса совпадает с именем класса объектов метаданных |
| [apk-01219-cut-comments-format](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/apk-01219-cut-comments-format.html) | Оформление вырезаемых служебных комментариев |
| [begin-transaction](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/begin-transaction.html) | После начала транзакции отсуствует блок Попытка-Исключение |
| [bsl-canonical-pragma](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/bsl-canonical-pragma.html) | Аннотация написана канонически |
| [bsl-nstr-string-literal-format](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/bsl-nstr-string-literal-format.html) | НСтр формат строкового литерала |
| [bsl-variable-name-invalid](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/bsl-variable-name-invalid.html) | Правила образования имен переменных |
| [change-and-validate-instead-of-around](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/change-and-validate-instead-of-around.html) | Используется аннотация &ИзменениеИКонтроль вместо &Вместо |
| [code-after-async-call](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/code-after-async-call.html) | Код расположен после асинхронного вызова |
| [cognitive-complexity](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/cognitive-complexity.html) | Когнитивная сложность метода выше допустимой |
| [commented-code](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/commented-code.html) | Закомментированный фрагмент кода |
| [commit-transaction](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/commit-transaction.html) | Проверка нарушения схемы работы с транзакциями |
| [common-module-missing-api](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/common-module-missing-api.html) | Общий модуль должен иметь хотя бы один экспортный метод |
| [common-module-named-self-reference](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/common-module-named-self-reference.html) | Избыточное обращение по собственному имени внутри общего модуля |
| [constructor-function-return-section](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/constructor-function-return-section.html) | Секция возвращаемого значения функции-конструктора данных |
| [cyclomatic-complexity](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/cyclomatic-complexity.html) | Цикломатическая сложность метода выше допустимой |
| [data-exchange-load](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/data-exchange-load.html) | Проверка ОбменДанными.Загрузка в обработчике события |
| [deleting-collection-item](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/deleting-collection-item.html) | Удаление элемента при обходе коллекции |
| [deprecated-current-date](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/deprecated-current-date.html) | Использование устаревшего метода «ТекущаяДата» |
| [deprecated-find](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/deprecated-find.html) | Использование устаревшего метода «Найти» |
| [deprecated-procedure-outside-deprecated-region](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/deprecated-procedure-outside-deprecated-region.html) | Устаревшая процедура (функция) расположена вне области "УстаревшиеПроцедурыИФункции" |
| [doc-comment-collection-item-type](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/doc-comment-collection-item-type.html) | Тип коллекций в документирующем комментарии содержит тип элемента коллекции |
| [doc-comment-complex-type-with-link](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/doc-comment-complex-type-with-link.html) | Поле документирующего комментария использует объявление сложного типа вместо ссылки на тип |
| [doc-comment-description-ends-on-dot](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/doc-comment-description-ends-on-dot.html) | Многострочное описание документирующего комментария оканчивается на точку |
| [doc-comment-export-function-return-section](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/doc-comment-export-function-return-section.html) | Секция возвращаемого значения документирующего комментария для экспортной функции |
| [doc-comment-export-procedure-description-section](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/doc-comment-export-procedure-description-section.html) | Документирующий комментарий не содержит секцию "Описание" для экспортной процедуры (функции) |
| [doc-comment-field-in-description-suggestion](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/doc-comment-field-in-description-suggestion.html) | Многострочное описание документирующего комментария содержит определение поля |
| [doc-comment-field-name](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/doc-comment-field-name.html) | Поле документирующего комментария является корректным именем |
| [doc-comment-field-type](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/doc-comment-field-type.html) | Поле документирующего комментария не имеет определения типа |
| [doc-comment-field-type-strict](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/doc-comment-field-type-strict.html) | Поле документирующего комментария имеет описание типа |
| [doc-comment-parameter-in-description-suggestion](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/doc-comment-parameter-in-description-suggestion.html) | Многострочное описание документирующего комментария содержит определение параметра |
| [doc-comment-parameter-section](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/doc-comment-parameter-section.html) | В секции параметров документирующего комментария пропущено определение параметра |
| [doc-comment-procedure-return-section](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/doc-comment-procedure-return-section.html) | Документирующий комментарий содежрит секцию возвращаемого значения для процедуры |
| [doc-comment-redundant-parameter-section](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/doc-comment-redundant-parameter-section.html) | Секция параметров документирующего комментария избыточная |
| [doc-comment-ref-link](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/doc-comment-ref-link.html) | Ссылка документирующего комментария на существующий объект |
| [doc-comment-return-section-type](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/doc-comment-return-section-type.html) | Секция возвращаемого значения документирующего комментария содержит корректные типы |
| [doc-comment-type](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/doc-comment-type.html) | Определение типа документирующего комментария |
| [doc-comment-use-minus](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/doc-comment-use-minus.html) | Использование только дефис-минуса в документирующем комментарии |
| [dont-use-modality-mode](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/dont-use-modality-mode.html) | Checks dont use modality call in dont use modality mode. |
| [dynamic-access-method-not-found](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/dynamic-access-method-not-found.html) | Метод в объекте не найден |
| [empty-code-block](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/empty-code-block.html) | Пустой блок кода |
| [empty-except-statement](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/empty-except-statement.html) | Конструкция "Попытка...Исключение...КонецПопытки" не содержит кода в исключении |
| [empty-statement](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/empty-statement.html) | Пустой оператор |
| [event-handler-boolean-param](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/event-handler-boolean-param.html) | Использование булевого параметра обработчика события |
| [export-method-in-command-form-module](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/export-method-in-command-form-module.html) | Ограничения на использование экспортных процедур и функций в модуле команд и форм |
| [export-procedure-missing-comment](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/export-procedure-missing-comment.html) | Отсутствует комментарий к экспортной процедуре (функции) |
| [extension-method-prefix](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/extension-method-prefix.html) | У метода отсутствует префикс расширения |
| [extension-method-visible-mode](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/extension-method-visible-mode.html) | У метода в расширении модуля та же видимость, что и оригинального метода |
| [extension-variable-prefix](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/extension-variable-prefix.html) | У имени переменной отсутствует префикс расширения |
| [form-module-missing-pragma](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/form-module-missing-pragma.html) | Всегда использовать директивы компиляции в модуле формы |
| [form-module-pragma](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/form-module-pragma.html) | Использование директив компиляции модуля формы |
| [form-self-reference](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/form-self-reference.html) | Использование устаревшего псевдонима |
| [function-return-value-type](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/function-return-value-type.html) | Функция возвращает типизированное значение |
| [function-should-have-return](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/function-should-have-return.html) | Функция должна содержать возврат |
| [identical-expressions](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/identical-expressions.html) | Одинаковые выражения слева и справа от оператора |
| [if-condition-complexity](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/if-condition-complexity.html) | Сложное условие в операторе «Если» |
| [if-else-duplicated-code-block](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/if-else-duplicated-code-block.html) | Повторяющиеся блоки кода в операторе «Если» |
| [if-else-duplicated-condition](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/if-else-duplicated-condition.html) | Повторяющееся условие в операторе «Если» |
| [invocation-form-event-handler](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/invocation-form-event-handler.html) | Программный вызов обработчика события формы |
| [invocation-parameter-type-intersect](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/invocation-parameter-type-intersect.html) | Вызываемый тип пересекается с типом параметра |
| [line-length](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/line-length.html) | Длина строки |
| [link-part-comment-space](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/link-part-comment-space.html) | Пробел в описании метода перед ссылкой |
| [lock-out-of-try](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/lock-out-of-try.html) | Вызов "Заблокировать()" находится вне попытки |
| [magic-date](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/magic-date.html) | Магическая дата |
| [magic-number](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/magic-number.html) | Магическое число |
| [manager-module-named-self-reference](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/manager-module-named-self-reference.html) | Избыточное обращение по собственному имени внутри модуля менеджера |
| [method-isinrole-role-exist](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/method-isinrole-role-exist.html) | Обращение к несуществующей роли |
| [method-optional-parameter-before-required](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/method-optional-parameter-before-required.html) | Необязательные параметры процедуры/функции расположены перед обязательными |
| [method-param-value-type](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/method-param-value-type.html) | Параметр метода имеет тип |
| [method-semicolon-extra](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/method-semicolon-extra.html) | Лишняя точка с запятой в конце объявления метода |
| [method-size](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/method-size.html) | Размер метода |
| [method-too-many-params](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/method-too-many-params.html) | Метод содержит слишком много параметров |
| [missing-space](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/missing-space.html) | Отсутствует пробел |
| [missing-temp-storage-deletion](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/missing-temp-storage-deletion.html) | Отсутствует удаление из временного хранилища |
| [missing-temporary-file-deletion](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/missing-temporary-file-deletion.html) | Отсутствует удаление временного файла после использования. |
| [module-accessibility-at-client](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/module-accessibility-at-client.html) | Метод или переменная доступны НаКлиенте |
| [module-attachable-event-handler-name](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/module-attachable-event-handler-name.html) | Имя подключаемого обработчка события |
| [module-consecutive-blank-lines](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/module-consecutive-blank-lines.html) | Проверка максимального количства пустых строк |
| [module-empty-method](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/module-empty-method.html) | Проверка пустых методов |
| [module-region-empty](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/module-region-empty.html) | Область пустая |
| [module-self-reference](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/module-self-reference.html) | Избыточное использование псевдонима "ЭтотОбъект" |
| [module-structure-event-regions](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/module-structure-event-regions.html) | Раздел «Обработчики событий» содержит только методы являющиеся обработчиками событий |
| [module-structure-form-event-regions](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/module-structure-form-event-regions.html) | Проверяет регион обработчиков событий формы |
| [module-structure-init-code-in-region](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/module-structure-init-code-in-region.html) | Раздел инициализации содержит код инициализации |
| [module-structure-method-in-regions](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/module-structure-method-in-regions.html) | Проверяет что метод находится в области |
| [module-structure-top-region](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/module-structure-top-region.html) | Стандартные области структуры модуля верхнего уровня |
| [module-structure-var-in-region](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/module-structure-var-in-region.html) | Раздел описания переменных |
| [module-undefined-function](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/module-undefined-function.html) | Функция не определена |
| [module-undefined-method](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/module-undefined-method.html) | Метод не определен |
| [module-undefined-variable](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/module-undefined-variable.html) | Переменная не определена |
| [module-unused-local-variable](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/module-unused-local-variable.html) | Проверка неиспользуемых локальных переменных |
| [module-unused-method](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/module-unused-method.html) | Проверка неиспользуемых методов |
| [nested-statements](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/nested-statements.html) | Слишком большая вложенность операторов |
| [nested-ternary-operator](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/nested-ternary-operator.html) | Вложенный тернарный оператор |
| [new-color](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/new-color.html) | Использование конструкции "Новый Цвет" |
| [new-font](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/new-font.html) | Использование конструкции "Новый Шрифт" |
| [not-support-goto-operator-webclient](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/not-support-goto-operator-webclient.html) | Ограничение на использование оператора Перейти |
| [notify-description-to-server-procedure](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/notify-description-to-server-procedure.html) | Описание оповещения на серверную процедуру |
| [o-s-users-method](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/o-s-users-method.html) | Использование метода ПользователиОС |
| [object-module-export-variable](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/object-module-export-variable.html) | Использование переменных в программных модулях |
| [one-statement-per-line](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/one-statement-per-line.html) | Одно выражение в одной строке |
| [optional-form-parameter-access](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/optional-form-parameter-access.html) | Обращение к опциональному параметру формы |
| [property-return-type](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/property-return-type.html) | Свойство объекта имеет тип возвращаемого значения |
| [public-method-caching](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/public-method-caching.html) | Проверка кэширования программного интерфейса |
| [query-in-loop](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/query-in-loop.html) | Запрос в цикле |
| [reading-attribute-from-database](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/reading-attribute-from-database.html) | Чтение отдельного реквизита объекта из базы данных |
| [redundant-export-method](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/redundant-export-method.html) | Тексты модулей конфигурации не должны содержать неиспользуемые экспортные процедуры и функции. |
| [restriction-execute-external-code](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/restriction-execute-external-code.html) | Ограничение на выполнение «внешнего» кода |
| [restriction-execute-external-component-code](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/restriction-execute-external-component-code.html) | Ограничение на выполнение «внешнего» кода |
| [rewrite-method-parameter](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/rewrite-method-parameter.html) | Перезапись параметров метода |
| [rollback-transaction](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/rollback-transaction.html) | Проверка нарушения схемы работы с транзакциями |
| [secure-password-storage](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/secure-password-storage.html) | Проверка использования безопасного хранилища для паролей |
| [security-software-call](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/security-software-call.html) | Безопасность программного обеспечения, вызываемого через открытые интерфейсы |
| [self-assign](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/self-assign.html) | Присвоение переменной самой себе |
| [self-insertion](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/self-insertion.html) | Вставка коллекции в саму себя |
| [semicolon-missing](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/semicolon-missing.html) | Отсутствие точки с запятой в конце оператора |
| [server-execution-safe-mode](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/server-execution-safe-mode.html) | Отсутствует включение безопасного режима перед вызовом метода "Выполнить" или "Вычислить" |
| [space-at-start-comment](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/space-at-start-comment.html) | Пробел в начале комментария |
| [statement-type-change](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/statement-type-change.html) | Утверждение меняет тип |
| [string-literal-type-annotation-invalid-place](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/string-literal-type-annotation-invalid-place.html) | Теги размещены неправильно, внутри конструкции языка |
| [structure-constructor-too-many-keys](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/structure-constructor-too-many-keys.html) | Конструктор структуры содержит слишком много ключей |
| [structure-constructor-value-type](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/structure-constructor-value-type.html) | Типизация значений в конструкторе структуры |
| [structure-key-modification](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/structure-key-modification.html) | Модификация ключа структуры вне функции-конструктора |
| [ternary-operator-usage](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/ternary-operator-usage.html) | Использование тернарного оператора |
| [timeouts-in-external-resources](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/timeouts-in-external-resources.html) | Таймауты при работе с внешними ресурсами |
| [typed-value-adding-to-untyped-collection](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/typed-value-adding-to-untyped-collection.html) | Добавление типизированного значения в не типизированную коллекцию |
| [unknown-form-parameter-access](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/unknown-form-parameter-access.html) | Обращение к несуществующему параметру формы |
| [unused-parameters](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/unused-parameters.html) | Неиспользуемый параметр метода |
| [use-goto-operator](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/use-goto-operator.html) | Используется оператор Перейти |
| [use-non-recommended-method](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/use-non-recommended-method.html) | Использование не рекомендуемых методов |
| [useless-ternary-operator](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/useless-ternary-operator.html) | Бесполезный тернарный оператор |
| [using-form-data-to-value](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/using-form-data-to-value.html) | Использование РеквизитФормыВЗначение и ДанныеФормыВЗначение |
| [using-hardcode-network-address](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/using-hardcode-network-address.html) | Хранение ip-адресов в коде |
| [using-hardcode-path](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/using-hardcode-path.html) | Хранение путей к файлам в коде |
| [using-hardcode-secret-information](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/using-hardcode-secret-information.html) | Хранение конфиденциальной информации в коде |
| [using-isinrole](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/using-isinrole.html) | Использован метод "РольДоступна" |
| [using-modal-windows](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/using-modal-windows.html) | Использование модальных окон |
| [using-synchronous-calls](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/using-synchronous-calls.html) | Использование синхронных вызовов |
| [variable-value-type](../../../com.e1c.v8codestyle.bsl/check.descriptions/ru/variable-value-type.html) | Переменная имеет тип значения |
