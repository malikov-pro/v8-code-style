# Inserting a collection into itself

Checks that a collection is not inserted (added) into itself: inserting a collection into itself leads to circular references.

## Noncompliant Code Example

```bsl
List.Add(List);
```

## Compliant Solution

```bsl
List.Add(Item);
```

## See

- [BSL Language Server: SelfInsertion](https://1c-syntax.github.io/bsl-language-server/diagnostics/SelfInsertion/)
- Ported from the BSL Language Server diagnostic `SelfInsertion`.
