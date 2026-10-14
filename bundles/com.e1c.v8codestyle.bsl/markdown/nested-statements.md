# Too many nested statements

Checks the nesting depth of If, While, For, For Each and Try statements in methods and module-level code. The `maxAllowedLevel` parameter defaults to 4; the outermost compound statement has depth 1. Reports only terminal compound blocks deeper than the limit, not all their ancestors. Sibling deep blocks are checked separately. A label does not increase depth or hide its statement. The issue highlights the opening keyword. No control-flow analysis or quick fix is provided.

The related parent-location list supplied by LS is not provided. Preprocessor and other grammar edge cases have not been certified against the full LS corpus; this remains a partial implementation.

## See

- [BSL Language Server: NestedStatements](https://1c-syntax.github.io/bsl-language-server/diagnostics/NestedStatements/)
- Ported from the BSL Language Server diagnostic `NestedStatements`.
