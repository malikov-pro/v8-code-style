# Reserved exception category

Checks the second argument of functional `Raise` when it resolves to an ErrorCategory member. The pinned LS contract forbids AllErrors, OtherError, ScriptCompileError, ScriptRuntimeError and ScriptRaisedException (and their Russian names). Does not infer values through variables or report unknown or shadowed members. A parenthesized expression is excluded, matching the LS last-token guard.

Enabled by default, TRIVIAL/CODE_STYLE, no parameters. No quick fix: choosing an application category requires intent. Related locations are not supplied. EDT names the last category `ExceptionRaisedFromScript`, not the pinned LS `ScriptRaisedException`; this spelling discrepancy is not silently treated as an additional forbidden spelling.

## See

- [BSL LS BadExceptionCategory](https://1c-syntax.github.io/bsl-language-server/diagnostics/BadExceptionCategory/)
