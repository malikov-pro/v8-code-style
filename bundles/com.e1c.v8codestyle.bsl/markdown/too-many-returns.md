# Too many returns

Counts all return statements in a function or procedure, including nested and unreachable returns. A method exceeding `maxReturnsCount` (default 3) has one issue on its name. No control-flow analysis is performed. MINOR/WARNING, disabled by default as in LS. No quick fix; LS related return locations are not provided.

## See

- [BSL LS TooManyReturns](https://1c-syntax.github.io/bsl-language-server/diagnostics/TooManyReturns/)
