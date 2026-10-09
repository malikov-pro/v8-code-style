#Region Public

// Correct calls of NotifyChanged (ОповеститьОбИзменении):
// no up-781-notify-handler-nonexistent markers.

Procedure NotifyAboutChanges(Data) Export

	// A simple handler name of another server common module.
	ОповеститьОбИзменении("BeforeWrite", Данные);

	// A qualified handler name: Common.CommonModule.
	ОповеститьОбИзменении("Common.CommonModule", Данные);

	// The handler of the same server common module.
	ОповеститьОбИзменении("NotifyAboutChanges", Данные);

	// The object form of the call needs no handler.
	ОповеститьОбИзменении(ЭтотОбъект);

	// A dynamic name (a variable) is not resolved.
	ИмяОбработчика = "BeforeWrite";
	ОповеститьОбИзменении(ИмяОбработчика, Данные);

КонецПроцедуры

#EndRegion
