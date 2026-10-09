# Unreachable code

Statements placed after `Return` (рус. «Возврат») or `Raise` (рус. «ВызватьИсключение») in the same block can never be executed: control always leaves the block at those statements. Such code usually remains after editing "someone else's code" and signals an error in the algorithm — correct the algorithm or delete the dead statements.

A block is a method body, an `If/ElseIf/Else` branch, a loop body, or a `Try/Except` section. One issue is reported per block, on its first unreachable statement. If all branches of an `If` (If + all ElseIf + Else) terminate with `Return` or `Raise` (including a nested fully terminating `If`), the statements after the `EndIf` in the same block are unreachable as well.

## Noncompliant Code Example

```bsl
Function Example(Parameter1, Parameter2)
    If Error Then
        Raise "Error occurred";
        Parameter1 = Parameter2;   // never executed
    EndIf;
    Return Parameter1;
EndFunction
```

## Compliant Solution

```bsl
Function Example(Parameter1, Parameter2)
    If Error Then
        Raise "Error occurred";
    EndIf;
    Return Parameter1;
EndFunction
```

## Sources

Simplified (lite) implementation of the unreachable statement check requested in the upstream issue [1C-Company/v8-code-style#1068](https://github.com/1C-Company/v8-code-style/issues/1068); a neighbor of the BSL Language Server diagnostic `UnreachableCode`.

The lite check does not build a control flow graph: statements after `Break` (рус. «Прервать»), `Continue` (рус. «Продолжить») and `Goto` (рус. «Перейти») are not detected, neither are dead conditions and module-level (non-method) code. No quick fix is provided: correcting the algorithm requires a user decision.

## See

- [BSL Language Server: UnreachableCode](https://1c-syntax.github.io/bsl-language-server/diagnostics/UnreachableCode/)
