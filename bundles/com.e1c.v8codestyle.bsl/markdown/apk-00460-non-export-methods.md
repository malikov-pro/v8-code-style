# Non-export method in overridable common module

Checks that an overridable common module (its name contains
**"Переопределяемый"/"Override"**, case-insensitive) has no non-export methods:
an overridable module may contain only export methods that override the library
methods. Each non-export method is reported as a separate issue.

A non-export method of an overridable module is not accessible to the override
mechanism and is never called.

Adopted (borrowed) common modules of extension projects are not checked.

## Noncompliant Code Example

```bsl
#Region Public

// The non-export method is never called
Procedure FillRecordSets(RecordSets)
    ...
EndProcedure

#EndRegion
```

## Compliant Solution

```bsl
#Region Public

Procedure FillRecordSets(RecordSets) Export
    ...
EndProcedure

#EndRegion
```

## See

- [Standard 554. Overriding common modules in the library hierarchy](https://its.1c.ru/db/v8std#content:554:hdoc)
- Ported from the APK check `АПК_00460` (sub-item 6: non-export methods).
