# Creating or executing a query in a loop

A query (or a query/report builder) shall not be created or executed inside a loop: every loop iteration sends a new query to the DBMS and slows the code down. Get all the data with a single query using the `In (&List)` condition, then process the result.

## Noncompliant Code Example

```bsl
Query = New Query;
Query.Text =
"SELECT
|	GoodsReceipt.Amount
|FROM
|	Document.GoodsReceipt AS GoodsReceipt
|WHERE
|	GoodsReceipt.Ref = &Ref";

For Index = 0 To DocumentList.Count() - 1 Do
	Query.SetParameter("Ref", DocumentList[Index]);
	Result = Query.Execute();   // query executed on every iteration
EndDo;
```

## Compliant Solution

```bsl
Query = New Query;
Query.Text =
"SELECT
|	SUM(GoodsReceipt.Amount) AS Amount
|FROM
|	Document.GoodsReceipt AS GoodsReceipt
|WHERE
|	GoodsReceipt.Ref In(&DocumentList)";

Query.SetParameter("DocumentList", DocumentList);
Result = Query.Execute();
```

## Sources

Port of the BSL Language Server diagnostic `create-query-in-cycle`.

The check is a replacement for `query-in-loop`, which is disabled by default since 09.10.2026: unlike it, this check also detects query and report builders, nested loops, and the While loop predicate. The cross-method scenario "loop calls a method that executes a query" remains covered by `query-in-loop` — enable it manually in the check settings if needed.

No quick fix is provided: rewriting the query (the `In (&List)` condition) requires a user decision.

## See

[Queries in loops](https://its.1c.ru/db/v8std/content/436/hdoc)
