# Field alias in a dynamic list query matches a metadata object class name

Checks that aliases after the `КАК`/`AS` keywords in the query texts of form attributes of the Dynamic list type are not named after metadata object class names (`Справочник`, `Документ`, `Catalog`, `Document`, etc.). Such an alias usually does not describe the purpose of the source or field in the specific query: aliases should be meaningful and formed from the domain terms like variable names (for example, `ТоварыНаСкладах`).

The keyword is searched textually in the query text of the dynamic list; comments in the query text are not inspected. For aliases in query texts in modules see the related check `apk-01216-query-field-alias` (bsl channel).

## Noncompliant

The query text of a dynamic list attribute:

```sql
ВЫБРАТЬ
	Товары.Ссылка КАК Справочник,
	ЕСТЬNULL(Остатки.КоличествоОстаток, 0) КАК Остаток
ИЗ
	Справочник.Номенклатура КАК Документ
		ЛЕВОЕ СОЕДИНЕНИЕ РегистрНакопления.ТоварыНаСкладах.Остатки КАК Справочник
		ПО Товары.Ссылка = Остатки.Номенклатура
```

## Compliant

```sql
ВЫБРАТЬ
	ВсяНоменклатура.Ссылка КАК Товар,
	ЕСТЬNULL(Остатки.КоличествоОстаток, 0) КАК Остаток
ИЗ
	Справочник.Номенклатура КАК ВсяНоменклатура
		ЛЕВОЕ СОЕДИНЕНИЕ РегистрНакопления.ТоварыНаСкладах.Остатки КАК ОстаткиНаСкладах
		ПО ВсяНоменклатура.Ссылка = ОстаткиНаСкладах.Номенклатура
```

## See

- [Standard 758. Data source aliases in queries](https://its.1c.ru/db/v8std#content:758:hdoc)
- Ported from the APK check `АПК_01214`.
