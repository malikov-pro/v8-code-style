# The "For calls from other subsystems" region is not inside the "Public" region

By standard 644 (clause 2.2) the
**«ДляВызоваИзДругихПодсистем»** (English **«InterfaceImplementation»**)
region must be nested inside the **«ПрограммныйИнтерфейс»** (English
**«Public»**) region. A top-level region or a region nested into any
other region is reported on the region name.

Both the Russian and the English canonical region names are recognized,
the case is ignored. All module types are checked; modules adopted in
extension projects are not checked.

## Incorrect

```bsl
#Область ДляВызоваИзДругихПодсистем
// верхнеуровневая область: нет области «ПрограммныйИнтерфейс»
...
#КонецОбласти
```

```bsl
#Область СлужебныеПроцедурыИФункции

#Область ДляВызоваИзДругихПодсистем
// вложена в служебную область, а не в «ПрограммныйИнтерфейс»
...
#КонецОбласти

#КонецОбласти
```

## Correct

```bsl
#Область ПрограммныйИнтерфейс

#Область ДляВызоваИзДругихПодсистем
...
#КонецОбласти

#КонецОбласти
```

## See also

- [Standard 644. Ensuring library compatibility (clause 2.2)](https://its.1c.ru/db/v8std#content:644:hdoc:2.2)
- Port of the upstream issue [1C-Company/v8-code-style#632](https://github.com/1C-Company/v8-code-style/issues/632) (APK rule 473).
