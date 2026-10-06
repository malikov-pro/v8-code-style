# Non-deprecated function in overridable common module

Checks that an overridable common module (its name contains
**"Переопределяемый"/"Override"**, case-insensitive) has no functions except
deprecated ones. Each non-deprecated function is reported as a separate issue.

A function in an overridable module is never called by the override mechanism:
only procedures are overridden. Deprecated functions are allowed for backward
compatibility.

A function is considered deprecated when:

- it is marked with the **"Устарела."** (English **"Deprecated."**) comment;
- it is placed in the **«УстаревшиеПроцедурыИФункции»** ("Deprecated") region.

Adopted (borrowed) common modules of extension projects are not checked.

## Noncompliant Code Example

```bsl
#Region Public

// The function in an overridable module is never called
Function FillData(Parameters) Export
    Return Undefined;
EndFunction

#EndRegion
```

## Compliant Solution

```bsl
#Region Public

// Deprecated. Use the FillData procedure instead.
Function FillData(Parameters) Export
    Return Undefined;
EndFunction

Procedure FillDataProc(Parameters) Export
    ...
EndProcedure

#EndRegion
```

## See

- [Standard 554. Overriding common modules in the library hierarchy](https://its.1c.ru/db/v8std#content:554:hdoc)
- Ported from the APK check `АПК_00460` (sub-item 5: non-deprecated functions).
