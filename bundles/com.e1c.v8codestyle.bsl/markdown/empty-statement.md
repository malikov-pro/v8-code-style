# Empty statement

Checks that the module has no empty statements `;`. An extra semicolon is noise that makes code harder to read.

## Noncompliant Code Example

```bsl
Procedure Calculate(Parameter)

    Parameter = Parameter + 1;
    ;
    Parameter = Parameter * 2;

EndProcedure
```

## Compliant Solution

```bsl
Procedure Calculate(Parameter)

    Parameter = Parameter + 1;
    Parameter = Parameter * 2;

EndProcedure
```

## See

- [BSL Language Server: EmptyStatement](https://1c-syntax.github.io/bsl-language-server/diagnostics/EmptyStatement/)
- Ported from the BSL Language Server diagnostic `EmptyStatement`.
