# Meaningless methods in common module with return value reuse

Checks common modules with return value reuse (cached): an **export procedure** has no effect there because procedures return no values, and a **function consisting of a single `Return <constant>` statement** returns data that is computed faster than fetching it from the cache. Such constants (string, number, boolean, date) and predefined items (`Catalogs.X.Y` and similar) needlessly fill the cache.

## Noncompliant Code Example

```bsl
// Common module with return value reuse
Procedure ManagementPackageName() Export
    
EndProcedure

Function ManagementPackage() Export
    Return "ManagementPackage";
EndFunction
```

## Compliant Solution

```bsl
// Common module with return value reuse
Function ApplicationName() Export
    Return ThisObject.ApplicationName; // computed value, not a constant
EndFunction
```

Move constant return values out of cached common modules, or make the function return a value that is actually expensive to compute (database or external data source queries, resource-intensive calculations).

## See

- [Standard 724. Using common modules with return value reuse](https://its.1c.ru/db/v8std#content:724:hdoc)
- Ported from the APK check `АПК_00350`.
