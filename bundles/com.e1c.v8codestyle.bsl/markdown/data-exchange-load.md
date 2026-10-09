# Check DataExchange.Load in event handler

Mandatory checking of DataExchange.Load is absent in event handler

A check in any condition is allowed: if the DataExchange.Load access appears in any If/ElsIf condition (including negation "Not DataExchange.Load" or as part of a complex expression), the handler is considered checked and no issue is reported.

## Noncompliant Code Example

```bsl
Procedure BeforeWrite(Cancel)
// handler code
// ...
EndProcedure
```

## Compliant Solution

```bsl
Procedure BeforeWrite(Cancel)
If DataExchange.Load Then
     Return;
EndIf;

// handler code
// ...
EndProcedure
```

## See

