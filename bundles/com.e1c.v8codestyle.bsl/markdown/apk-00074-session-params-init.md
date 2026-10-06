# Session parameters shall be initialized in the session module

Checks that session parameters (`SessionParameters.Name`) are **assigned** only in the session module. Initializing a session parameter in any other module is reported. Reading a session parameter is allowed everywhere and is not reported.

## Noncompliant Code Example

```bsl
// Common module
Procedure InitializeSessionParameters() Export
    SessionParameters.CurrentUser = Undefined; // assignment outside the session module
EndProcedure
```

## Compliant Solution

```bsl
// Session module
Procedure InitializeParameters()
    SessionParameters.CurrentUser = Undefined;
EndProcedure
```

Session parameters are intended to store values for each client session while it runs. Initializing them in the session module guarantees they are set when the session starts; the values are recommended for use in queries and data access restriction conditions of the current session.

## See

- [Standard 413. Using session parameters](https://its.1c.ru/db/v8std#content:413:hdoc)
- Ported from the APK check `АПК_00074`.
