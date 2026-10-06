# SUM() function with a numeric operand in queries in module texts

Checks that query texts in modules do not call the `SUM()` (`СУММА()`) function with a constant numeric operand to count records: with 10 million records and more the number overflows because of the default number capacity used by the platform in DBMS. The `QUANTITY` (`КОЛИЧЕСТВО`) function should always be used to count records instead.

`СУММА(0)` and calls with a non-numeric operand (a field or an expression, e.g. `СУММА(ВЫБОР … КОНЕЦ)`) are not reported, as in the original APK algorithm. Each occurrence is reported as a separate issue.

## Noncompliant Code Example

```bsl
Procedure CountRecords()
    
    Query = New Query;
    Query.Text =
    "SELECT
    |   SUM(1) AS Quantity
    |FROM
    |   Catalog.Nomenclature AS Nomenclature";
    
EndProcedure
```

## Compliant Solution

```bsl
Procedure CountRecords()
    
    Query = New Query;
    Query.Text =
    "SELECT
    |   QUANTITY(*) AS Quantity
    |FROM
    |   Catalog.Nomenclature AS Nomenclature";
    
EndProcedure
```

## See

- [Standard 787. Calculating the number of records in queries](https://its.1c.ru/db/v8std#content:787:hdoc)
- Ported from the APK check `АПК_01192`.
