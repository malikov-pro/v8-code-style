# Bulk string concatenation in a loop

Checks that a string variable is accumulated in a loop via concatenation:
a variable is assigned to itself with appending (Var = Var + ...). Each
concatenation creates a new string, which is slow and memory-hungry on
large data volumes.

For bulk operations (the standard guideline is 1000+ concatenations) collect
the parts into an array and join them with `StrJoin` (СтрСоединить); use
`StrSplit` (СтрРазделить) to split. One issue per loop. Numeric counters
(Counter = Counter + 1) are not reported.

## Noncompliant Code Example

```bsl
ExtractedText = "";
For ColumnNumber = 1 To Template.TableWidth Do
    ExtractedText = ExtractedText + LineFeed + AreaText;
EndDo;
```

## Compliant Solution

```bsl
ExtractedTexts = New Array;
For ColumnNumber = 1 To Template.TableWidth Do
    ExtractedTexts.Add(AreaText);
EndDo;
ExtractedText = StrJoin(ExtractedTexts, LineFeed);
```

## Limitations

The original APK rule counts concatenations at runtime and triggers from
1000 operations. A static check cannot count iterations, so it reports the
accumulation pattern itself; types are not evaluated, so accumulation with
variables and function calls can also be reported for numeric accumulation.

## See

- [Standard 782. Bulk string concatenation](https://its.1c.ru/db/v8std#content:782:hdoc)
- Ported from the APK check `АПК_01171`.
