# Function returns the same primitive

Checks functions with more than one Return whose expressions are textually identical and contain no variable/property/call accesses. No reachability analysis or constant folding is performed. `skipAttachable=true` skips Attachable_/Подключаемый_ functions; `caseSensitiveForString=false` compares string text without case. Ordinary functions are covered, not only cached modules as in APK00350. Highlights the function name. MAJOR/ERROR, enabled by default; no quick fix. LS related return locations are not provided. A missing return expression is conservatively excluded instead of LS's nullable-expression shortcut.

## See

- [BSL LS FunctionReturnsSamePrimitive](https://1c-syntax.github.io/bsl-language-server/diagnostics/FunctionReturnsSamePrimitive/)
