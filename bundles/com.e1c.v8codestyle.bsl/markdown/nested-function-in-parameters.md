# Initialization of method and constructor parameters by calling nested methods

Enabled by default, CODE_STYLE / MINOR. Port of the BSL LS diagnostic
`NestedFunctionInParameters`.

Nested calls in actual parameters make a call hard to read: the reader has to
evaluate the inner call first and keep its result in mind. The check reports the
outer call.

## What is reported

A call is reported when all of the following hold:

- the call expression spans more than one line (a call written on one line is
  never reported);
- the call has actual parameters;
- the parameters contain a nested call that is not allowed: any call with a
  receiver (`Object.Method(...)`), a constructor with at least one non-empty
  argument (`New Array(...)`, `Новый Массив(...)`), or a global function whose
  name is not in `allowedMethodNames`;
- at least one actual parameter itself spans more than one line. With
  `allowOneliner=false` this last condition is dropped.

The marker is placed on the method name (or on the constructor type name), as in
the LS.

## Parameters

| Name | Type | Default | Meaning |
|---|---|---|---|
| `allowOneliner` | Boolean | `true` | Allow oneliner expression: when `true`, a nested call is reported only if some parameter spans several lines; when `false`, any nested call in a multi-line call is reported |
| `allowedMethodNames` | String | `НСтр,NStr,ПредопределенноеЗначение,PredefinedValue` | Comma-separated names of global functions allowed to initialize parameters (compared case-insensitively) |

## Example

```bsl
Procedure Fill(Object, Ref, Collection)

    // reported: the parameter spans two lines and contains a nested call
    Object.Fill(Object.GetValue(
        Ref));

    // not reported: everything is on one line
    Object.Fill(Collection, Object.GetValue(Ref));

    // not reported: NStr is whitelisted
    Object.Fill(NStr(
        "ru = 'Text'"));

EndProcedure
```

## Limitations

- A constructor with the type given by an expression, `Новый(Тип("..."), ...)`,
  is not analyzed: the EDT model keeps it without a typed argument list.
- The constructor type name is taken from the written token, so an aliased type
  is reported as written.
- Only the parameter list of a call is inspected; expressions of `Return`,
  assignments and conditions are out of scope, as in the LS.

## References

- [BSL LS diagnostic](https://1c-syntax.github.io/bsl-language-server/diagnostics/NestedFunctionInParameters/)
