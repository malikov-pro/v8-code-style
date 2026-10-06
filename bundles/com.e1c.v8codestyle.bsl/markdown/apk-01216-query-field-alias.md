# Query alias matches a metadata object class name

Checks that aliases after the `КАК`/`AS` keywords in query texts in modules are not named after metadata object class names (`Справочник`, `Документ`, `Catalog`, `Document`, etc.). Such an alias usually does not describe the purpose of the source or field in the particular query: source and field aliases should be meaningful, formed from the domain terms, like variable names (for example, `ТоварыНаСкладах`).

The keyword occurrence is analyzed textually in query texts in modules; comments are not checked.

## Noncompliant Code Example

```bsl
Query.Text =
"SELECT
|   Catalog.Nomenclature.Ref AS Catalog,
|   IsNull(Stocks.QuantityBalance, 0) AS Balance
|FROM
|   Catalog.Nomenclature AS Document";
```

## Compliant Solution

```bsl
Query.Text =
"SELECT
|   AllNomenclature.Ref AS Goods,
|   IsNull(Stocks.QuantityBalance, 0) AS Balance
|FROM
|   Catalog.Nomenclature AS AllNomenclature
|       LEFT JOIN AccumulationRegister.GoodsInStock.Balances AS Stocks
|       ON AllNomenclature.Ref = Stocks.Nomenclature";
```

## See

- [Standard 758. Data source aliases in queries](https://its.1c.ru/db/v8std#content:758:hdoc)
- Ported from the APK check `АПК_01216`.
