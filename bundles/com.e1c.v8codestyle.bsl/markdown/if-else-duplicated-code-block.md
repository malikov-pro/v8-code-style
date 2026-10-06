# Duplicated code blocks in "If" statement

Checks that the **If ... Then ... ElsIf** statement has no duplicated code blocks. A duplicated branch is a probable copy-paste error or a sign of a redundant condition.

Blocks are compared by text (case-insensitive, whitespace-insensitive). Two empty blocks are not treated as duplicates.

## Noncompliant Code Example

```bsl
If ErrorCode = 1 Then
    WriteToLog(ErrorCode);
    Notify(ErrorCode);
ElsIf ErrorCode = 2 Then
    WriteToLog(ErrorCode);
    Notify(ErrorCode);
EndIf;
```

## Compliant Solution

```bsl
If ErrorCode = 1 Then
    WriteToLog(ErrorCode);
    Notify(ErrorCode);
ElsIf ErrorCode = 2 Then
    DoStop();
EndIf;
```

## See

- [BSL Language Server: IfElseDuplicatedCodeBlock](https://1c-syntax.github.io/bsl-language-server/diagnostics/IfElseDuplicatedCodeBlock/)
- Ported from the BSL Language Server diagnostic `IfElseDuplicatedCodeBlock`.
