# File system access audit

An opt-in SECURITY/MAJOR audit of global file-system calls and constructors,
following the pinned BSL Language Server FileSystemAccess diagnostic. It marks
places to review, not proven vulnerabilities. No quick fix is provided.
The check does not execute code or access files.

Parameters `globalMethods` and `newExpression` are Java regular expressions:
full name match, case-insensitive for RU/EN. Defaults retain the entire LS lists,
including asynchronous file APIs and legacy types. Member calls are excluded,
even if their name matches `globalMethods`. Call markers highlight the method
name; constructor markers highlight the whole expression, in methods and module code.

Dynamic `New("File")` follows the shared pinned syntax: the first string member
of the first argument supplies the type name, with surrounding spaces stripped.
`New("File" + Suffix)` is reported, a parenthesized string and a variable type
name are not evaluated. This is not constant evaluation or data-flow analysis.

An empty regex matches no nonempty names. An invalid regex logs a warning and
disables only its own branch for that validation pass; the other branch still works.

`TempFilesDir` is intentionally retained in `globalMethods`. Enabling both this
audit and `temp-files-dir` can produce two messages on the same call. There is
no automatic deduplication or settings modification; enable only the broader
audit if you do not need the narrower message. File cleanup, synchronous-call
and APK file-operation checks have independent invariants.

Source: [BSL Language Server: FileSystemAccess](https://1c-syntax.github.io/bsl-language-server/en/diagnostics/FileSystemAccess/).
