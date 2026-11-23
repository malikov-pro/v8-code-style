# Transferring parameters between the client and the server

Enabled by default, CODE_STYLE / MAJOR. Port of the BSL LS diagnostic
`TransferringParametersBetweenClientAndServer`.

When control passes from the client to the server and back, copies of the
parameters are transferred. A client method calling a server method copies every
by-reference parameter back to the client on return. If the server method never
assigns such a parameter, the return copy is pure overhead: the parameter should
be declared with the `Знач`/`ByValue` modifier.

The check reports the actual parameter of a call when all of the following hold:

- the calling method runs only in a client context;
- the target method runs only in a server context;
- the matching formal parameter has no `Знач`/`ByValue` modifier;
- the server method never assigns the parameter in its body.

The port reports the parameter at the call site (the actual parameter), while
the LS reports the parameter definition. Only direct calls with a resolvable
target method are analyzed.

## Example

```bsl
&AtClient
Procedure ClientCall()

    Value = 1;
    ServerModule.ServerProcedure(Value); // reported: Value is not assigned on the server

EndProcedure

&AtServer
Procedure ServerProcedure(Value) Export

    Message(Value);

EndProcedure
```

Fix the parameter with `Знач`/`ByValue` when the server does not return it:

```bsl
&AtServer
Procedure ServerProcedure(Знач Value) Export
```

## Limitations

- Assignment detection walks only the target method body and compares the name of
  a plain variable reference on the left side of `Assignment`. Assignments through
  a property or an index (`Parameter.Field = ...`, `Parameter[0] = ...`) are not
  recognized, so a parameter modified only that way is reported (partial scope).
- Calls through a variable and unresolved/indirect calls are skipped silently.
- The method context comes from `Environmental.environments()`; compilation
  directives of form-module methods are used only as a fallback.
- The LS parameter `cachedValueNames` is not implemented.
- Extension projects (configuration extensions) are not analyzed.

## References

- [BSL LS diagnostic](https://1c-syntax.github.io/bsl-language-server/diagnostics/TransferringParametersBetweenClientAndServer/)
- [1C:ITS — Call with transfer of control from the client to the server](https://its.1c.ru/db/v8318doc#bookmark:dev:TI000000153)
