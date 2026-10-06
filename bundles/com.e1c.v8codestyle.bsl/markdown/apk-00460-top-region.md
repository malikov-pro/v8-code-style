# Top-level region other than "Public" in overridable common module

Checks that an overridable common module (its name contains
**"Переопределяемый"/"Override"**, case-insensitive) has no top-level regions
except **«ПрограммныйИнтерфейс»** ("Public"). Each not allowed region is
reported as a separate issue.

The whole code of an overridable module is placed in the
«ПрограммныйИнтерфейс»/"Public" region; nested regions (inside other regions)
are allowed.

Adopted (borrowed) common modules of extension projects are not checked.

## Noncompliant Code Example

```bsl
#Region Public
...
#EndRegion

#Region Private
// A top-level region is not allowed in an overridable module
...
#EndRegion
```

## Compliant Solution

```bsl
#Region Public

Procedure DefineSettings(Settings) Export
    ...
EndProcedure

#EndRegion
```

## See

- [Standard 554. Overriding common modules in the library hierarchy](https://its.1c.ru/db/v8std#content:554:hdoc)
- Ported from the APK check `АПК_00460` (sub-item 7: top-level regions).
