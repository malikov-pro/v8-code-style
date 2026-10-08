# Constant is written inside a transaction

Constants are written (**Constants.&lt;Name&gt;.Set(...)** / **Constants.&lt;Name&gt;.Write(...)**, рус. **Константы.&lt;Имя&gt;.Установить/Записать**) outside transactions. While a constant is being written, work of other sessions writing the same constant is suspended, so writing a constant inside a transaction becomes a bottleneck in concurrent use.

A call is reported when it is lexically placed in the same method between the **BeginTransaction** (рус. «НачатьТранзакцию») and **CommitTransaction** (рус. «ЗафиксироватьТранзакцию») calls — by statement positions, without unfolding nested `If`/loops. A transaction opened in another method is not tracked; constant reads (рус. «Получить») and writes of other objects (record sets, etc.) are not reported.

The second part of the standard — workarounds of the constant locking (total caching of constants in session parameters or in common module functions with repeated returned values) — is not statically checkable and is not covered.

## Noncompliant Code Example

```bsl
Процедура ОбработкаПроведения(Отказ, РежимПроведения)

	НачатьТранзакцию();

	ТекущееЗначение = Константы.СчетчикПроведенныхДокументов.Получить();
	Константы.СчетчикПроведенныхДокументов.Установить(ТекущееЗначение + 1);

	ЗафиксироватьТранзакцию();

КонецПроцедуры
```

## Compliant Solution

```bsl
Процедура ЗаписатьСчетчик()

	ТекущееЗначение = Константы.СчетчикПроведенныхДокументов.Получить();
	Константы.СчетчикПроведенныхДокументов.Установить(ТекущееЗначение + 1);

КонецПроцедуры
```

## See

- [Standard 632. Using constants](https://its.1c.ru/db/v8std#content:632:hdoc)
- Ported from the APK check `АПК_00157` (simplified scope, see above).
