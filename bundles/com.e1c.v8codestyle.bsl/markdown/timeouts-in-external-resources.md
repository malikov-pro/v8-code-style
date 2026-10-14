# Timeouts when working with external resources

Checks that a timeout is specified when creating `FTPConnection`, `HTTPConnection`, `WSDefinitions`, `WSProxy` and `InternetMailProfile`: either the timeout constructor parameter is filled, or the `Timeout` property is assigned to the same variable on a later line. Numbers, variable/property accesses and calls are accepted; strings, booleans and omitted arguments are not. Without a timeout, the session may hang indefinitely.

Parameter indexes are the same as in BSL Language Server: WSDefinitions — 4, FTPConnection — 6, others — 5. The `analyzeInternetMailProfileZeroTimeout` parameter enables analysis of `InternetMailProfile` (enabled by default). The check has no quick fix.

Indexes are zero-based and correspond to LS's modern/default compatibility mode; index shifts for compatibility below 8.3.7/8.3.9 are not implemented. The whole method, including nested statements, is searched; a conditional setter can suppress the issue. This is not control-flow analysis. Module-level statements and arbitrary arithmetic timeout expressions remain outside this implementation's scope. A setter on the creation line is not counted.

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
