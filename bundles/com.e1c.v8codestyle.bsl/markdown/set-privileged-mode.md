# Privileged mode usage audit

Opt-in security audit, **MAJOR**, no parameters. Reviews calls named
`SetPrivilegedMode` / `УстановитьПривилегированныйРежим`, including calls on
arbitrary receivers. A matching name is not proof that the callee is a platform method.

The pinned BSL LS exception applies only to a global call with one argument whose
first unparenthesized member is `False` (unary operators do not change the LS test).
Thus `False`, `Not False`, `False Or True` and `False = True` are skipped; `(False)`
is audited. Member calls are audited even with `False`. This is a syntactic LS
compatibility rule, **not** constant evaluation or a guarantee that privilege is disabled.

Review the need and scope of privilege. No quick fix: changing privilege can alter
access control. Unlike LS (enabled by default), the fork requires explicit opt-in.
Existing password-storage checks are independent and are not disabled automatically.

References: [BSL LS](https://v8std.ru/diagnostics/bslls/SetPrivilegedMode/),
[standard 485](https://v8std.ru/std/485/), [standard 678](https://v8std.ru/std/678/).
