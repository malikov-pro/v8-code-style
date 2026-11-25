# Letter "ё" in the module code

Checks that the letter **ё** (in any case) is not used in the module code: in identifiers and string literals. Each occurrence is reported as a separate issue. Line comments are not checked: the letter "ё" is allowed there.

## Noncompliant Code Example

```bsl
Procedure OnBeforeWrite(ChangeObject)
    
    Message = "Ёлка возле дома";
    Ёлочка = Message;
    
EndProcedure
```

## Compliant Solution

```bsl
// A comment with the letter ё is allowed: comments are not checked
Procedure OnBeforeWrite(ChangeObject)
    
    Message = "Елка возле дома";
    Елочка = Message;
    
EndProcedure
```

## See

- [Standard 598. Refusing to use the letter "ё" in module texts](https://its.1c.ru/db/v8std#content:598:hdoc)
- Ported from the APK check `АПК_00260`.
- Comments are excluded from the check: the rule applies to the code only.
