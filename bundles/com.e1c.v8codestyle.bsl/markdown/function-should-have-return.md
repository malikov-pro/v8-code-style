# Function should have return

Checks that a function contains at least one **Return** statement. A function without "Return" implicitly returns **Undefined**, which is usually a bug: such method should either return a value or be rewritten as a procedure. The 1C:Enterprise compiler does not diagnose this case.

## Noncompliant Code Example

```bsl
Function SumBeforeLimit(ItemAmount)
    
    If ItemAmount > Limit Then
        DoSomething(ItemAmount);
    EndIf;
    
EndFunction
```

## Compliant Solution

```bsl
Function SumBeforeLimit(ItemAmount)
    
    Result = 0;
    
    If ItemAmount > Limit Then
        DoSomething(ItemAmount);
    Else
        Result = ItemAmount;
    EndIf;
    
    Return Result;
    
EndFunction
```

## See

- [BSL Language Server: FunctionShouldHaveReturn](https://1c-syntax.github.io/bsl-language-server/diagnostics/FunctionShouldHaveReturn/)
- Ported from the BSL Language Server diagnostic `FunctionShouldHaveReturn`.
