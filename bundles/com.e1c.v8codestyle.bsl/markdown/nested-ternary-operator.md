# Nested ternary operator

Nested ternary operators and ternary operators in `If`/`ElsIf` conditions are hard to read and easy to get wrong. Use `If-Then-Else` statements and intermediate variables instead.

Reported:

- a ternary operator inside another ternary operator (all but the outermost);
- any ternary operator inside a condition of `If`/`ElsIf`.

## Noncompliant Code Example

```bsl
Result = ?(Flag, ?(Value > 0, 1, 2), 3);
If ?(Flag, True, False) Then
    DoWork();
EndIf;
```

## Compliant Solution

```bsl
If Flag Then
    If Value > 0 Then
        Result = 1;
    Else
        Result = 2;
    EndIf;
Else
    Result = 3;
EndIf;
```

## See

- [BSL Language Server: NestedTernaryOperator](https://1c-syntax.github.io/bsl-language-server/diagnostics/NestedTernaryOperator/)
- Ported from the BSL Language Server diagnostic `NestedTernaryOperator`.
