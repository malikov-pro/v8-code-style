# Output parameters in functions

Reports the first direct assignment to each by-reference function parameter, including nested statements. By-value parameters, procedures, property/index changes and calls that may modify an argument are not checked. The assignment target is highlighted. MAJOR/WARNING, disabled by default as in LS; no parameters or quick fix. Unlike RewriteMethodParameter, this is about by-reference function results, not rewriting a by-value argument before its first read. No reachability analysis is performed.

## See

- [BSL LS FunctionOutParameter](https://1c-syntax.github.io/bsl-language-server/diagnostics/FunctionOutParameter/)
