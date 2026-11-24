# Overuse "Reference" in a query

Reading the `Reference` (`Ссылка`, `Ref`) field of a reference attribute makes the DBMS
join the referenced table for nothing, so the query reads one more table and
returns the same link it already has.

A path is reported when the last `Reference` segment is redundant:

- a path that ends with `Reference` is reported unless the previous segment is
  a data source of the same query — `Table.Reference` is the reference itself;
- `Reference` as the first segment of a longer path is not reported;
- `Table.Reference.Attribute` is not reported when the table is a tabular
  section, a virtual table or a table without resolved metadata: there
  `Table.Reference` is the owner reference;
- a later `Reference` followed by another segment is always reported.

Fields of nested queries are checked as their own query, fields inside an inline
table constructor are not checked, as in the pinned BSL Language Server. The
marker covers the whole field path.

## Noncompliant Code Example

```bsl
Query.Text = "SELECT
|   Doc.Ref.Description AS DocDescription
|FROM Document.SalesOrder AS Doc";
```

## Compliant Solution

```bsl
Query.Text = "SELECT
|   Doc.Description AS DocDescription
|FROM Document.SalesOrder AS Doc";
```

## See

- [1C:Standards, dereferencing composite reference fields](https://its.1c.ru/db/v8std/content/654/hdoc)
- BSL Language Server diagnostic `RefOveruse`
