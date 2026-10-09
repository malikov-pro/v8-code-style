# Non-existent procedure in the notification handler parameter

Checks that the notification handler name passed as the first parameter of the `NotifyChanged` (`ОповеститьОбИзменении`) call resolves to an existing export procedure or function of a server common module of the configuration.

The platform allows to specify server procedures as notification handlers. A name that resolves to nothing means a lost notification: the platform silently ignores the call, so the error is found only by testing.

## Noncompliant Code Example

```bsl
// The handler does not exist in any server common module (a typo).
ОповеститьОбИзменении("ПриИзминенииДанных", Данные);

// The named common module does not exist.
ОповеститьОбИзменении("ОбщегоНазначенияНесуществующий.ПриИзмененииДанных", Данные);
```

## Compliant Solution

```bsl
// An export procedure of a server common module.
ОповеститьОбИзменении("ОбменДаннымиСервер.ПриИзмененииДанных", Данные);

// The object form of the call needs no handler.
ОповеститьОбИзменении(ЭтотОбъект);
```

The handler name may be simple (`ProcedureName`) or qualified (`ModuleName.ProcedureName`); the comparison is case-insensitive. Only a string literal in the first parameter is checked: a variable or an expression is not resolved. The check is not applied to modules adopted in extensions, where the handler may be located in the parent configuration.

## See also

- [Standard 467. General configuration requirements](https://its.1c.ru/db/v8std#content:467:hdoc)
- Ported from the upstream issue [1C-Company/v8-code-style#781](https://github.com/1C-Company/v8-code-style/issues/781).
- The **Notify description to Server procedure** check deals with the related case of the `NotifyDescription` constructor pointing to an existing server procedure that is unavailable in the web client.
