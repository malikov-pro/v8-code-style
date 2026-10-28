# Not all function paths return a value

Function contains a `Return` statement but can still reach the end of its body without returning — the result is implicitly `Undefined` on that path. Port of pinned BSL Language Server diagnostic AllFunctionPathMustHaveReturn: built on a control flow graph over the method body, defaults MAJOR/CODE_STYLE, enabled.

Function without a single `Return` is intentionally not reported here — it stays to the simpler `function-should-have-return` check (same deduplication as in LS). Functions with incomplete syntax (no `EndFunction`) and contexts with syntax errors are skipped. An endless `While True` loop does not create a fall-through exit. Preprocessor branches are conservatively treated as all-possible paths.

Parameters (LS defaults): `loopsExecutedAtLeastOnce=true` — a loop body is considered executed, so exiting the loop by condition is not a fall-through; `ignoreMissingElseOnExit=false` — an `ElsIf` chain without `Else` reaching the end of the function is reported.

The marker is placed on the function name. Related offender locations of the LS diagnostic are not transferred in this version. Graph is built per method during validation; no cross-validation cache.
