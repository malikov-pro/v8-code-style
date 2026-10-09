# Object module method parameter name matches an attribute or tabular section

Checks that no method of an object module (of a catalog, document, etc.)
declares a parameter whose name (case-insensitive) matches the name of an
attribute or a tabular section of the same metadata object.

Such a name shadows the object member (attribute or tabular section) inside
the method body and may lead to implicit behavior or runtime errors that are
hard to detect. Rename the parameter.

Only top-level attributes and tabular section names are compared:

- attributes of tabular sections are **not** compared;
- standard attributes (Code, Description, ThisObject, etc.) are **not**
  compared — a method parameter cannot conflict with them in scope.

Each matching parameter is reported once.

## Noncompliant Code Example

```bsl
// Catalog "Goods": attribute "Barcode", tabular section "Remains"

Procedure FillByBarcode(Barcode)
    // The parameter name matches the attribute Barcode
    Message(Barcode);
EndProcedure

Procedure RecalcRemains(Remains)
    // The parameter name matches the tabular section Remains
    Message(Remains);
EndProcedure
```

## Compliant Solution

```bsl
Procedure FillByBarcode(BarcodeValue)
    Message(BarcodeValue);
EndProcedure

Procedure RecalcRemains(RemainsTable)
    Message(RemainsTable);
EndProcedure
```

## See

- [Upstream issue 1C-Company/v8-code-style#1354](https://github.com/1C-Company/v8-code-style/issues/1354)
