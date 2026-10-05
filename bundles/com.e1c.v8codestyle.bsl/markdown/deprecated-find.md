# Using of the deprecated method "Find"

Checks the use of the deprecated global method `Find`. Use `StrFind` instead: it has clear argument semantics and more search capabilities.

Object methods (`Object.Find`) are not checked — as in BSL Language Server. This check **replaces** `Find` in the default list of `use-non-recommended-method` (the coarse check no longer reports it by default).

The check has no quick fix: `Find` and `StrFind` have different argument order.

## Noncompliant Code Example

```bsl
Position = Find("Hello, world", "world");
```

## Compliant Solution

```bsl
Position = StrFind("Hello, world", "world");
```

## See

- [BSL Language Server: DeprecatedFind](https://1c-syntax.github.io/bsl-language-server/diagnostics/DeprecatedFind/)
- Ported from the BSL Language Server diagnostic `DeprecatedFind`.
