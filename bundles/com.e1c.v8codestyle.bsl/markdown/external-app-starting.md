# External application start audit

An opt-in SECURITY/MAJOR audit of global and member calls, following the pinned
BSL Language Server ExternalAppStarting diagnostic. The marker highlights the
method name, not its arguments or receiver. No quick fix is provided.

`userPatternString` is a Java regex matched against the full method name,
case-insensitively in RU/EN. Its default includes System/RunSystem/RunApp,
BeginRunningApplication/RunAppAsync and their Russian names, plus
ЗапуститьПрограмму, ОткрытьПроводник and ОткрытьФайл.

`checkGotoUrl` defaults to false. When true, the literal alternatives
ПерейтиПоНавигационнойСсылке, GotoURL and ОткрытьНавигационнуюСсылку are appended
to the custom regex exactly as in LS. An empty custom regex matches no names
unless navigation is enabled. An invalid combined regex logs a warning and
skips this check for that validation pass; it does not fall back to defaults.

The receiver is deliberately NOT resolved: both
`ФайловаяСистема.ЗапуститьПрограмму(...)` and
`MyModule.ЗапуститьПрограмму(...)` match. A user-defined method with the same
name can therefore produce a false positive. A marker asks you to review the
call; it proves neither an actual external launch nor a vulnerability. Adapt
the regex to the project when necessary. Comments, strings and method references
without an invocation are excluded. Methods and module-level code are checked.
Other security and non-recommended-method checks remain independent; no settings
are changed or automatically deduplicated. This check never executes BSL code.

Source: [BSL Language Server: ExternalAppStarting](https://1c-syntax.github.io/bsl-language-server/en/diagnostics/ExternalAppStarting/).
