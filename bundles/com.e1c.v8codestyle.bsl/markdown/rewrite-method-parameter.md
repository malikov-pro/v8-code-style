# Rewrite method parameter

Checks that a parameter passed by value (`Val`) is not rewritten before its first use. The passed value is lost, which is probably an error.

Modification like `Parameter = Parameter + 1` is not a rewrite (the parameter is read); parameters passed by reference (without `Val`) are not checked — as in BSL Language Server. The marker is placed on the first rewrite of the parameter.

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
