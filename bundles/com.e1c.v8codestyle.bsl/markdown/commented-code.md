# Commented out code fragment

Checks that program modules have no commented out code fragments. The place for unused code is in the version control system, not in comments.

Adjacent comment lines are grouped; a group is reported if it is recognized as code (the heuristic is ported from BSL Language Server; the `threshold` parameter, default 0.9). Method documentation headers are not reported; the `exclusionPrefixes` parameter excludes comments starting with given prefixes.

The quick fix deletes the commented out group.

## Noncompliant Code Example

```bsl
//А = 1;
//Б = 2;
//Сообщить(А + Б);
```

## Compliant Solution

Delete the commented out code (it stays in the version control history).

## See

- [BSL Language Server: CommentedCode](https://1c-syntax.github.io/bsl-language-server/diagnostics/CommentedCode/)
- Ported from the BSL Language Server diagnostic `CommentedCode`.
