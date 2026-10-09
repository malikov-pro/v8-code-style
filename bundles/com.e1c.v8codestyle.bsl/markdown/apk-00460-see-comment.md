# No "See Module.Method" comment on the called method

Checks that the method of a library common module called from an overriding
procedure of an overridable common module (its name contains
**"Переопределяемый"/"Override"**, case-insensitive) has the reference comment
`См. <OverridableModule>.<Procedure>.` above its declaration (blank lines
between the comment and the declaration are allowed). After normalization
(without "/", spaces and line breaks) the comment must start with
`См.<Module>.<Method>`; the case of "См." and the names is significant, as in
the APK algorithm.

Calls of the forms from the APK algorithm are recognized: the direct call
`Module.Method(...)`, the implicit call
`Common.CommonModule("Module").Method(...)` (рус.
`ОбщегоНазначения.ОбщийМодуль(...)`) and a call via a variable assigned with
`Common.CommonModule("Module")`. Calls of methods with a name other than the
overriding procedure name are not checked here (see
apk-00460-procedure-name-match).

Deviations from the APK algorithm: the issue is reported on the overriding
procedure (the APK reports it on the called module — cross-resource markers
are not used); the procedure is checked only when a "pair" method exists — a
method with the same name in another common module of the configuration
(global search via IV8ProjectManager); the APK subsystem filter is not
reproduced. Adopted (borrowed) common modules of extension projects are not
checked. The check is disabled by default — enable it for configurations
developed on top of a library hierarchy.

## Noncompliant Code Example

```bsl
#Region Public

// The library method has no reference comment
Procedure BeforeWrite(DataObject, WriteParameters) Export

EndProcedure

#EndRegion
```

## Compliant Solution

```bsl
#Region Public

// See OverridableModule.BeforeWrite.
Procedure BeforeWrite(DataObject, WriteParameters) Export

EndProcedure

#EndRegion
```

## See

- [Standard 554. Overriding common modules in the library hierarchy](https://its.1c.ru/db/v8std#content:554:hdoc)
- Ported from the APK check `АПК_00460` (sub-item 4: the "See Module.Method" comment).
