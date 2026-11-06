&НаКлиентеНаСервереБезКонтекста
Функция КлиентСервер(ServerProvider)
    clientprovider = 1;
    CALLPROVIDER = 1;
    ServerProvider = 1;
    Возврат ServerProvider;
КонецФункции

&НаСервереБезКонтекста
Функция Сервер(ServerProvider)
    ServerProvider = 1;
    ClientProvider = 1;
    Возврат ServerProvider;
КонецФункции

&НаСервереБезКонтекста
Функция ПоУмолчанию(ServerProvider = Неопределено)
    ServerProvider = 1;
    Возврат ServerProvider;
КонецФункции
