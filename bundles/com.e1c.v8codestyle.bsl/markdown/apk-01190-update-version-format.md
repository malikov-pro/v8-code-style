# Invalid version format in the infobase update handler

The version assigned to a handler in the **OnAddUpdateHandlers** (ПриДобавленииОбработчиковОбновления) procedure shall be either `*` (run on every infobase update) or a version number in the **«M.m.v.b»** or **«M.m.v»** format: at least three dot-separated segments, each segment contains digits only.

An empty version is not reported (the standard requires the `InitialFill = True` property in that case, which is out of scope of this check).

## Noncompliant Code Example

```bsl
Procedure OnAddUpdateHandlers(Handlers)

    Handler = Handlers.Add();
    Handler.Version = "2.1";      // only two segments
    Handler.Version = "2.1.Х";    // non-digit segment

EndProcedure
```

## Compliant Solution

```bsl
Procedure OnAddUpdateHandlers(Handlers)

    Handler = Handlers.Add();
    Handler.Version = "2.1.3.1";
    Handler.Version = "*";

EndProcedure
```

## See

- [Standard 690. Infobase update handlers (SSL)](https://its.1c.ru/db/v8std#content:690:hdoc)
- Ported from the APK check `АПК_01190`.
