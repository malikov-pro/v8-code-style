# Redundant comparison with Boolean

Reports `=` or `<>` when one operand is a Boolean literal and the other is proven solely Boolean by EDT TypesComputer, including comparisons of two Boolean constants. Unknown, unresolved and union types are skipped. Both operand orders, methods and module-level statements are checked. Highlights the complete comparison. MINOR/WARNING, enabled by default; no parameters or quick fix. Types must be computed in validation context; an unlinked BM snapshot can return empty type sets.

## See

- [BSL LS CompareWithBoolean](https://1c-syntax.github.io/bsl-language-server/diagnostics/CompareWithBoolean/)
