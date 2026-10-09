# Non-existent subsystem in a comment of the "For calls from other subsystems" region

Checks consumer marking comments inside the
**«ДляВызоваИзДругихПодсистем»** (English **«InterfaceImplementation»**)
region: every marking comment like `// Subsystem1.Subsystem2` must name
subsystems that exist in the configuration.

A multi-segment path is resolved hierarchically from a top-level subsystem
to a nested one, the case is ignored, several comma-separated consumers are
allowed. A path that does not resolve to an existing subsystem is reported
on the comment.

Simplifications: a single name matching no subsystem is not reported (such
a line is indistinguishable from an ordinary text comment); closing markers
`// End ...` are not markings; if the configuration has no subsystems,
nothing is reported. Modules adopted in extension projects are not checked.

## Incorrect

```bsl
#Область ПрограммныйИнтерфейс

#Область ДляВызоваИзДругихПодсистем

// СтандартныеПодсистемы.ГрупповоеИзменениеОбектов // subsystem name has a typo
Процедура ПечатьДокумента() Экспорт
    ...
КонецПроцедуры

#КонецОбласти

#КонецОбласти
```

## Correct

```bsl
#Область ПрограммныйИнтерфейс

#Область ДляВызоваИзДругихПодсистем

// СтандартныеПодсистемы.ГрупповоеИзменениеОбъектов
Процедура ПечатьДокумента() Экспорт
    ...
КонецПроцедуры

#КонецОбласти

#КонецОбласти
```

## See also

- [Standard 644. Ensuring library compatibility (clause 2.2)](https://its.1c.ru/db/v8std#content:644:hdoc:2.2)
- Port of the upstream issue [1C-Company/v8-code-style#634](https://github.com/1C-Company/v8-code-style/issues/634) (APK rule 475).
