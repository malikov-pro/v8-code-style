# Using the ALLOWED keyword in queries

Checks queries that use the ALLOWED keyword. It hides records the user has no rights for, which may distort the query result: the user sees incomplete data while the business logic expects all of it.

Do not use ALLOWED in queries that affect calculations and operations. If the operation needs data the user cannot read — either grant read rights or interrupt the operation with an access-denied message. ALLOWED is acceptable when the hidden data does not participate in business processes (dynamic lists, interactive reports).

The check is an audit: legitimate usage depends on the role of the query in the application and cannot be proven statically, so each usage is only reported. The check is **disabled by default** — enable it to audit the project.

## Noncompliant Code Example

```bsl
// The query affects a calculation: data is hidden and the result is distorted.
Query = New Query(
    "SELECT ALLOWED
    |   Batches.Product,
    |   Batches.CostBalance
    |FROM
    |   AccumulationRegister.BatchesOfGoods.Balance AS Batches");
```

## Compliant Solution

```bsl
// If there is no access to the data for the calculation, interrupt the operation.
If Not HasRightsForCostCalculation Then
    Raise Exception;
EndIf;

Query = New Query(
    "SELECT
    |   Batches.Product,
    |   Batches.CostBalance
    |FROM
    |   AccumulationRegister.BatchesOfGoods.Balance AS Batches");
```

## See

- [Standard 415. Restrictions on ALLOWED clauses in queries](https://its.1c.ru/db/v8std#content:415:hdoc)
- Ported from the APK check `АПК_00429`.
