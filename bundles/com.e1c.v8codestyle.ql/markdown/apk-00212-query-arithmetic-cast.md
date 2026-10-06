# Rounding of arithmetic operation results in queries

When performing arithmetic operations in queries, the 1C:Enterprise platform supports calculation precision of up to 8 decimal places. However, due to specifics of different DBMS the precision of results may differ from 8 in some situations. This applies to division operations and the AVERAGE (СРЕДНЕЕ) aggregate function.

Apply the explicit precision casting operator CAST(... AS Number(m, n)) (ВЫРАЗИТЬ(... КАК Число(m, n))) to the operands and/or the results of these operations if the precision of the result differs on different DBMS. The specified total precision should be minimal, but not less than the one sufficient to represent the values of each operand: unjustified overestimation of precision may lead to loss of precision in subsequent calculations and slightly slow down the query. Keep in mind the DBMS limitations on maximum number precision (the strictest one is 31 digits in total).

The check is an audit: whether the required precision is sufficient depends on the application (which number orders participate, how many decimal places the result needs) and cannot be proven statically, so each case is only reported. The check is **disabled by default** — enable it to audit arithmetic operations in the project.

Deliberate simplifications against the APK algorithm: multiplication is not checked (operands that may have a fractional part cannot be determined statically); the order of division operands (avoid dividing a small-order number by a large-order number) is not checked; sufficiency and minimality of the specified CAST precision is not evaluated.

## Noncompliant Code Example

```bsl
// Division and AVERAGE without a number cast: the result may lose precision on some DBMS.
Query = New Query(
    "SELECT
    |   Prices.Product,
    |   Prices.Price / Prices.Quantity AS UnitPrice,
    |   AVG(Prices.Price) AS AveragePrice
    |FROM
    |   InformationRegister.Prices AS Prices");
```

## Compliant Solution

```bsl
// The operation result and the AVERAGE parameter are cast to a number.
Query = New Query(
    "SELECT
    |   Prices.Product,
    |   CAST(Prices.Price / Prices.Quantity AS NUMBER(15, 2)) AS UnitPrice,
    |   AVG(CAST(Prices.Price AS NUMBER(15, 2))) AS AveragePrice
    |FROM
    |   InformationRegister.Prices AS Prices");
```

## See

- [Standard 535. Rounding of arithmetic operation results in queries](https://its.1c.ru/db/v8std#content:535:hdoc)
- Ported from the APK check `АПК_00212`.
