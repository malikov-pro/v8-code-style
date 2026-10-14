# Duplicate string literal

Groups raw quoted string literals, preserving whitespace within strings. Defaults: `allowedNumberCopies=2`, `analyzeFile=false`, `caseSensitive=false`, `minTextLength=5` including quotes. Copy limits below 1 reset to 2; minimum length is clamped to 5, as in LS. With file analysis disabled, each method (including parameter defaults) and module-body block is analyzed separately; file analysis groups the entire module. Reports the first literal once per excessive group. MINOR/WARNING, enabled by default; no quick fix. Related occurrence locations from LS are not provided.

## See

- [BSL LS DuplicateStringLiteral](https://1c-syntax.github.io/bsl-language-server/diagnostics/DuplicateStringLiteral/)
