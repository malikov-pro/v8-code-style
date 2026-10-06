# Complex condition in If statement

Checks that a condition of If/ElsIf has no more logical And/Or operations than the maximum (parameter, default 3, as in BSL LS). Split a complex condition into intermediate variables or separate statements. The check has no quick fix.

## See

- [BSL Language Server: IfConditionComplexity](https://1c-syntax.github.io/bsl-language-server/diagnostics/IfConditionComplexity/)
- Ported from the BSL Language Server diagnostic `IfConditionComplexity`.
