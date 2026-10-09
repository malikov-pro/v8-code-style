#Область ПрограммныйИнтерфейс

// Correct localized strings of the NStr function: no up-747-nstr-unprintable-chars markers.

Процедура СообщитьКорректное(Подробности) Экспорт

	// A plain single-language string.
	Текст1 = НСтр("ru='Текст сообщения'");

	// Several languages without edge whitespace.
	Текст2 = НСтр("ru='Текст сообщения', en='Message text'");

	// Doubled single quotes inside the text.
	Текст3 = НСтр("ru='Текст о ''цитатах'''");

	// Interior whitespace is allowed.
	Текст4 = НСтр("ru='Текст   сообщения  с пробелами'");

	// A multiline text without edge whitespace.
	Текст5 = НСтр("ru='Длинный текст сообщения
	|в две строки'");

	// Non-language characters are separate string literals (standard 761).
	Текст6 = НСтр("ru='Не удалось сохранить файл по причине:'") + Символы.ПС + Подробности;

	// An empty value has no edge characters.
	Текст7 = НСтр("ru=''");

	// A malformed entry is the subject of the up-746-nstr-syntax check.
	Текст8 = НСтр("ru=""Текст в двойных кавычках""");

	// The first parameter that is not a string literal is skipped here.
	Текст9 = НСтр(Подробности);

КонецПроцедуры

#КонецОбласти
