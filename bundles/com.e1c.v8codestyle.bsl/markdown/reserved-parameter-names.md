# Reserved parameter names

Checks formal parameters of functions and procedures against `reservedWords`, a case-insensitive regular expression. The empty default has no effect. The mask is trimmed and anchored as in pinned LS (`^` + mask + `$`); use grouping for alternatives. Invalid regex is ignored with a warning rather than aborting validation. Enabled by default, MAJOR/WARNING; no quick fix. This configurable mask is not a fixed platform-reserved-word list.

## See

- [BSL LS ReservedParameterNames](https://1c-syntax.github.io/bsl-language-server/diagnostics/ReservedParameterNames/)
