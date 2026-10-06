# Cognitive complexity of method is too high

Checks the cognitive complexity of a method: each If/ElsIf/loop/Try structure adds 1 plus its nesting depth, each And/Or adds 1 (simplified port of BSL LS, threshold default 15). Split a complex method into several. The check has no quick fix.

## See

- [BSL Language Server: CognitiveComplexity](https://1c-syntax.github.io/bsl-language-server/diagnostics/CognitiveComplexity/)
- Ported from the BSL Language Server diagnostic `CognitiveComplexity`.
