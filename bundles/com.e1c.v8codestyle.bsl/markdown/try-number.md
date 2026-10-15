# Numeric conversion inside Try

Reports global Number / Число calls in Try bodies, including nested blocks. A direct call in Except is excluded unless inside another outer Try body. Receiver calls are excluded. No type or data-flow analysis. MAJOR/CODE_STYLE, enabled by default; no parameters or quick fix. Source names are matched as in LS; user-defined same-name methods are not resolved separately.

## See

- [BSL LS TryNumber](https://1c-syntax.github.io/bsl-language-server/diagnostics/TryNumber/)
