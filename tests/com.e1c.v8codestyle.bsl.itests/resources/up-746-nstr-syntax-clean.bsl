#Область ПрограммныйИнтерфейс

// Correct localized strings of the NStr function: no up-746-nstr-syntax markers.

Процедура СообщитьКорректное(Сообщение) Экспорт
	
	Текст1 = НСтр("ru='Текст сообщения'");
	
	Текст2 = НСтр("ru = 'Текст с пробелами вокруг знака равенства'");
	
	Текст3 = NStr("en = 'Message text'");
	
	// Single quotes inside the text are doubled.
	Текст4 = НСтр("ru='Текст о ''цитатах'''");
	
	// Several languages: comma and semicolon separators.
	Текст5 = НСтр("ru='Текст', en='Text'");
	Текст6 = НСтр("ru = 'Текст'; en = 'Text'");
	
	// Region suffix in the language code.
	Текст7 = NStr("zh-CN='Text'");
	
	// The second parameter of the function is allowed.
	Текст8 = НСтр("ru='Текст'", "ru");
	
	// Double quotes in the text are escaped in the BSL literal.
	Текст9 = НСтр("ru = 'Видите ли вы корректно это сообщение: ""%1""?'");
	
	// A multiline message text.
	Текст10 = НСтр("ru = 'Длинный текст сообщения
	|в две строки'");
	
КонецПроцедуры

#КонецОбласти
