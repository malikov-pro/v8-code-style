# Dereferencing a composite reference in a query

Dereferencing a composite reference can add implicit joins to every table
in its type. Use `CAST` to restrict the type before accessing a field.

```sdbl
SELECT Data.Target.Description FROM Catalog.Data AS Data
// Prefer a specific target:
SELECT CAST(Data.Target AS Catalog.Products).Description FROM Catalog.Data AS Data
```

The check uses EDT field types, not dot counts. Broad `TypeSet` groups
(including `AnyRef`) are expanded in the query context and duplicate concrete
types are counted once. `minReferenceTypes` defaults to 2; lower values are
treated as 2.

DB-backed reference families covered: catalogs, documents, exchange plans,
charts of accounts, characteristic and calculation types, business processes
and tasks. Enums, route points and primitives are excluded. Unknown types and
syntax errors are skipped. A field accessed after an explicit `CAST` is clean;
dereferences inside the cast argument are still checked.

This is a separate standards-oriented check, not a literal LS or APK port.
No quick fix: the appropriate target type requires the author's decision.

[Standard 654](https://its.1c.ru/db/v8std#content:654:hdoc)
