# Unused For Each iterator

Checks For Each loops in methods and module-level statements. The local iterator must be referenced inside the body; reads, assignments and property receivers count. Strings, comments and a same-named called method do not count. Module/global variables are exempt. CRITICAL/ERROR, enabled by default; no parameters or quick fix. Symbol resolution is used when available; unresolved names are compared lexically. Nested loop definitions alone do not use the outer iterator.

## See

- [BSL LS UseLessForEach](https://1c-syntax.github.io/bsl-language-server/diagnostics/UseLessForEach/)
