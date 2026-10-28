# Double negatives

Reports NOT applied directly to inequality (`<>`) or another NOT using the EDT AST.

## Noncompliant

```bsl
If Not Value <> Undefined Then
EndIf;
Result = Not (Not Flag);
```

## Compliant

```bsl
If Value = Undefined Then
EndIf;
Result = Flag;
```

As in LS, NOT applied to equality or to an AND/OR expression is not reported.
Strings, comments and incomplete expressions with syntax errors are skipped.
EDT already reports consecutive NOT without parentheses as a syntax error.
The check covers procedure/function bodies, not module-level statements.
No quick fix is offered: rewriting a condition requires the author's decision.

## References

- [BSL Language Server: DoubleNegatives](https://1c-syntax.github.io/bsl-language-server/diagnostics/DoubleNegatives/)
- [Remove double negative](https://www.refactoring.com/catalog/removeDoubleNegative.html)
