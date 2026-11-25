# Missing space

Checks for spaces around operators (=, <, >, <=, >=, <>, %, +, -, *, /) and after comma and semicolon. A unary minus or plus in front of a number does not need a space (`-5`, `Method(-5)` are not reported). Comments and string literals are not analyzed; commas inside a string literal are not checked either. Arithmetic signs are checked on the left side only (as in BSL LS by default). The check has no quick fix.

## See

- [BSL Language Server: MissingSpace](https://1c-syntax.github.io/bsl-language-server/diagnostics/MissingSpace/)
- Ported from the BSL Language Server diagnostic `MissingSpace`.
