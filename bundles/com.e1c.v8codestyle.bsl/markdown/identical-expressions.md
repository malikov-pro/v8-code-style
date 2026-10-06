# Identical expressions on the left and right of the operator

Checks binary expressions with identical left and right operands, for example `A = A` or `A = 1 And A = 1`. Usually this is an error: the operator does not do what the author expected.

Allowed (as in BSL Language Server):

- repeating an operand in addition and multiplication (`A + A` is doubling);
- division by popular divisors — the `popularDivisors` parameter, by default `60, 1024` (scaling of time and bytes).

Compared by text (case-insensitive, whitespace-insensitive); chains like `A = B Or A <> B` are not analyzed.

## Noncompliant Code Example

```bsl
If A = A Then
    Result = True;
EndIf;
Result = A = 1 And A = 1;
```

## Compliant Solution

```bsl
If A = 1 Then
    Result = True;
EndIf;
Result = A = 1;
```

## See

- [BSL Language Server: IdenticalExpressions](https://1c-syntax.github.io/bsl-language-server/diagnostics/IdenticalExpressions/)
- Ported from the BSL Language Server diagnostic `IdenticalExpressions`.
