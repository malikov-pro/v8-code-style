# Incorrect use of StrTemplate

Checks known templates passed to the global StrTemplate call. Supports `%1` through `%10`, parenthesized indexes such as `%(1)45`, escaped percent pairs, NStr's first argument and literals assigned in earlier sibling statements. Unknown dynamic templates and object methods are ignored. Every supplied argument must have a substitution index, and every referenced index must have an argument. Empty literal templates are skipped, as in pinned LS. Enabled by default, CRITICAL/ERROR (LS BLOCKER mapped to CRITICAL). No parameters or quick fix.

Previous assignments are searched only within the same statement list, not across branches or methods. Property names are matched by the complete access text: unlike pinned LS's root-identifier lookup, `Object.A` does not take a template from `Object.B`. The lookup is lexical, not a reference index or full constant-propagation analysis; it searches earlier literal assignments and does not invalidate them for intervening dynamic assignments. This limitation and the property-lookup difference prevent a claim of full LS equivalence.

```bsl
Result = StrTemplate("%1 %2", A); // missing argument
Result = StrTemplate("%11", A);  // invalid index
Result = StrTemplate("%(1)45", A); // valid
Result = StrTemplate("%%1"); // escaped, no argument needed
```

## See

- [BSL Language Server: IncorrectUseOfStrTemplate](https://1c-syntax.github.io/bsl-language-server/diagnostics/IncorrectUseOfStrTemplate/)
