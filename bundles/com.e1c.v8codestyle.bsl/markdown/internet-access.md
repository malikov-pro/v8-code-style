# Internet resource usage audit

An opt-in SECURITY/MAJOR audit of constructors for `FTPConnection`,
`HTTPConnection`, `WSDefinitions`, `WSProxy`, `InternetMailProfile`, `InternetMail`,
`Mail`, `HTTPRequest` and `InternetProxy`, including their Russian names.
There are no parameters or quick fixes. Review the need for network access,
destination trust and handling of transferred data. A marker is an audit point,
not proof of a vulnerability or of an actual network connection.

Both explicit constructors and dynamic `New("InternetMail")` are checked in
methods and module-level code. Names are matched fully and case-insensitively.
Following pinned BSL Language Server syntax, the first string member of the first
dynamic argument supplies the name (surrounding spaces stripped):
`New("InternetMail" + Suffix)` is reported, while a parenthesized string or
a variable type name is not evaluated. The whole constructor is highlighted.
Legacy `Mail` remains in scope for LS compatibility, even when unavailable in
the selected platform execution context.

Timeout and hardcoded-address checks have different invariants and remain
independent. Enabling this audit does not disable them: two different messages
on one constructor can be appropriate. No automatic deduplication is performed.

Source: [BSL Language Server: InternetAccess](https://1c-syntax.github.io/bsl-language-server/en/diagnostics/InternetAccess/).
