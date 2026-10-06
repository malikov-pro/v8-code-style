# Space at the start of comment

Checks that a comment text is separated from `//` by a space: it makes comments easier to read.

Not reported (as in BSL Language Server): comment annotations (the `commentsAnnotation` parameter, by default `//@,//(c),//©`) and comments that look like commented out code.

The quick fix inserts the space after `//`.

## Noncompliant Code Example

```bsl
//ОшибкаБезПробела
```

## Compliant Solution

```bsl
// Ошибка без пробела
```

## See

- [BSL Language Server: SpaceAtStartComment](https://1c-syntax.github.io/bsl-language-server/diagnostics/SpaceAtStartComment/)
- Ported from the BSL Language Server diagnostic `SpaceAtStartComment`.
