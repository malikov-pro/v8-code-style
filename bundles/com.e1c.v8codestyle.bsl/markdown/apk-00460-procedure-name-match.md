# Called method name differs from overriding procedure name

Checks that a procedure of an overridable common module (its name contains
**"Переопределяемый"/"Override"**, case-insensitive) that has a same-named
pair method in another common module calls exactly that pair: the called
method name equals the overriding procedure name. A call of a differently
named method of a common module is reported — each call as a separate issue.

Calls of the forms from the APK algorithm are recognized: the direct call
`Module.Method(...)`, the implicit call
`Common.CommonModule("Module").Method(...)` (рус.
`ОбщегоНазначения.ОбщийМодуль(...)`) and a call via a variable assigned with
`Common.CommonModule("Module")`. Code between the comments
`// _Демо начало примера` and `// _Демо конец примера` is not checked.

Simplification against the APK algorithm: the procedure is checked only when a
"pair" method exists — a method with the same name in another common module of
the configuration (global search via IV8ProjectManager); the APK subsystem
filter is not reproduced. Deprecated procedures are not checked, as in the
APK. Adopted (borrowed) common modules of extension projects are not checked.
The check is disabled by default — enable it for configurations developed on
top of a library hierarchy.

## Noncompliant Code Example

```bsl
#Region Public

Procedure BeforeWrite(DataObject, WriteParameters) Export

    // Wrong method: the overriding procedure must call its same-named pair
    BaseLibraryModule.WriteObject(DataObject, WriteParameters);

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
- Ported from the APK check `АПК_00460` (sub-item 2: called procedure name).
