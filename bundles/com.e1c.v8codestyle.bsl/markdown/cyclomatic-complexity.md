# Cyclomatic complexity of method is too high

Checks cyclomatic complexity of methods and module-level code. Defaults: `complexityThreshold=20`, `checkModuleBody=true`. Methods start at one, module-level code at zero. If/ElsIf/Else, loops, Except, ternaries, Goto, direct recursive calls and each And/Or operation add one. Try, Return and ordinary calls add nothing. No reachability analysis is performed.

Method issues highlight the method name; module-body issues highlight the first statement token. Numeric regression tests include the adapted pinned LS computer corpus. Secondary locations are not provided; full differential parity, especially nested grammar/preprocessor edge cases, is not certified. Split complex code into smaller methods. No quick fix is provided.

## See

- [BSL Language Server: CyclomaticComplexity](https://1c-syntax.github.io/bsl-language-server/diagnostics/CyclomaticComplexity/)
- Ported from the BSL Language Server diagnostic `CyclomaticComplexity`.
