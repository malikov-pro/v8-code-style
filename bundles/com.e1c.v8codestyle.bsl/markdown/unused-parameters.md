# Unused method parameter

Checks that all method parameters are used in the method body. An unused parameter is misleading: callers must pass it for no reason (as in BSL LS). The check has no quick fix: removing a parameter changes the signature and the callers.

## See

- [BSL Language Server: UnusedParameters](https://1c-syntax.github.io/bsl-language-server/diagnostics/UnusedParameters/)
- Ported from the BSL Language Server diagnostic `UnusedParameters`.
