# Zero margins of a spreadsheet document

Zero margins (fields) must not be set for a spreadsheet document: when printing with zero margins the document content is clipped at the page edges. Set sufficient margins, considering the printer's non-printable area, or keep the default margins (standard 548, printing).

The check scans the whole module text, including comments and string literals, case-insensitively, and reports each assignment of 0 to the margin properties of a spreadsheet document (Russian and English property names):

- `.ПолеСлева = 0` (`.LeftMargin = 0`), `.ПолеСправа = 0` (`.RightMargin = 0`)
- `.ПолеСверху = 0` (`.TopMargin = 0`), `.ПолеСнизу = 0` (`.BottomMargin = 0`)

Unlike the exact substring search of the source APK algorithm, whitespace around the assignment sign is allowed. As in the APK algorithm, an assignment is not distinguished from a comparison, so an expression like `Если ТД.ПолеСлева = 0` is also reported.

## Noncompliant Code Example

```bsl
Procedure PrintDocument(Document, Data)

    // the printed content will be clipped
    Document.LeftMargin = 0;
    Document.TopMargin = 0;

EndProcedure
```

## Compliant Solution

```bsl
Procedure PrintDocument(Document, Data)

    Document.LeftMargin = 10;   // mm
    Document.RightMargin = 10;  // mm
    Document.TopMargin = 20;    // mm
    Document.BottomMargin = 20; // mm

EndProcedure
```

## See

- [Standard 548. Generating print forms](https://its.1c.ru/db/v8std#content:548:hdoc)
- Ported from the APK check `АПК_00576`.
