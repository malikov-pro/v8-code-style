# One statement per line

Checks that a line contains a single statement. Multiple statements on one line make code harder to read and debug (a breakpoint can not be placed on a specific statement).

Preprocessor instructions and empty statements (`;`) are not counted; each statement of the line is reported — as in BSL Language Server. The quick fix moves the statement to a new line (it is not available for the first statement of a line).

## Noncompliant Code Example

```bsl
Procedure Fill()
    A = 1; B = 2;
EndProcedure
```

## Compliant Solution

```bsl
Procedure Fill()
    A = 1;
    B = 2;
EndProcedure
```

## See

- [BSL Language Server: OneStatementPerLine](https://1c-syntax.github.io/bsl-language-server/diagnostics/OneStatementPerLine/)
- Ported from the BSL Language Server diagnostic `OneStatementPerLine`.
