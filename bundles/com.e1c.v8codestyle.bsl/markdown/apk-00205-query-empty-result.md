# Query result emptiness is checked via the selection instead of the Empty method

Checks that the absence of rows in a query result is checked with the **Empty()** method of the query result, not by fetching a selection (`Query.Execute().Select()`) and testing it in a condition. Fetching the selection (or unloading the result to a value table) takes extra time.

## Noncompliant Code Example

```bsl
Selection = Query.Execute().Select();
If Selection.Next() Then
    Return True;
Else
    Return False;
EndIf;
```

## Compliant Solution

```bsl
Return Not Query.Execute().Empty();
```

## See

- [Standard 438. Checking for an empty query result](https://its.1c.ru/db/v8std#content:438:hdoc)
- Ported from the APK check `АПК_00205`.
