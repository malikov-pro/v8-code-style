# Too many nested statements

Checks the nesting depth of If, loops and Try statements: deep nesting makes code hard to read (parameter, default 4, as in BSL LS). Statements deeper than the maximum are reported. The check has no quick fix.

## See

- [BSL Language Server: NestedStatements](https://1c-syntax.github.io/bsl-language-server/diagnostics/NestedStatements/)
- Ported from the BSL Language Server diagnostic `NestedStatements`.
