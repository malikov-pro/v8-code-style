# Using CAST(... AS STRING(N)) in queries

When evaluating expressions in queries — in particular, when comparing values, grouping and getting distinct — unlimited-length string attributes have to be cast to a string of a certain length so that the expression is evaluated correctly: `CAST(... AS STRING(1000))` (ВЫРАЗИТЬ(... КАК СТРОКА(1000))).

Keep in mind that frequent casting of an unlimited string to a limited length in queries and DCS reports may be a sign of a wrong design decision and a signal to revise the string attribute type in favor of a limited-length string. For DCS report fields, set the field value type parameter (on the Data sets tab) instead of casting in the query.

The check is a **manual audit**: proving that the specified string length is sufficient for the expression to be evaluated correctly is up to the developer (the APK algorithm has this manual step), so the check is **disabled by default** (enable it in the check settings), and each casting is only reported. The related md check `apk-00141-unlimited-string` reports the unlimited-length attributes themselves.

## Noncompliant Code Example

Frequent truncation of an unlimited string in queries:

```bsl
Query = New Query(
    "SELECT DISTINCT
    |   CAST(Balances.Comment AS STRING(1000)) AS Comment
    |FROM
    |   AccumulationRegister.Balances AS Balances");
```

## Compliant Solution

Limit the length of the `Comment` attribute in metadata (for example, 200 characters) and remove the casting from the query; if the unlimited length is justified — make sure the casting length is sufficient and revise the design decision.

## See

- [Standard 432. Using string-type attributes](https://its.1c.ru/db/v8std#content:432:hdoc)
- Ported from the APK check `АПК_00142`.
