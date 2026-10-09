#Область ПрограммныйИнтерфейс

// Demonstrates localized strings of the NStr function that start or end
// with an unprintable character (standard 761).
// Expected: 6 markers of up-747-nstr-unprintable-chars, one per violated
// string below.

Процедура СообщитьНарушение(Подробности) Экспорт

	// 1. A trailing space inside the message text.
	Текст1 = НСтр("ru='Текст сообщения '");

	// 2. A leading space inside the message text.
	Текст2 = НСтр("ru=' Текст сообщения'");

	// 3. A trailing tab inside the message text.
	Текст3 = НСтр("ru='Текст сообщения	'");

	// 4. A trailing line feed inside the message text.
	Текст4 = НСтр("ru = 'Не удалось сохранить файл по причине:
	|'") + Подробности;

	// 5. An edge space in one of several language entries.
	Текст5 = НСтр("ru='Текст сообщения', en='Message '");

	// 6. A leading line feed inside the message text.
	Текст6 = НСтр("ru='
	|Текст сообщения'");

КонецПроцедуры

#КонецОбласти
