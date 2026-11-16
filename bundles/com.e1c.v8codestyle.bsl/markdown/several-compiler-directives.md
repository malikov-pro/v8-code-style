# Several compiler directives

A method or a module variable can have one compiler directive: `&AtClient`,
`&AtServer`, `&AtServerNoContext`, `&AtClientAtServerNoContext` (the Russian
variants behave the same). A second directive is a contradiction and the
platform rejects or silently picks one of them.

Extension annotations (`&Around`, `&Before`, `&After`, `&ChangeAndValidate`) are
a different pragma kind: they take the overridden method name and are not
counted. Module variables and methods are checked separately, so the
module-variable and module-method structure checks are not duplicated.

## Noncompliant Code Example

```bsl
&AtServer
&AtServerNoContext
Procedure FillDocument()
EndProcedure
```

## Compliant Solution

```bsl
&AtServerNoContext
Procedure FillDocument()
EndProcedure
```

## See

- [1C:Enterprise modules and compiler directives](https://its.1c.ru/db/v8std#content:439:hdoc)
- BSL Language Server diagnostic `SeveralCompilerDirectives`
