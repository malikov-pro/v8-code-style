# Rewrite method parameter

Checks that a parameter passed by value (`Val`) is not rewritten before its first use. The passed value is lost, which is probably an error.

Modification like `Parameter = Parameter + 1` is not a rewrite (the parameter is read); parameters passed by reference (without `Val`) are not checked — as in BSL Language Server. The marker is placed on the first rewrite of the parameter.

Reads in call arguments, conditions, loop expressions, direct assignment right-hand sides and property receivers count as use. A called method's name is not a read of a same-named parameter. Identity assignments (`Parameter = Parameter`) neither use nor overwrite the original value: a later `Parameter = 12` can still be reported.

The check follows source order, not control-flow reachability. It checks direct assignments, not loop iterator definitions. Unlike LS, it has no reference index or related-reference list; the marker covers the assignment statement rather than only the identifier. These are remaining limitations, not full LS equivalence.

## Noncompliant Code Example

```bsl
Procedure FillOrder(Val Order, Val UseDiscount)
    Order = Undefined; // the passed value is lost
    If UseDiscount Then
        ApplyDiscount(Order);
    EndIf;
EndProcedure
```

## Compliant Solution

```bsl
Procedure FillOrder(Order, Val UseDiscount)
    If UseDiscount Then
        ApplyDiscount(Order);
    EndIf;
EndProcedure
```

## See

- [BSL Language Server: RewriteMethodParameter](https://1c-syntax.github.io/bsl-language-server/diagnostics/RewriteMethodParameter/)
- Ported from the BSL Language Server diagnostic `RewriteMethodParameter`.
