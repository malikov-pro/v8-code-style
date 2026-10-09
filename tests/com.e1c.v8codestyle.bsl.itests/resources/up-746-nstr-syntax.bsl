#Область ПрограммныйИнтерфейс

// Demonstrates violations of the localized string syntax (standard 761).
// Expected: 7 markers of up-746-nstr-syntax, one per violated string below.

Процедура СообщитьНарушение(Сообщение) Экспорт
	
	// 1. The message text is in double quotes instead of single quotes.
	Текст1 = НСтр("ru=""Текст в двойных кавычках""");
	
	// 2. The message text is without quotes.
	Текст2 = НСтр("ru=Текст");
	
	// 3. The language code is missing.
	Текст3 = НСтр("Просто текст со словами");
	
	// 4. A single quote inside the text is not doubled (the entry breaks).
	Текст4 = НСтр("ru='Текст с ' кавычкой внутри'");
	
	// 5. The message text is not closed.
	Текст5 = НСтр("ru='Незакрытый текст");
	
	// 6. The localized string is empty.
	Текст6 = НСтр("");
	
	// 7. A separator without the next language entry.
	Текст7 = НСтр("ru='Текст',");
	
КонецПроцедуры

#КонецОбласти
