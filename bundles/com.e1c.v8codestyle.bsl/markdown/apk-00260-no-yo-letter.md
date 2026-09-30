# Letter "ё" in module texts

Checks that the letter **ё** (in any case) is not used in module texts: in code, string literals and comments. Each occurrence is reported as a separate issue.

## Noncompliant Code Example

```bsl
// The word "still" must not be written with "ё"
Procedure OnBeforeWrite(ChangeObject)
    
    Message = "Fresh numbers";
    
EndProcedure
```

## Compliant Solution

```bsl
// The word "still" must not be written with "yo"
Procedure OnBeforeWrite(ChangeObject)
    
    Message = "Fresh numbers";
    
EndProcedure
```

## See

- [Standard 598. Refusing to use the letter "ё" in module texts](https://its.1c.ru/db/v8std#content:598:hdoc)
- Ported from the APK check `АПК_00260`.
