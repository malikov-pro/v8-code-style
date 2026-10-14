# Cognitive complexity of method is too high

Checks cognitive complexity of methods and module-level code, with `complexityThreshold=15` and `checkModuleBody=true` by default. If, loops, Except and ternaries add one plus their nesting depth; ElsIf and Else add one and introduce nesting for their contents. Try itself does not increase complexity or nesting. Goto and direct recursive calls add one. Each series of the same And/Or operator adds one; changing the operator or passing through a negated parenthesized logical expression starts a new series. Call arguments are separate expressions. No control-flow graph is needed.

Method issues highlight the method name; module-body issues highlight its first statement token. The adapted pinned LS metric corpus is checked numerically, including logical-series and nesting cases. LS's secondary-location list is not provided; the full differential corpus (including preprocessor/receiver edge cases) has not been certified. Split complex code into smaller methods. No quick fix is provided.

## See

- [BSL Language Server: CognitiveComplexity](https://1c-syntax.github.io/bsl-language-server/diagnostics/CognitiveComplexity/)
- Ported from the BSL Language Server diagnostic `CognitiveComplexity`.
