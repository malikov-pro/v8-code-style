# Too many nested statements

Checks the nesting depth of If, While, For, For Each and Try statements in methods and module-level code. The `maxAllowedLevel` parameter defaults to 4; the outermost compound statement has depth 1. Reports only terminal compound blocks deeper than the limit, not all their ancestors. Sibling deep blocks are checked separately. A label does not increase depth or hide its statement. The issue highlights the opening keyword. No control-flow analysis or quick fix is provided.

Preprocessor If and Region statement containers are transparent to runtime nesting:
their branches and following statements are traversed without adding a level.
Regression cases cover deep blocks across a preprocessor boundary, sibling branches,
regions and statements following the directives; keyword positions and incremental
removal/restoration are checked. Both source branches are analyzed, not only the
branch active in one execution environment.

The related parent-location list supplied by LS is not provided. The complete
preprocessor and grammar corpus has not been certified against LS; this remains
a partial implementation.

## See

- [BSL Language Server: NestedStatements](https://1c-syntax.github.io/bsl-language-server/diagnostics/NestedStatements/)
- Ported from the BSL Language Server diagnostic `NestedStatements`.
