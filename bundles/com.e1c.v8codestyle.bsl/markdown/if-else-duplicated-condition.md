# Duplicate condition in "If" statement

Checks that conditions of the **If ... Then ... ElsIf** statement are not duplicated: a duplicated condition is never checked, its branch is unreachable.

Compares significant tokens: comments and whitespace outside strings are ignored, identifiers ignore case, string contents remain exact. Reports one issue at the first condition of each duplicate group, as in LS, including module-level If statements. Related duplicate locations from LS are not provided. The comparison retains source parentheses and dialect token spelling; it is not a complete port of LS grammar-tree equivalence.

## Noncompliant Code Example

```bsl
If ErrorCode = 1 Then
    DoReconnect();
ElsIf ErrorCode = 1 Then
    DoStop();
EndIf;
```

## Compliant Solution

```bsl
If ErrorCode = 1 Then
    DoReconnect();
ElsIf ErrorCode = 2 Then
    DoStop();
EndIf;
```

## See

- [BSL Language Server: IfElseDuplicatedCondition](https://1c-syntax.github.io/bsl-language-server/diagnostics/IfElseDuplicatedCondition/)
- Ported from the BSL Language Server diagnostic `IfElseDuplicatedCondition`.
