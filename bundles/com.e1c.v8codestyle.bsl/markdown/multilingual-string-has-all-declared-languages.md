# NStr must contain all declared languages

Enabled by default, ERROR / MINOR. Parameter `declaredLanguages` defaults to `ru`.
The explicit comma-separated list is independent of configuration languages;
whitespace is removed, case and repeated entries are preserved as in pinned LS.
Language keys are extracted lexically, not by runtime execution or full NStr
format validation. Invalid formats can also be reported by the existing NStr check.

The marker targets the whole global NStr call. Strings supplied through variables
and member methods are excluded. Syntax-recovery calls are skipped.
Calls used inside StrTemplate or assigned to a variable later passed as a whole
argument to StrTemplate in the same block are excluded. The lookup requires a
later line and does not track reassignment, reachability or variable identity.

The existing `bsl-nstr-string-literal-format` and its `checkEmptyInterface` setting
remain unchanged. Enabling that project-language audit may produce additional
markers. No automatic disabling, translation generation or quick fix is performed.

## References

- [BSL LS diagnostic](https://1c-syntax.github.io/bsl-language-server/diagnostics/MultilingualStringHasAllDeclaredLanguages/)
- [Localization requirements](https://its.1c.ru/db/v8std#content:763:hdoc)
