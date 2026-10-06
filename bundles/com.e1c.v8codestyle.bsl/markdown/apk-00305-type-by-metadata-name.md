# Type of a variable value should be determined by comparing with a type

Checks that the type of a variable value is determined by comparing the value type with a type, not by a metadata name. Determining the type by a metadata name (or full name) is unreliable and may lead to errors.

## Noncompliant Code Example

```bsl
If Ref.Metadata().Name = "GoodsReceipt" Then
    Flag = True;
EndIf;
```

## Compliant Solution

```bsl
If TypeOf(Ref) = Type("DocumentRef.GoodsReceipt") Then
    Flag = True;
EndIf;
```

## See

- [Standard 442. Testing variable's type](https://its.1c.ru/db/v8std#content:442:hdoc)
- Ported from the APK check `АПК_00305`.
