# Field from an outer join without ISNULL

A field read from the nullable side of an outer join contains `NULL` when no
row is matched. Comparing such a field with a value in `WHERE` or reading it in
a report usually gives an unexpected result, so the field should be wrapped in
`ISNULL` (`ЕСТЬNULL`).

The nullable side is the joined source of a `LEFT` join, the parent data source
of a `RIGHT` join, and both sides of a `FULL` join. Selected fields and the
`WHERE` condition of the query are inspected, together with the conditions of
the other joins of the same data source. A field is considered guarded when it
is inside an `ISNULL`/`ЕСТЬNULL` function call or an `IS NULL`/`IS NOT NULL`
comparison. When the `WHERE` condition contains an `IS NOT NULL` check (or a
negated `IS NULL`), the whole query is skipped, as in the BSL Language Server.

The check is disabled by default. One marker is placed on the outer join; the
message lists the fields found. `HAVING` and grouping are not inspected, same
as in the pinned BSL Language Server diagnostic.

## Noncompliant Code Example

```bsl
Query.Text = "SELECT
|   Products.Description AS ProductDescription,
|   Targets.Description AS TargetDescription
|FROM Catalog.Products AS Products
|   LEFT JOIN Catalog.Targets AS Targets
|   ON Products.Target = Targets.Ref";
```

## Compliant Solution

```bsl
Query.Text = "SELECT
|   Products.Description AS ProductDescription,
|   ISNULL(Targets.Description, """") AS TargetDescription
|FROM Catalog.Products AS Products
|   LEFT JOIN Catalog.Targets AS Targets
|   ON Products.Target = Targets.Ref";
```

## See

- [1C:Standards, ISNULL in queries](https://its.1c.ru/db/v8std#content:435)
- BSL Language Server diagnostic `FieldsFromJoinsWithoutIsNull`
