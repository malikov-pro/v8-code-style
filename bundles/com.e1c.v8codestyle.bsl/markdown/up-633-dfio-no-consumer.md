# No consumer subsystem specified in the "For calls from other subsystems" region

Checks that each export method (procedure or function) located inside the
**«ДляВызоваИзДругихПодсистем»** (English **«InterfaceImplementation»**)
region, including nested regions, has a consumer marking comment above its
declaration: a line like `// Subsystem1.Subsystem2` naming the consuming
subsystems the method is intended for (a comma-separated list and one
trailing dot are allowed).

A method with no comment or with an ordinary text comment is reported on
the method name. Non-export methods and methods outside this region are
not checked. Modules adopted in extension projects are not checked.

## Incorrect

```bsl
#Область ПрограммныйИнтерфейс

#Область ДляВызоваИзДругихПодсистем

// нет отметки подсистемы-потребителя
Процедура ВыгрузитьДанные() Экспорт
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
// Процедура выгружает данные за период.
Процедура ВыгрузитьДанные() Экспорт
    ...
КонецПроцедуры

#КонецОбласти

#КонецОбласти
```

## See also

- [Standard 644. Ensuring library compatibility (clause 2.2)](https://its.1c.ru/db/v8std#content:644:hdoc:2.2)
- Port of the upstream issue [1C-Company/v8-code-style#633](https://github.com/1C-Company/v8-code-style/issues/633) (APK rule 474).
