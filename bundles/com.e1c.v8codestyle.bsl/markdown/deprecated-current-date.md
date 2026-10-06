# Using of the deprecated method "CurrentDate"

Checks the use of the deprecated method `CurrentDate`: the current time may differ on the client and the server, which leads to unpredictable behavior. Use `CurrentSessionDate` instead — the session time zone is unambiguous.

This check **replaces** `CurrentDate` in the default list of `use-non-recommended-method` (the coarse check no longer reports it by default) and raises the severity: MAJOR/ERROR instead of MINOR/CODE_STYLE.

The check has no quick fix: the semantics of time changes (session vs client/server) — the decision is up to the user.

## Noncompliant Code Example

```bsl
Moment = CurrentDate();
```

## Compliant Solution

```bsl
Moment = CurrentSessionDate();
```

## See

- [BSL Language Server: DeprecatedCurrentDate](https://1c-syntax.github.io/bsl-language-server/diagnostics/DeprecatedCurrentDate/)
- Ported from the BSL Language Server diagnostic `DeprecatedCurrentDate`.
