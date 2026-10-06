# Magic date

Checks the code for magic dates (literals like '20240101'). The empty date, authorized dates (parameter), method parameter defaults, the right side of a simple assignment and the Return expression are not reported (as in BSL LS). The check has no quick fix.

## See

- [BSL Language Server: MagicDate](https://1c-syntax.github.io/bsl-language-server/diagnostics/MagicDate/)
- Ported from the BSL Language Server diagnostic `MagicDate`.
