# Duplicate condition in "If" statement

Checks that conditions of the **If ... Then ... ElsIf** statement are not duplicated: a duplicated condition is never checked, its branch is unreachable.

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
