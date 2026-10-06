# String attribute has fixed allowed length

For string attributes use variable length (the **Allowed length** property
is **Variable**) and specify the maximum length of the string.

The **Allowed length** property can be **Fixed** only in cases when
manipulating this data really requires a guarantee that the string has
a certain length, despite the presence of trailing spaces. Fixed strings
waste database space and produce trailing spaces in comparisons and search.

## Noncompliant Code Example

The `ArticleNumber` attribute of type `String(10, Fixed)`:

```xml
<type>
  <types>String</types>
  <stringQualifiers>
    <length>10</length>
    <fixed>true</fixed>
  </stringQualifiers>
</type>
```

## Compliant Solution

```xml
<type>
  <types>String</types>
  <stringQualifiers>
    <length>10</length>
  </stringQualifiers>
</type>
```

## See

- Ported from the APK check `АПК_00139`.
- The check does not report objects adopted in extensions and attributes
  typed with a defined type.
