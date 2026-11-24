# Non-recommended words in a common module name

Common module names should describe their purpose. Avoid generic words such as
`Procedures`, `Functions`, `Handlers`, `Module`, and `Functionality`, including
their Russian equivalents.

## Parameters

`words` is a case-insensitive regular expression searched **anywhere within the name**.
Default: `процедуры|procedures|функции|functions|обработчики|handlers|модуль|module|функциональность|functionality`.
An empty expression matches every name. An invalid expression logs a WARNING
and skips this check; it does not disable other checks.

## Scope and compatibility

Enabled by default, CODE_STYLE / TRIVIAL. The marker targets the metadata name,
even when no BSL source exists. Common module flags do not affect this rule.
Initially only base configurations are checked; extension projects are excluded.
Vendor support restrictions are preserved.

The name predicate follows BSL Language Server `CommonModuleNameWords`. Unlike
LS delivery, this metadata check does not require a nonempty BSL selection range.
Invalid regex handling is also intentionally different: WARNING and skip.
Other independent naming rules may report their own issues. No automatic
renaming or disabling of other checks is performed.

## References

- [BSL Language Server: CommonModuleNameWords](https://1c-syntax.github.io/bsl-language-server/diagnostics/CommonModuleNameWords/)
- [Common modules](https://its.1c.ru/db/v8std#content:467:hdoc)
