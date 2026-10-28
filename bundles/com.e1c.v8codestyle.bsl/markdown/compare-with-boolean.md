# Redundant comparison with Boolean

Reports `=` or `<>` when one operand is a Boolean literal and the other is proven solely Boolean by EDT TypesComputer, including comparisons of two Boolean constants. Unknown, unresolved and union types are skipped. Both operand orders, methods and module-level statements are checked. Highlights the complete comparison. MINOR/WARNING, enabled by default; no parameters or quick fix. Types must be computed in validation context; an unlinked BM snapshot can return empty type sets.

Regression coverage includes typed receiver calls, string and unknown properties,
and Boolean-to-union return-type changes in local and exported common-module
functions. The cross-module test changes only the provider and verifies the
consumer file's content and modification stamp remain unchanged. This relies on
EDT's existing type inference and dependency validation; it is not a separate
inference engine. Metadata-object properties, extensions and full LS engine
differential equivalence remain outside the certified corpus.

## See

- [BSL LS CompareWithBoolean](https://1c-syntax.github.io/bsl-language-server/diagnostics/CompareWithBoolean/)
