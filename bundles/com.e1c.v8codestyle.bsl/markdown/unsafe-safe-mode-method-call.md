# Unsafe Boolean use of SafeMode

SafeMode() may return a security profile name, not only Boolean. Implicit Boolean use in If/ElsIf, logical expressions or unary operations is unsafe. Compare explicitly, for example SafeMode() <> False. Receiver calls and explicit comparisons are excluded; a plain assignment or call argument alone is not a violation. CRITICAL/ERROR (LS BLOCKER mapping), enabled by default, no parameters or quick fix. Applies from compatibility 8.3.1. This is separate from disabling safe mode.

## See

- [BSL LS UnsafeSafeModeMethodCall](https://1c-syntax.github.io/bsl-language-server/diagnostics/UnsafeSafeModeMethodCall/)
