# Temporary directory usage audit

Opt-in **SECURITY / MAJOR** audit of global `TempFilesDir()` /
`КаталогВременныхФайлов()` calls, case-insensitive. No parameters. The marker
targets the method name. Member calls, strings, comments and similarly named
methods are excluded, matching the pinned BSL LS scope.

Review temporary-file ownership, access, concurrency and cleanup. Prefer an
appropriate managed temporary-file API (for example the project's BSP facilities)
where available; this check does not require BSP or infer that every call is unsafe.
It is not a data-flow or file-deletion check and provides no automatic fix.

Unlike LS (CODE_SMELL, enabled by default), this fork uses SECURITY and explicit
opt-in, as an audit policy. Existing file-deletion and synchronous-call checks
remain independent; none are disabled automatically.

References: [BSL LS](https://v8std.ru/diagnostics/bslls/TempFilesDir/),
[standard 542](https://v8std.ru/std/542/).
