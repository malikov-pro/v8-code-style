# Method parameter of a form module is named "Parameters"

Checks that no method of a form module declares a parameter named
**"Parameters"** (or the Russian **«Параметры»**, case-insensitive).

In a form module the form parameters are accessed via the `Parameters`
(«Параметры») property. A method parameter with the same name shadows this
property inside the method body and may lead to implicit errors that are hard
to detect. Rename the parameter.

Each such parameter is reported once.

## Noncompliant Code Example

```bsl
&AtServer
Procedure FillData(Parameters)
    // The parameter name conflicts with the form Parameters property
    FormAttribute = Parameters.SourceAttribute;
EndProcedure
```

## Compliant Solution

```bsl
&AtServer
Procedure FillData(FillParameters)
    FormAttribute = FillParameters.SourceAttribute;
EndProcedure
```

## See

- [Upstream issue 1C-Company/v8-code-style#1353](https://github.com/1C-Company/v8-code-style/issues/1353)
