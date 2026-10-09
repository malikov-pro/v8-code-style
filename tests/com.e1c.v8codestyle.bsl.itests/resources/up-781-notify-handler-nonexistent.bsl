#Region Public

// Demonstrates calls of NotifyChanged (ОповеститьОбИзменении) with a
// handler name that does not resolve to any export method of the server
// common modules of the configuration (standard 467).
// Expected: 2 markers of up-781-notify-handler-nonexistent.

Procedure NotifyAboutChanges(Data) Export

	// 1. The handler does not exist in any server common module (a typo).
	ОповеститьОбИзменении("НесуществующийОбработчик", Данные);

	// 2. The named common module does not exist.
	ОповеститьОбИзменении("НесуществующийМодуль.Обработчик", Данные);

КонецПроцедуры

#EndRegion
