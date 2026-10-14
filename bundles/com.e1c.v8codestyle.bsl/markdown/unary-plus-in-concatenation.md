# Unary plus in concatenation

Reports a unary plus whose preceding non-comment, non-whitespace token is another plus, except when its operand is a numeric literal. No string type inference is required, matching the pinned BSL LS algorithm. Methods and module-level statements are checked. The issue highlights only the unary plus. Enabled by default, CRITICAL/ERROR (LS BLOCKER mapped to CRITICAL); no parameters or quick fix.

```bsl
Result = "First" + + "Second"; // reported on the second plus
Result = A + + B;             // reported without type inference
Result = A + + 5;             // numeric exception
Result = A + (+ B);           // preceding token is '(', not '+'
```

## See

- [BSL Language Server: UnaryPlusInConcatenation](https://1c-syntax.github.io/bsl-language-server/diagnostics/UnaryPlusInConcatenation/)
