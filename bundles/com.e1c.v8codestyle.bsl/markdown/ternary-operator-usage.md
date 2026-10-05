# Ternary operator usage

The `If-Then-Else` statement is recommended instead of the ternary operator `?(Condition, A, B)`: it is easier to read and debug.

The check is **disabled by default** (as in BSL Language Server); enable it in the check settings.

## Noncompliant Code Example

```bsl
Discount = ?(IsRegularCustomer, 10, 0);
```

## Compliant Solution

```bsl
If IsRegularCustomer Then
    Discount = 10;
Else
    Discount = 0;
EndIf;
```

## See

- [BSL Language Server: TernaryOperatorUsage](https://1c-syntax.github.io/bsl-language-server/diagnostics/TernaryOperatorUsage/)
- Ported from the BSL Language Server diagnostic `TernaryOperatorUsage`.
