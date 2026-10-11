# SUM of a numeric literal in a DCS query

Use `COUNT(*)` or `COUNT(Field)` to count records instead of `SUM(1)`.
The default SUM numeric precision can overflow for large record counts.

This APK_01193 port covers direct numeric literals in DCS query data sets
only. It preserves the source's exact-zero spelling exception: `SUM(0)`
is clean; `SUM(0.0)`, `SUM(+0)` and `SUM(-0)` are reported. This exception
is textual, not numeric. Fields, arithmetic expressions and `CAST` operands
are excluded. Query strings and comments are not scanned; `SUM(DISTINCT 1)`
is excluded as in the source's nonnumeric argument test.

Module queries and dynamic lists are skipped to avoid overlap with APK_01192.
This AST implementation deliberately excludes text-search false positives
and malformed numeric arguments; it is not a full emulation of `Число()`.
No quick fix: counting records and summing values have different meanings.

## See

- [Standard 787](https://its.1c.ru/db/v8std#content:787:hdoc)
