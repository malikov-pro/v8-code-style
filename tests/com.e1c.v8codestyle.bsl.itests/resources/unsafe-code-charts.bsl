Процедура Проба(Код) Экспорт
    Счет = ПланыСчетов.Accounts.НайтиПоКоду(Код);
    Account = ChartsOfAccounts.Accounts.FindByCode(Code);
    Вид = ПланыВидовХарактеристик.Kinds.НайтиПоКоду(Код);
    Kind = ChartsOfCharacteristicTypes.Kinds.FindByCode(Code);
КонецПроцедуры
