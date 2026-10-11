# Complex condition in If statement

Checks the number of conditions in If/ElsIf: the number of logical And/Or operations plus one, as in BSL LS. The `maxIfConditionComplexity` parameter defaults to 3: three conditions joined by two operations are allowed; four conditions joined by three operations are reported. Parentheses and Not do not increase the count. Split a complex condition into intermediate variables or separate statements. The check has no quick fix.

## See

- [BSL Language Server: IfConditionComplexity](https://1c-syntax.github.io/bsl-language-server/diagnostics/IfConditionComplexity/)
- Ported from the BSL Language Server diagnostic `IfConditionComplexity`.
