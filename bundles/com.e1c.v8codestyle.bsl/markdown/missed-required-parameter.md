# Empty required argument

Complements EDT's native argument-count validation. Reports explicitly empty argument positions such as `Calculate(, 2)` when binding resolves to a user function/procedure whose corresponding parameter has no default value. Empty optional arguments and an explicit Undefined value are not violations. Unresolved or ambiguous methods are skipped; names alone do not establish a signature. One issue lists the empty required parameters in declaration order.

Enabled by default, MAJOR/ERROR, no parameters or quick fix. This is a partial implementation of LS MissedRequiredParameter: missing trailing arguments and constructors/platform signatures are left to native validation and are not independently reported. A call with too few arguments is skipped entirely to avoid duplicate native arity issues, even when it also contains empty positions. Related locations are not supplied. Cross-module signature changes must revalidate the caller without editing its source.

## See

- [BSL LS MissedRequiredParameter](https://1c-syntax.github.io/bsl-language-server/diagnostics/MissedRequiredParameter/)
