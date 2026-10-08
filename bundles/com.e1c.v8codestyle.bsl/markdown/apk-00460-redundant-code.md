# Redundant code in overridable common module procedure

Checks that a procedure of an overridable common module (its name contains
**"Переопределяемый"/"Override"**, case-insensitive) that overrides a
same-named method of another common module consists only of calls of the
overridden method: each statement has the form `Module.Method(...)`.
Assignments, conditions, loops and local calls are redundant: the overriding
procedure must be reduced to the call of the original. One issue per procedure
— the first redundant statement.

Calls of the forms from the APK algorithm are recognized: the direct call
`Module.Method(...)`, the implicit call
`Common.CommonModule("Module").Method(...)` (рус. `ОбщегоНазначения.ОбщийМодуль(...)`)
and a call via a variable assigned with `Common.CommonModule("Module")` (the
assignment statement itself is redundant code, as in the APK). Code between
the comments `// _Демо начало примера` and `// _Демо конец примера` is not
checked.

Simplification against the APK algorithm: the procedure is checked only when a
"pair" method exists — a method with the same name in another common module of
the configuration; the APK subsystem filter is not reproduced. Deprecated
procedures are not checked, as in the APK. Adopted (borrowed) common modules
of extension projects are not checked. The check is disabled by default —
enable it for configurations developed on top of a library hierarchy.

## Noncompliant Code Example

```bsl
#Region Public

Procedure BeforeWrite(DataObject, WriteParameters) Export

    // Redundant: extra logic in the overriding procedure
    WriteParameters.Insert("Changed", True);
    BaseLibraryModule.BeforeWrite(DataObject, WriteParameters);

EndProcedure

#EndRegion
```

## Compliant Solution

```bsl
#Region Public

Procedure BeforeWrite(DataObject, WriteParameters) Export

    BaseLibraryModule.BeforeWrite(DataObject, WriteParameters);

EndProcedure

#EndRegion
```

## See

- [Standard 554. Overriding common modules in the library hierarchy](https://its.1c.ru/db/v8std#content:554:hdoc)
- Ported from the APK check `АПК_00460` (sub-item 1: only calls in overriding procedures).
