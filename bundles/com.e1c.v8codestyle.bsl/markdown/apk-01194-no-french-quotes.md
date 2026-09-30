# French quotes in interface texts

Checks that French quotes (guillemets) are not used in interface texts — string literals of the **NStr** function. Straight double quotes should be used instead.

Quotes outside `NStr` (including help texts) are allowed and are not reported.

## Noncompliant Code Example

```bsl
Message = NStr("ru = 'Document «GoodsSales» not posted'");
```

## Compliant Solution

```bsl
Message = NStr("ru = 'Document ""GoodsSales"" not posted'");
```

## See

- [Standard 598. Refusing to use the letter "ё" and French quotes](https://its.1c.ru/db/v8std#content:598:hdoc)
- Ported from the APK check `АПК_01194`.
