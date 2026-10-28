# Unreachable code after loop exit or goto

Statements after `Break`, `Continue` and `Goto` can never execute. Built on the same control flow graph as `all-function-path-must-have-return` (per method during validation, no cross-validation cache). Defaults: enabled, MINOR severity, ERROR issue type, no parameters.

Scope decision (variant B): statements after `Return` and `Raise` and fully terminal `If` chains are intentionally NOT reported here — the platform check `bsl-legacy-check-method-for-statements-after-return` and `up-1068-unreachable-code` already mark them; a third marker would only duplicate. Preprocessor branches are conservatively treated as all-possible paths, so dead code inside inactive-looking branches is not reported. Contexts with syntax errors are skipped.

Basis: pinned BSL Language Server UnreachableCodeDiagnostic (control-flow part); full parity with its statement ranges and preprocessor range handling is not claimed in this version — one marker is placed per unreachable statement group.
