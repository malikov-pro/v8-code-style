# Wrong use of ProceedWithCall()

`ProceedWithCall()` (`ПродолжитьВызов()`) continues the original method of an
extension. The platform has a call point for it only in a method annotated with
`&Вместо`/`&Around`; anywhere else the call fails at run time.

`&ИзменениеИКонтроль`/`&ChangeAndValidate` is the annotation for a method where
the original is continued automatically (platform 8.3.16+), so `ПродолжитьВызов`
must not be called there either.

The check reports a global `ПродолжитьВызов`/`ProceedWithCall` call when the
enclosing method does not have exactly one `&Вместо` annotation. A call outside
any method is not reported, as in the pinned BSL Language Server.

## Noncompliant Code Example

```bsl
&After("MyFunction")
Procedure Ext_MyFunction()
    ProceedWithCall(); // run-time error: no original call point here
EndProcedure
```

## Compliant Solution

```bsl
&Around("MyFunction")
Function Ext_MyFunction()
    Return ProceedWithCall();
EndFunction
```

## See

- [1C:Enterprise extensions, modules](https://its.1c.ru/db/pubextensions#content:54:1)
- BSL Language Server diagnostic `WrongUseFunctionProceedWithCall`
