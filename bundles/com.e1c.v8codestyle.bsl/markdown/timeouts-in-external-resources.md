# Timeouts when working with external resources

Checks that a timeout is specified when creating `FTPConnection`, `HTTPConnection`, `WSDefinitions`, `WSProxy` and `InternetMailProfile`: either the timeout constructor parameter is filled (with a number or a variable), or the `Timeout` property is assigned to the variable below with a number or a variable. Without a timeout, the session may hang indefinitely.

Parameter indexes are the same as in BSL Language Server: WSDefinitions — 4, FTPConnection — 6, others — 5. The `analyzeInternetMailProfileZeroTimeout` parameter enables analysis of `InternetMailProfile` (enabled by default). The check has no quick fix.

## Noncompliant Code Example

```bsl
Connection = New HTTPConnection("server");
```

## Compliant Solution

```bsl
Connection = New HTTPConnection("server", , , , , 60);
```

## See

- [BSL Language Server: TimeoutsInExternalResources](https://1c-syntax.github.io/bsl-language-server/diagnostics/TimeoutsInExternalResources/)
- Ported from the BSL Language Server diagnostic `TimeoutsInExternalResources`.
