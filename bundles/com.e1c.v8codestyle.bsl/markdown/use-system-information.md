# System information usage audit

An opt-in security audit of `SystemInfo` / `СистемнаяИнформация` constructors.
The default severity is CRITICAL; the check has no parameters or quick fix.

Review whether collecting operating-system and hardware information is needed
and whether the collected information is exposed to untrusted users.
The check identifies an audit point, not a proven vulnerability.

Explicit constructors and dynamic `New("SystemInfo")` are checked in methods
and in module-level code. Matching follows the pinned BSL Language Server syntax:
the first string member of the first dynamic argument is used, with surrounding
spaces stripped. Thus `New("SystemInfo" + Suffix)` is also reported; parentheses
around the string and a variable type name are not evaluated. The whole constructor
expression is highlighted. Names are matched case-insensitively in both languages.

Source: [BSL Language Server: UseSystemInformation](https://1c-syntax.github.io/bsl-language-server/en/diagnostics/UseSystemInformation/).
